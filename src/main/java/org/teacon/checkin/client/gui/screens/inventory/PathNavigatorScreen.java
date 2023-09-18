package org.teacon.checkin.client.gui.screens.inventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.network.PacketDistributor;
import org.teacon.checkin.CheckMeIn;
import org.teacon.checkin.client.gui.screens.inventory.widgets.CustomButton;
import org.teacon.checkin.client.gui.screens.inventory.widgets.ProgressBar;
import org.teacon.checkin.network.protocol.game.PathNaviErasePacket;
import org.teacon.checkin.network.protocol.game.PathNaviSetGuidingPathPacket;
import org.teacon.checkin.network.protocol.game.PathNaviTpBackPacket;
import org.teacon.checkin.world.inventory.PathNavigatorMenu;

import java.util.ArrayList;
import java.util.List;

import static org.teacon.checkin.client.gui.screens.inventory.widgets.CustomButton.Status.*;
import static org.teacon.checkin.client.gui.screens.inventory.widgets.ProgressBar.TextureType.COMPLETE;
import static org.teacon.checkin.client.gui.screens.inventory.widgets.ProgressBar.TextureType.UNDERLAY;

public class PathNavigatorScreen extends Screen implements MenuAccess<PathNavigatorMenu> {
    public static final ResourceLocation TEXTURE = new ResourceLocation(CheckMeIn.MODID, "textures/gui/nvg_path.png");
    @SuppressWarnings("unused")
    public static final int TEXTURE_WIDTH = 256;
    @SuppressWarnings("unused")
    public static final int TEXTURE_HEIGHT = 256;

    private static final int BG_X = 0;
    private static final int BG_Y = 0;
    private static final int BG_WIDTH = 208;
    private static final int BG_HEIGHT = 208;

    private static final CustomButton.Factory PATHFINDING_BTN = CustomButton.FactoryBuilder.createWithTexture(TEXTURE, 0, 237, 87, 19)
            .texture(HOVER, 87, 237).texture(PRESS, 0, 237 - 19).build();
    private static final CustomButton.Factory TP_BACK_BTN = CustomButton.FactoryBuilder.createWithTexture(TEXTURE, 208, 114, 8, 9)
            .texture(HOVER, 208, 114+9).texture(PRESS, 208, 114 + 9 * 2)
            .tooltip(Component.translatable("container.check_in.tp_back")).build();
    private static final ProgressBar.Factory PROGRESS = ProgressBar.FactoryBuilder.createWithOverlay(TEXTURE, 208, 69, 42, 9)
            .texture(UNDERLAY, 208, 69 - 9).texture(COMPLETE, 208, 69 + 9).build();
    private static final CustomButton.Factory ERASE_BTN = CustomButton.FactoryBuilder.createWithTexture(TEXTURE, 208, 87, 8, 9)
            .texture(HOVER, 208, 87+9).texture(PRESS, 208, 87 + 9 * 2)
            .tooltip(Component.translatable("container.check_in.clear_progress")).build();
    private static final CustomButton.Factory BACKWARD_BTN = CustomButton.FactoryBuilder.createWithTexture(TEXTURE, 208, 0, 24, 15)
            .texture(DISABLED, 208, 15).texture(HOVER, 208, 15 * 2).texture(PRESS, 208, 15 * 3)
            .tooltip(Component.translatable("container.check_in.prev_page")).build();
    private static final CustomButton.Factory FORWARD_BTN = CustomButton.FactoryBuilder.createWithTexture(TEXTURE, 208 + 24, 0, 24, 15)
            .texture(DISABLED, 208 + 24, 15).texture(HOVER, 208 + 24, 15 * 2).texture(PRESS, 208 + 24, 15 * 3)
            .tooltip(Component.translatable("container.check_in.next_page")).build();

    private final PathNavigatorMenu menu;
    private final List<Button> tpBackButtons = new ArrayList<>(PathNavigatorMenu.PAGE_SIZE);
    private final List<Button> eraseButtons = new ArrayList<>(PathNavigatorMenu.PAGE_SIZE);

    public PathNavigatorScreen(PathNavigatorMenu menu, @SuppressWarnings("unused") Inventory inventory, Component component) {
        super(component);
        this.menu = menu;
    }

    @Override
    public PathNavigatorMenu getMenu() {return this.menu;}

    @Override
    public void init() {
        this.clearWidgets();

        var centerX = this.width / 2;

        this.addPageWidgets();

        this.addRenderableWidget(BACKWARD_BTN.create(centerX + 43, 194, btn -> this.flipPage(false)));
        this.addRenderableWidget(FORWARD_BTN.create(centerX + 43 + 26, 194, btn -> this.flipPage(true)));
    }

    @Override
    protected void clearWidgets() {
        super.clearWidgets();
        this.tpBackButtons.clear();
        this.eraseButtons.clear();
    }

    private void addPageWidgets() {
        var centerX = this.width / 2;
        var entries = this.menu.currentPageEntries();
        for (int i = 0; i < entries.length; i++) {
            var y = 99 + 20 * i;
            this.tpBackButtons.add(this.addRenderableWidget(TP_BACK_BTN.create(centerX + 1, y, this::tpBack)));
            this.eraseButtons.add(this.addRenderableWidget(ERASE_BTN.create(centerX + 87, y - 1, this::erase)));
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);

        var centerX = this.width / 2;
        // title
        guiGraphics.drawCenteredString(this.font, Component.translatable("container.check_in.nvg_path"), centerX - 55, 19, 0xffffff);
        // subtitles
        guiGraphics.drawString(this.font, Component.translatable("container.check_in.navigating"), centerX - 89, 39, 0xffffff);
        guiGraphics.drawString(this.font, Component.translatable("container.check_in.candidates"), centerX - 89, 80, 0xffffff);
        // entries
        this.drawCurrentlyGuiding(guiGraphics);
        this.drawEntries(guiGraphics, mouseX, mouseY);
        // page number
        var pageNo = formatPageNo(this.menu.getPageNo() + 1, this.menu.getPages());
        var pageNoX = centerX + 23 - (int)(this.font.getSplitter().stringWidth(pageNo) / 2);
        guiGraphics.drawString(this.font, pageNo, pageNoX, 199, 0xffffff, false);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        super.renderBackground(guiGraphics);
        var centerX = this.width / 2;
        guiGraphics.blit(TEXTURE, centerX - BG_WIDTH / 2, 10, BG_X, BG_Y, BG_WIDTH, BG_HEIGHT);
    }

    private void drawCurrentlyGuiding(GuiGraphics guiGraphics) {
        var centerX = this.width / 2;
        var y = 59;
        var entry = this.menu.getCurrentlyGuiding();
        if (entry == null) {
            return;
        }
        // name
        var name = entry.pointName();
        var splitter = this.font.getSplitter();
        if (splitter.stringWidth(name) > 60)
            name = splitter.plainHeadByWidth(entry.pointName(), 55, Style.EMPTY) + "...";
        guiGraphics.drawString(this.font, name, centerX - 92, y, 0xffffff, false);
        // distance
        var distance = Float.isInfinite(entry.distance()) ? "∞" : Float.isNaN(entry.distance()) ? "N/A"
                : Math.round(entry.distance() * 10) / 10f + "m";// imprecise (for large numbers) but much faster than formatting
        var distX = centerX - 5 - (int) splitter.stringWidth(distance);
        guiGraphics.drawString(this.font, distance, distX, y, 0xffffff, false);
        // progress
        var progress = Math.round(entry.progress() * 100) + "%";
        guiGraphics.drawString(this.font, progress, centerX + 17, y, 0xffffff, false);
        // progress bar
        PROGRESS.draw(guiGraphics, centerX + 42, y - 1, entry.progress());
    }

    private void drawEntries(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        var centerX = this.width / 2;
        int i = 0;
        var splitter = this.font.getSplitter();
        for (var entry : this.menu.currentPageEntries()) {
            var y = 99 + 20 * i;
            // name
            var name = entry.pointName();
            if (splitter.stringWidth(name) > 60)
                name = splitter.plainHeadByWidth(entry.pointName(), 55, Style.EMPTY) + "...";
            guiGraphics.drawString(this.font, name, centerX - 92, y, 0xffffff, false);
            if (mouseX >= centerX - 92 && mouseX <= centerX - 32 && mouseY >= y-5 && mouseY <= y + 11) {
                this.setTooltipForNextRenderPass(Component.translatable("container.check_in.start_guiding"));
            }
            // distance
            var distance = Float.isInfinite(entry.distance()) ? "∞" : Float.isNaN(entry.distance()) ? "N/A"
                    : Math.round(entry.distance() * 10) / 10f + "m";// imprecise (for large numbers) but much faster than formatting
            var distX = centerX - 5 - (int) splitter.stringWidth(distance);
            guiGraphics.drawString(this.font, distance, distX, y, 0xffffff, false);
            // progress
            var progress = Math.round(entry.progress() * 100) + "%";
            guiGraphics.drawString(this.font, progress, centerX + 17, y, 0xffffff, false);
            // progress bar
            PROGRESS.draw(guiGraphics, centerX + 42, y - 1, entry.progress());

            i++;
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        var leftBound = this.width / 2 - 92;
        var rightBound = leftBound + 60;
        final int upperBound = 94;
        if (x >= leftBound && x <= rightBound && y >= upperBound) {
            int clickedIndex = (int) ((y - upperBound) / 20);
            if (clickedIndex >= 0 && clickedIndex <= 5) {
                var entries = this.menu.currentPageEntries();
                var p = this.minecraft.player;
                if (p != null && clickedIndex < entries.length) {
                    var clickedEntry = entries[clickedIndex];
                    menu.updateCurrentlyGuidingUnsafe(clickedEntry);
                    var c2sPacket = new PathNaviSetGuidingPathPacket(clickedEntry.teamId(), clickedEntry.pathId());
                    CheckMeIn.CHANNEL.send(PacketDistributor.SERVER.noArg(), c2sPacket);
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
            }
        }
        return super.mouseClicked(x, y, button);
    }

    private void flipPage(boolean forward) {
        this.menu.flipPage(forward);
        this.init();
    }

    private void tpBack(Button button) {
        var idx = this.tpBackButtons.indexOf(button);
        if (idx == -1) return;
        var entry = this.menu.currentPageEntries()[idx];
        new PathNaviTpBackPacket(entry.teamId(), entry.pathId()).send();
    }

    private void erase(Button button) {
        var idx = this.eraseButtons.indexOf(button);
        if (idx == -1) return;
        var entry = this.menu.currentPageEntries()[idx];
        new PathNaviErasePacket(entry.teamId(), entry.pathId()).send();
        var newEntry = new PathNavigatorMenu.Entry(entry.teamId(), entry.pathId(), entry.pointName(), entry.distance(), 0);
        this.menu.currentPageEntries()[idx] = newEntry;
        if (entry.equals(this.menu.getCurrentlyGuiding())) this.menu.updateCurrentlyGuidingUnsafe(newEntry);
    }

    /**
     * Equivalent to {@code "%02d/%02d"} but faster
     */
    private static String formatPageNo(int pageNo, int pages) {
        var str1 = Integer.toString(pageNo);
        var str2 = Integer.toString(pages);
        var l1 = str1.length();
        var zeros1 = l1 > 2 ? 0 : 2 - l1;
        var l2 = str2.length();
        var zeros2 = l2 > 2 ? 0 : 2 - l2;
        var sb = new StringBuilder(zeros1 + l1 + 1 + zeros2 + l2);
        for (int i = 0; i < zeros1; i++) sb.append('0');
        sb.append(str1).append('/');
        for (int i = 0; i < zeros2; i++) sb.append('0');
        return sb.append(str2).toString();
    }
}
