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
        this.pointName = new EditBox(this.font, centerX + 4, 60, 125, 20, Component.translatable("container.check_in.point_name"));
        this.addWidget(this.teamID);
        this.addWidget(this.pointName);
        this.doneBtn = this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, this::onDone)
                .bounds(centerX - 154, centerY / 2 + 132, 150, 20)
                .build());
        this.cancelBtn = this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, btn -> this.onClose())
                .bounds(centerX + 4, centerY / 2 + 132, 150, 20)
                .build());
        this.setInitialFocus(this.pointName);
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

    private void onDone(Button btn) {
        CheckMeIn.CHANNEL.sendToServer(new PointUniqueSetDataPacket(this.getMenu().getBlockPos(),
                this.teamID.getValue(), this.pointName.getValue()));
        this.onClose();
    }

    @Override
    public void onClose() {this.minecraft.player.closeContainer();}

    @Override
    public PointUniqueMenu getMenu() {return this.menu;}
}
