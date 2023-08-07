package org.teacon.checkin.network.protocol.game;

import com.google.common.collect.Iterables;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.teacon.checkin.CheckMeIn;
import org.teacon.checkin.client.ClientPacketHandler;
import org.teacon.checkin.world.inventory.PathNavigatorMenu;

import java.util.ArrayList;
import java.util.function.Supplier;

public class PathNaviPageResponsePacket {
    private final int pageNo;
    private final PathNavigatorMenu.Entry[] entries;

    public PathNaviPageResponsePacket(int pageNo, Iterable<PathNavigatorMenu.Entry> entries) {
        this.pageNo = pageNo;
        this.entries = Iterables.toArray(entries, PathNavigatorMenu.Entry.class);
    }

    public PathNaviPageResponsePacket(FriendlyByteBuf buf) {
        this.pageNo = buf.readInt();
        var entries = new ArrayList<PathNavigatorMenu.Entry>();
        while (buf.isReadable()) entries.add(PathNavigatorMenu.Entry.readFromBuf(buf));
        this.entries = entries.toArray(PathNavigatorMenu.Entry[]::new);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.pageNo);
        for (PathNavigatorMenu.Entry entry : entries) entry.writeToBuf(buf);
    }

    public int getPageNo() {return pageNo;}

    public PathNavigatorMenu.Entry[] getEntries() {return entries;}

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> ClientPacketHandler.handlePathNaviPageResponsePacket(this));
        context.setPacketHandled(true);
    }

    public void send(ServerPlayer target) {
        CheckMeIn.CHANNEL.send(PacketDistributor.PLAYER.with(() -> target), this);
    }
}
