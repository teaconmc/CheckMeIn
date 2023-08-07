package org.teacon.checkin.client.gui.screens.inventory.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.teacon.checkin.utils.IntBox;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
public class CustomButton extends Button {
    protected boolean pressed;

    private CustomButton(int x, int y, int width, int height, Component component, OnPress onPress, CreateNarration narration) {
        super(x, y, width, height, component, onPress, narration);
    }

    public boolean isPressed() {return pressed;}

    @Override
    public void onPress() {
        this.pressed = true;
        super.onPress();
    }

    @Override
    public void onRelease(double p_93669_, double p_93670_) {
        this.pressed = false;
        super.onRelease(p_93669_, p_93670_);
    }

    public static class FactoryBuilder {

        private final ResourceLocation texture;
        private final int textureWidth;
        private final int textureHeight;
        private final Map<Status, IntBox> textureMap;

        private FactoryBuilder(ResourceLocation texture, int textureWidth, int textureHeight, int x, int y, int width, int height) {
            this.texture = texture;
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
            this.textureMap = new EnumMap<>(Status.class);
            this.textureMap.put(Status.NORMAL, new IntBox(x, y, width, height));
        }

        public static FactoryBuilder createWithTexture(ResourceLocation texture, int textureWidth, int textureHeight, int x, int y, int width, int height) {
            return new FactoryBuilder(texture, textureWidth, textureHeight, x, y, width, height);
        }

        public static FactoryBuilder createWithTexture(ResourceLocation texture, int x, int y, int width, int height) {
            return createWithTexture(texture, 256, 256, x, y, width, height);
        }

        public FactoryBuilder texture(Status status, int x, int y) {
            var normal = textureMap.get(Status.NORMAL);
            return this.texture(status, x, y, normal.width(), normal.height());
        }

        public FactoryBuilder texture(Status status, int x, int y, int width, int height) {
            this.textureMap.put(status, new IntBox(x, y, width, height));
            return this;
        }

        public Factory build() {
            final IntBox normal = this.textureMap.get(Status.NORMAL);
            final IntBox hover = this.textureMap.getOrDefault(Status.HOVER, normal);
            final IntBox focus = this.textureMap.getOrDefault(Status.FOCUS, hover);
            final IntBox press = this.textureMap.getOrDefault(Status.PRESS, focus);
            final IntBox disabled = this.textureMap.getOrDefault(Status.DISABLED, normal);

            return (x, y, component, onPress) -> new CustomButton(x, y, normal.width(), normal.height(), component, onPress, Supplier::get) {
                @Override
                protected void renderWidget(GuiGraphics guiGraphics, int p_282682_, int p_281714_, float p_282542_) {
                    IntBox box = normal;
                    if (!this.isActive()) box = disabled;
                    else if (this.isPressed()) box = press;
                    else if (this.isHovered()) box = hover;
                    else if (this.isFocused()) box = focus;

                    guiGraphics.blit(texture, this.getX(), this.getY(), 0, box.x(), box.y(), box.width(), box.height(), textureWidth, textureHeight);
                }
            };
        }
    }

    public enum Status {
        NORMAL, HOVER, FOCUS, PRESS, DISABLED
    }

    @FunctionalInterface
    public interface Factory {
        Button create(int x, int y, Component component, Button.OnPress onPress);

        default Button create(int x, int y, Button.OnPress onPress) {return this.create(x, y, CommonComponents.EMPTY, onPress);}
    }
}
