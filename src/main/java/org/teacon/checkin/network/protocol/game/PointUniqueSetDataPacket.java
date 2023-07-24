package org.teacon.checkin.network.protocol.game;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraftforge.network.NetworkEvent;
import org.teacon.checkin.network.capability.CheckInPoints;
import org.teacon.checkin.world.inventory.PointUniqueMenu;
import org.teacon.checkin.world.level.block.entity.PointUniqueBlockEntity;

import java.util.function.Supplier;

public class PointUniqueSetDataPacket {
    private final BlockPos pointUniqueBlockPos;
    private final String teamID;
    private final String pointName;

    public PointUniqueSetDataPacket(BlockPos blockPos, String teamID, String pointName) {
        this.pointUniqueBlockPos = blockPos;
        this.teamID = teamID;
        this.pointName = pointName;
    }

    public PointUniqueSetDataPacket(FriendlyByteBuf buf) {
        this.pointUniqueBlockPos = buf.readBlockPos();
        this.teamID = buf.readUtf();
        this.pointName = buf.readUtf();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pointUniqueBlockPos);
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
        var level = player.level();
        if (level.getBlockEntity(this.pointUniqueBlockPos) instanceof PointUniqueBlockEntity pointUniqueBlockEntity) {
            try {
                var result = sanitize(context);
                pointUniqueBlockEntity.initializeData(result.teamID, result.pointName);
            } catch (SanitizeException e) {
                player.sendSystemMessage(e.getMsg().plainCopy().withStyle(ChatFormatting.BOLD, ChatFormatting.RED));
            }
        }
        if (player.containerMenu instanceof PointUniqueMenu) player.doCloseContainer();
    }

    private SanitizeResult sanitize(NetworkEvent.Context context) throws SanitizeException {
        var teamID = this.teamID.trim();
        var pointName = this.pointName.trim();

        if (teamID.isEmpty()) throw new SanitizeException(Component.translatable("sanitize.check_in.empty",
                Component.translatable("container.check_in.team_id")));
        if (teamID.length() > 50) throw new SanitizeException(Component.translatable("sanitize.check_in.too_long",
                Component.translatable("container.check_in.team_id"), teamID.length(), 50));

        for (var level : context.getSender().server.getAllLevels()) {
            var cap = level.getCapability(CheckInPoints.Provider.CAPABILITY).resolve();
            if (cap.isPresent()) {
                var point = cap.get().getUniquePoint(teamID);
                if (point != null) {
                    int x = point.pos().getX(), y = point.pos().getY(), z = point.pos().getZ();
                    var dim = level.dimensionTypeId().location().toString();
                    throw new SanitizeException(Component.translatable("sanitize.check_in.dup_team_id",
                            teamID, Component.translatable("container.check_in.point_unique"),
                            Component.literal("%d, %d, %d (%s)".formatted(x, y, z, dim)).withStyle(Style.EMPTY
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                            "/execute as @p in %s run teleport %d %d %d".formatted(dim, x, y, z))))));
                }
            }
        }


        if (pointName.isEmpty()) throw new SanitizeException(Component.translatable("sanitize.check_in.empty",
                Component.translatable("container.check_in.point_name")));
        if (pointName.length() > 50) throw new SanitizeException(Component.translatable("sanitize.check_in.too_long",
                Component.translatable("container.check_in.point_name"), pointName.length(), 50));

        return new SanitizeResult(teamID, pointName);
    }

    private record SanitizeResult(String teamID, String pointName) {}
}
