package org.teacon.checkin.client.gui.screens.inventory.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.teacon.checkin.utils.IntBox;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

public class ProgressBar {
    @FunctionalInterface
    public interface Factory {
        void draw(GuiGraphics guiGraphics, int x, int y, float progress);
    }

    public static class FactoryBuilder {
        private final ResourceLocation texture;
        private final int textureWidth;
        private final int textureHeight;
        private final Map<TextureType, IntBox> textureMap;

        public FactoryBuilder(ResourceLocation texture, int textureWidth, int textureHeight, int x, int y, int width, int height) {
            this.texture = texture;
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
            this.textureMap = new EnumMap<>(TextureType.class);
            this.textureMap.put(TextureType.OVERLAY, new IntBox(x, y, width, height));
        }

        public static FactoryBuilder createWithOverlay(ResourceLocation texture, int textureWidth, int textureHeight, int x, int y, int width, int height) {
            return new FactoryBuilder(texture, textureWidth, textureHeight, x, y, width, height);
        }

        public static FactoryBuilder createWithOverlay(ResourceLocation texture, int x, int y, int width, int height) {
            return createWithOverlay(texture, 256, 256, x, y, width, height);
        }

        public FactoryBuilder texture(TextureType type, int x, int y) {
            var overlay = textureMap.get(TextureType.OVERLAY);
            return this.texture(type, x, y, overlay.width(), overlay.height());
        }

        public FactoryBuilder texture(TextureType type, int x, int y, int width, int height) {
            this.textureMap.put(type, new IntBox(x, y, width, height));
            return this;
        }

        public Factory build() {
            final IntBox overlay = textureMap.get(TextureType.OVERLAY);
            @Nullable final IntBox underlay = textureMap.get(TextureType.UNDERLAY);
            @Nullable final IntBox noProgress = textureMap.getOrDefault(TextureType.NO_PROGRESS, underlay);
            final IntBox complete = textureMap.getOrDefault(TextureType.COMPLETE, overlay);

            return (guiGraphics, x, y, progress) -> {
                if (progress <= 0 && noProgress != null)
                    guiGraphics.blit(texture, x, y, 0, noProgress.x(), noProgress.y(), noProgress.width(), noProgress.height(), textureWidth, textureHeight);
                else if (progress >= 1)
                    guiGraphics.blit(texture, x, y, 0, complete.x(), complete.y(), complete.width(), complete.height(), textureWidth, textureHeight);
                else {
                    if (underlay != null) guiGraphics.blit(texture, x, y, 0, underlay.x(), underlay.y(), underlay.width(), underlay.height(), textureWidth, textureHeight);
                    guiGraphics.blit(texture, x, y, 0, overlay.x(), overlay.y(), (int) (overlay.width() * progress), overlay.height(), textureWidth, textureHeight);
                }
            };
        }
    }

    public enum TextureType {
        UNDERLAY, OVERLAY, COMPLETE, NO_PROGRESS
    }
}
