package org.teacon.checkin.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
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
            pointUniqueBlockEntity.setTeamID(this.teamID);
            pointUniqueBlockEntity.setPointName(this.pointName);
        }
    }
}
