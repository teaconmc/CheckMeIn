package org.teacon.checkin.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.teacon.checkin.CheckMeIn;
import org.teacon.checkin.world.inventory.PathNavigatorMenu;

import java.util.function.Supplier;

public class PathNaviPageRequestPacket {
    private final int pageNo;

    public PathNaviPageRequestPacket(int pageNo) {
        this.pageNo = pageNo;
    }

    public PathNaviPageRequestPacket(FriendlyByteBuf buf) {
        this.pageNo = buf.readInt();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(pageNo);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var sender = context.getSender();
            if (sender != null && sender.containerMenu instanceof PathNavigatorMenu menu) menu.sendPage(sender, pageNo);
        });
        context.setPacketHandled(true);
    }

    public void send() {
        CheckMeIn.CHANNEL.sendToServer(this);
    }
}
