package org.teacon.checkin.client.gui.screens.inventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.teacon.checkin.CheckMeIn;
import org.teacon.checkin.network.protocol.game.PointUniqueSetDataPacket;
import org.teacon.checkin.world.inventory.PointUniqueMenu;

public class PointUniqueScreen extends Screen implements MenuAccess<PointUniqueMenu> {
    private final PointUniqueMenu menu;
    private EditBox teamID;
    private EditBox pointName;
    private Button doneBtn;
    private Button cancelBtn;

    public PointUniqueScreen(PointUniqueMenu menu, Inventory inventory, Component component) {
        super(component);
        this.menu = menu;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2, centerY = this.height / 2;
        this.teamID = new EditBox(this.font, centerX - 150, 60, 125, 20, Component.translatable("container.check_in.team_id"));
        teamID.setMaxLength(50);
        this.pointName = new EditBox(this.font, centerX + 4, 60, 125, 20, Component.translatable("container.check_in.point_name"));
        pointName.setMaxLength(50);
        this.addWidget(this.teamID);
        this.addWidget(this.pointName);
        this.doneBtn = this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, btn -> this.onDone())
                .bounds(centerX - 154, centerY / 2 + 132, 150, 20)
                .build());
        this.cancelBtn = this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, btn -> this.onClose())
                .bounds(centerX + 4, centerY / 2 + 132, 150, 20)
                .build());
        this.setInitialFocus(this.teamID);
    }

    @Override
    public boolean keyPressed(int keyCode, int p_97668_, int p_97669_) {
        if (super.keyPressed(keyCode, p_97668_, p_97669_)) {
            return true;
        } else if (keyCode == 257 || keyCode == 335) { // Enter or Numpad Enter
            this.onDone();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float p_282465_) {
        this.renderBackground(guiGraphics);

        int centerX = this.width / 2;
        guiGraphics.drawCenteredString(this.font, Component.translatable("container.check_in.point_unique"),
                centerX, 20, 0xffffff);
        guiGraphics.drawString(this.font, Component.translatable("container.check_in.team_id"),
                centerX - 150, 50, 0xa0a0a0);
        this.teamID.render(guiGraphics, mouseX, mouseY, p_282465_);
        guiGraphics.drawString(this.font, Component.translatable("container.check_in.point_name"),
                centerX + 4, 50, 0xa0a0a0);
        this.pointName.render(guiGraphics, mouseX, mouseY, p_282465_);

        super.render(guiGraphics, mouseX, mouseY, p_282465_);
    }

    private void onDone() {
        CheckMeIn.CHANNEL.sendToServer(new PointUniqueSetDataPacket(this.getMenu().getBlockPos(),
                this.teamID.getValue(), this.pointName.getValue()));
        super.onClose(); // close without send closing container packet
    }

    /**
     * Close without saving data (server should remove block)
     */
    @Override
    public void onClose() {
        this.minecraft.player.closeContainer();
        super.onClose();
    }

    @Override
    public PointUniqueMenu getMenu() {return this.menu;}
}
