package org.teacon.checkin.client.gui.screens.inventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.teacon.checkin.CheckMeIn;
import org.teacon.checkin.network.capability.UniquePointData;
import org.teacon.checkin.network.protocol.game.PointUniqueSetDataPacket;
import org.teacon.checkin.world.inventory.PointUniqueMenu;

public class PointUniqueScreen extends AbstractCheckPointScreen<PointUniqueMenu> {

    @SuppressWarnings("NotNullFieldNotInitialized")
    private EditBox teamID;
    @SuppressWarnings("NotNullFieldNotInitialized")
    private EditBox pointName;

    public PointUniqueScreen(PointUniqueMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        this.teamID = new EditBox(this.font, centerX - 150, 60, 125, 20, Component.translatable("container.check_in.team_id"));
        this.teamID.setMaxLength(UniquePointData.TEAM_ID_MAX_LENGTH);
        this.teamID.setValue(this.getMenu().getData().teamID());
        this.pointName = new EditBox(this.font, centerX + 4, 60, 125, 20, Component.translatable("container.check_in.point_name"));
        this.pointName.setMaxLength(UniquePointData.POINT_NAME_MAX_LENGTH);
        this.pointName.setValue(this.getMenu().getData().pointName());
        this.addWidget(this.teamID);
        this.addWidget(this.pointName);
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

        super.render(guiGraphics, mouseX, mouseY, p_282465_);
    }

    @Override
    protected void onDone() {
        CheckMeIn.CHANNEL.sendToServer(new PointUniqueSetDataPacket(this.getMenu().getData().pos(),
                this.teamID.getValue(), this.pointName.getValue()));
        this.onClose(); // close on client side only
    }
}
