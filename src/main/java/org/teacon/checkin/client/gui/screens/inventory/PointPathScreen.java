package org.teacon.checkin.client.gui.screens.inventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.teacon.checkin.CheckMeIn;
import org.teacon.checkin.network.protocol.game.PointPathSetDataPacket;
import org.teacon.checkin.world.inventory.PointPathMenu;

public class PointPathScreen extends AbstractCheckPointScreen<PointPathMenu> {
    @SuppressWarnings("NotNullFieldNotInitialized")
    private EditBox teamID;
    @SuppressWarnings("NotNullFieldNotInitialized")
    private EditBox pointName;
    @SuppressWarnings("NotNullFieldNotInitialized")
    private EditBox pathID;
    @SuppressWarnings("NotNullFieldNotInitialized")
    private EditBox ord;

    public PointPathScreen(PointPathMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        this.teamID = new EditBox(this.font, centerX - 150, 60, 125, 20, Component.translatable("container.check_in.team_id"));
        teamID.setMaxLength(50);
        this.pointName = new EditBox(this.font, centerX + 4, 60, 125, 20, Component.translatable("container.check_in.point_name"));
        pointName.setMaxLength(50);
        this.pathID = new EditBox(this.font, centerX - 150, 100, 125, 20, Component.translatable("container.check_in.path_id"));
        pathID.setMaxLength(50);
        pathID.setFilter(str -> str.chars().allMatch(c -> '0' <= c && c <= '9' || 'a' <= c && c <= 'z' || c == '_'));
        this.ord = new EditBox(this.font, centerX + 4, 100, 125, 20, Component.translatable("container.check_in.ord"));
        pathID.setMaxLength(5);
        ord.setFilter(str -> str.chars().allMatch(c -> '0' <= c && c <= '9'));
        this.addWidget(this.teamID);
        this.addWidget(this.pointName);
        this.addWidget(this.pathID);
        this.addWidget(this.ord);
        this.setInitialFocus(this.teamID);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float p_282465_) {
        this.renderBackground(guiGraphics);

        int centerX = this.width / 2;
        guiGraphics.drawCenteredString(this.font, Component.translatable("container.check_in.point_unique"),
                centerX, 20, TITLE_COLOR);
        guiGraphics.drawString(this.font, Component.translatable("container.check_in.team_id"),
                centerX - 150, 50, LABEL_COLOR);
        this.teamID.render(guiGraphics, mouseX, mouseY, p_282465_);
        guiGraphics.drawString(this.font, Component.translatable("container.check_in.point_name"),
                centerX + 4, 50, LABEL_COLOR);
        this.pointName.render(guiGraphics, mouseX, mouseY, p_282465_);
        guiGraphics.drawString(this.font, Component.translatable("container.check_in.path_id"),
                centerX - 150, 90, LABEL_COLOR);
        this.pathID.render(guiGraphics, mouseX, mouseY, p_282465_);
        guiGraphics.drawString(this.font, Component.translatable("container.check_in.ord"),
                centerX + 4, 90, LABEL_COLOR);
        this.ord.render(guiGraphics, mouseX, mouseY, p_282465_);

        super.render(guiGraphics, mouseX, mouseY, p_282465_);
    }

    @Override
    protected void onDone() {
        CheckMeIn.CHANNEL.sendToServer(new PointPathSetDataPacket(this.getMenu().getBlockPos(),
                this.teamID.getValue(), this.pointName.getValue(), this.pathID.getValue(), this.ord.getValue()));
        this.onClose(); // close on client side only
    }
}
