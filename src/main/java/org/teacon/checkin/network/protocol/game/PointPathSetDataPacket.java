package org.teacon.checkin.network.protocol.game;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.teacon.checkin.network.capability.CheckInPoints;
import org.teacon.checkin.network.capability.PathPointData;
import org.teacon.checkin.utils.TextComponent;
import org.teacon.checkin.world.inventory.PointPathMenu;
import org.teacon.checkin.world.level.block.entity.PointPathBlockEntity;

import java.util.function.Supplier;

public class PointPathSetDataPacket {
    private final BlockPos blockPos;
    private final String teamID;
    private final String pointName;
    private final String pathID;
    private final String ord;

    public PointPathSetDataPacket(BlockPos blockPos, String teamID, String pointName, String pathID, String ord) {
        this.blockPos = blockPos;
        this.teamID = teamID;
        this.pointName = pointName;
        this.pathID = pathID;
        this.ord = ord;
    }

    public PointPathSetDataPacket(FriendlyByteBuf buf) {
        this.blockPos = buf.readBlockPos();
        this.teamID = buf.readUtf();
        this.pointName = buf.readUtf();
        this.pathID = buf.readUtf();
        this.ord = buf.readUtf();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.blockPos);
        buf.writeUtf(this.teamID);
        buf.writeUtf(this.pointName);
        buf.writeUtf(this.pathID);
        buf.writeUtf(this.ord);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> this.doHandle(context));
        context.setPacketHandled(true);
    }

    public void doHandle(NetworkEvent.Context context) {
        var player = context.getSender();
        if (player == null) return;
        var level = player.level();
        if (level.getBlockEntity(this.blockPos) instanceof PointPathBlockEntity) {
            try {
                var result = sanitize(player);
                CheckInPoints.of(level).resolve().ifPresent(cap -> {
                    cap.removePathPoint(result.pos());
                    cap.addPathPointIfAbsent(result);
                });
            } catch (SanitizeException e) {
                player.sendSystemMessage(e.getMsg().plainCopy().withStyle(ChatFormatting.BOLD, ChatFormatting.RED));
            }
        }
        if (player.containerMenu instanceof PointPathMenu)
            player.closeContainer(); // also tells the client to close menu
    }

    private PathPointData sanitize(ServerPlayer player) throws SanitizeException {
        var teamID = this.teamID.trim();
        var pointName = this.pointName.trim();
        var pathID = this.pathID.trim();
        var ord = this.ord.trim();

        if (teamID.isEmpty()) throw new SanitizeException(Component.translatable("sanitize.check_in.empty",
                Component.translatable("container.check_in.team_id")));
        if (teamID.length() > PathPointData.TEAM_ID_MAX_LENGTH) throw new SanitizeException(Component.translatable("sanitize.check_in.too_long",
                Component.translatable("container.check_in.team_id"), teamID.length(), 50));

        if (pointName.isEmpty()) throw new SanitizeException(Component.translatable("sanitize.check_in.empty",
                Component.translatable("container.check_in.point_name")));
        if (pointName.length() > PathPointData.POINT_NAME_MAX_LENGTH) throw new SanitizeException(Component.translatable("sanitize.check_in.too_long",
                Component.translatable("container.check_in.point_name"), pointName.length(), 50));

        if (pathID.isEmpty()) throw new SanitizeException(Component.translatable("sanitize.check_in.empty",
                Component.translatable("container.check_in.path_id")));
        if (pathID.length() > PathPointData.PATH_ID_MAX_LENGTH) throw new SanitizeException(Component.translatable("sanitize.check_in.too_long",
                Component.translatable("container.check_in.point_name"), pointName.length(), 50));

        Short ordNum = null;
        if (!ord.isEmpty()) {
            try {ordNum = Short.parseShort(ord);} catch (NumberFormatException e) {
                throw new SanitizeException(Component.translatable("sanitize.check_in.invalid_number", Component.translatable("container.check_in.ord")));
            }
            if (ordNum < 0 || ordNum > PathPointData.ORD_MAX)
                throw new SanitizeException(Component.translatable("sanitize.check_in.out_of_range",
                        Component.translatable("container.check_in.ord"), ord, "[0, 1024]"));

            PathPointData dup;
            for (var level : player.server.getAllLevels()) {
                var cap = CheckInPoints.of(level).resolve();
                if (cap.isPresent() && (dup = cap.get().getPathPoint(teamID, pathID, ordNum)) != null
                        && /* not updating the block being edited */ (!dup.pos().equals(this.blockPos) || level != player.level())) {

                    var dim = level.dimension().location().toString();
                    var dupPos = dup.pos();
                    int x = dupPos.getX(), y = dupPos.getY(), z = dupPos.getZ();
                    throw new SanitizeException(Component.translatable("sanitize.check_in.dup_ord",
                            ordNum, Component.literal("%d, %d, %d (%s)".formatted(x, y, z, dim))
                                    .withStyle(Style.EMPTY.withClickEvent(TextComponent.teleportTo(dup.pos(), level)))
                                    .withStyle(ChatFormatting.UNDERLINE)));
                }
            }
        }

        return new PathPointData(this.blockPos, teamID, pointName, pathID, ordNum);
    }
}
