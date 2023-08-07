package org.teacon.checkin.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.teacon.checkin.CheckMeIn;
import org.teacon.checkin.network.capability.CheckProgress;

@Mod.EventBusSubscriber(modid = CheckMeIn.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerCloneEventHandler {
    @SubscribeEvent
    public static void handlePlayerClone(PlayerEvent.Clone event) {
        var original = event.getOriginal();
        var player = event.getEntity();
        if (original instanceof ServerPlayer && player instanceof ServerPlayer && event.isWasDeath()) {
            original.reviveCaps();
            CheckProgress.of((ServerPlayer) original).ifPresent(progOrig ->
                    CheckProgress.of((ServerPlayer) player).ifPresent(progPlayer ->
                            progPlayer.copyFrom(progOrig)
                    )
            );
            original.invalidateCaps();
        }
    }
}
