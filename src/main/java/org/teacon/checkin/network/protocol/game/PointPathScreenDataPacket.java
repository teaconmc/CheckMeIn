package org.teacon.checkin.network.protocol.game;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.teacon.checkin.client.gui.screens.inventory.PointPathScreen;
import org.teacon.checkin.world.level.block.entity.PointPathBlockEntity;

import java.util.function.Supplier;

public class PointPathScreenDataPacket {
    private final BlockPos blockPos;
    private final String teamID;
    private final String pointName;
    private final String pathID;
    private final String ord;

    public PointPathScreenDataPacket(BlockPos blockPos, String teamID, String pointName, String pathID, String ord) {
        this.blockPos = blockPos;
        this.teamID = teamID;
        this.pointName = pointName;
        this.pathID = pathID;
        this.ord = ord;
    }

    public PointPathScreenDataPacket(FriendlyByteBuf buf) {
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
        var mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(this.blockPos) instanceof PointPathBlockEntity && mc.screen instanceof PointPathScreen screen) {
            screen.updateGui(this.teamID, this.pointName, this.pathID, this.ord);
        }
    }
}
