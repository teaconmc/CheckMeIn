package org.teacon.checkin.network.protocol.game;

import net.minecraft.client.Minecraft;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.teacon.checkin.network.capability.GuidingManager;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class SyncNextNavPointPacket {

    @Nullable
    private final GlobalPos pos;

    public SyncNextNavPointPacket(@Nullable GlobalPos pos) {
        this.pos = pos;
    }

    public SyncNextNavPointPacket(FriendlyByteBuf buf) {
        this.pos = buf.isReadable() ? buf.readGlobalPos() : null;
    }

    public void write(FriendlyByteBuf buf) {
        if (this.pos != null) buf.writeGlobalPos(pos);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        var ctx = contextSupplier.get();
        ctx.enqueueWork(() -> Handler.handle0(this, ctx));
        ctx.setPacketHandled(true);
    }

    private static final class Handler {
        public static void handle0(SyncNextNavPointPacket packet, NetworkEvent.Context context) {
            var p = Minecraft.getInstance().player;
            if (p == null) {
                return;
            }

            GuidingManager.of(p).ifPresent(guiding -> guiding.clientFace.setPathNavNextPoint(packet.pos));
        }
    }
}
