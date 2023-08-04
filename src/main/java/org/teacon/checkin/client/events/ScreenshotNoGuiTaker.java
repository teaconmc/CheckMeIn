package org.teacon.checkin.client.events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.teacon.checkin.CheckMeIn;


@Mod.EventBusSubscriber(modid = CheckMeIn.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ScreenshotNoGuiTaker {
    private static boolean shot = false;

    public static void requestShot() {shot = true;}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void screenshotOnDemand(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;

        if (shot) {
            var mc = Minecraft.getInstance();
            Screenshot.grab(mc.gameDirectory, mc.getMainRenderTarget(), msg -> mc.execute(() -> mc.gui.getChat().addMessage(msg)));
            shot = false;
        }
    }
}
