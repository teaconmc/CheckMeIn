package org.teacon.checkin.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.teacon.checkin.client.ClientPacketHandler;

import java.util.function.Supplier;

public class PointUniqueScreenDataPacket {
    private final BlockPos blockPos;
    private final String teamID;
    private final String pointName;

    public PointUniqueScreenDataPacket(BlockPos blockPos, String teamID, String pointName) {
        this.blockPos = blockPos;
        this.teamID = teamID;
        this.pointName = pointName;
    }

    public PointUniqueScreenDataPacket(FriendlyByteBuf buf) {
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
        context.enqueueWork(() -> ClientPacketHandler.handlePointUniqueScreenDataPacket(this, context));
        context.setPacketHandled(true);
    }

    public BlockPos getBlockPos() {return blockPos;}

    public String getTeamID() {return teamID;}

    public String getPointName() {return pointName;}
}
