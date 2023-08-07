package org.teacon.checkin.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.teacon.checkin.CheckMeIn;
import org.teacon.checkin.client.ClientPacketHandler;
import org.teacon.checkin.utils.NetworkHelper;

import java.util.List;
import java.util.function.Supplier;

public class PathPlannerGuidePacket {
    private final List<BlockPos> guidePoints;

    public PathPlannerGuidePacket(List<BlockPos> guidePoints) {
        this.guidePoints = guidePoints;
    }

    public PathPlannerGuidePacket(FriendlyByteBuf buf) {
        this.guidePoints = NetworkHelper.readAllBlockPosOptimized(buf, false);
    }

    public void write(FriendlyByteBuf buf) {
        NetworkHelper.writeAllBlockPosOptimized(guidePoints, buf, false);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> ClientPacketHandler.handlePathPlannerGuidePacket(this, context));
        context.setPacketHandled(true);
    }

    public List<BlockPos> getGuidePoints() {return guidePoints;}

    public void send(ServerPlayer player) {
        CheckMeIn.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), this);
    }
}
