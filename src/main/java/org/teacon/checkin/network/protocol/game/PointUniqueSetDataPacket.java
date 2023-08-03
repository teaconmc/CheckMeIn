package org.teacon.checkin.network.protocol.game;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.teacon.checkin.network.capability.CheckInPoints;
import org.teacon.checkin.network.capability.UniquePointData;
import org.teacon.checkin.utils.TextComponent;
import org.teacon.checkin.world.inventory.PointUniqueMenu;
import org.teacon.checkin.world.level.block.entity.PointUniqueBlockEntity;

import java.util.function.Supplier;

public class PointUniqueSetDataPacket {
    private final BlockPos blockPos;
    private final String teamID;
    private final String pointName;

    public PointUniqueSetDataPacket(BlockPos blockPos, String teamID, String pointName) {
        this.blockPos = blockPos;
        this.teamID = teamID;
        this.pointName = pointName;
    }

    public PointUniqueSetDataPacket(FriendlyByteBuf buf) {
        this.blockPos = buf.readBlockPos();
        this.teamID = buf.readUtf();
        this.pointName = buf.readUtf();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.blockPos);
        buf.writeUtf(this.teamID);
        buf.writeUtf(this.pointName);
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
        if (level.getBlockEntity(this.blockPos) instanceof PointUniqueBlockEntity) {
            try {
                var result = sanitize(player);
                CheckInPoints.of(level).resolve().ifPresent(cap -> {
                    cap.removeUniquePoint(result.pos());
                    cap.addUniquePointIfAbsent(result);
                });
            } catch (SanitizeException e) {
                player.sendSystemMessage(e.getMsg().plainCopy().withStyle(ChatFormatting.BOLD, ChatFormatting.RED));
            }
        }
        if (player.containerMenu instanceof PointUniqueMenu) player.closeContainer(); // also tells the client to close menu
    }

    private UniquePointData sanitize(ServerPlayer player) throws SanitizeException {
        var teamID = this.teamID.trim();
        var pointName = this.pointName.trim();

        if (teamID.isEmpty()) throw new SanitizeException(Component.translatable("sanitize.check_in.empty",
                Component.translatable("container.check_in.team_id")));
        if (teamID.length() > 50) throw new SanitizeException(Component.translatable("sanitize.check_in.too_long",
                Component.translatable("container.check_in.team_id"), teamID.length(), 50));

        for (var level : player.server.getAllLevels()) {
            var cap = CheckInPoints.of(level).resolve();
            if (cap.isPresent()) {
                var point = cap.get().getUniquePoint(teamID);
                if (point != null
                        && /* not updating the block being edited */ (!point.pos().equals(this.blockPos) || level != player.level())) {

                    int x = point.pos().getX(), y = point.pos().getY(), z = point.pos().getZ();
                    var dim = level.dimensionTypeId().location().toString();
                    throw new SanitizeException(Component.translatable("sanitize.check_in.dup_team_id",
                            teamID, Component.translatable("container.check_in.point_unique"),
                            Component.literal("%d, %d, %d (%s)".formatted(x, y, z, dim))
                                    .withStyle(Style.EMPTY.withClickEvent(TextComponent.teleportTo(point.pos(), level)))
                                    .withStyle(ChatFormatting.UNDERLINE)));
                }
            }
        }


        if (pointName.isEmpty()) throw new SanitizeException(Component.translatable("sanitize.check_in.empty",
                Component.translatable("container.check_in.point_name")));
        if (pointName.length() > 50) throw new SanitizeException(Component.translatable("sanitize.check_in.too_long",
                Component.translatable("container.check_in.point_name"), pointName.length(), 50));

        return new UniquePointData(this.blockPos, teamID, pointName);
    }
}
