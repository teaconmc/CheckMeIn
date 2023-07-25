package org.teacon.checkin.client.gui.screens.inventory;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.teacon.checkin.world.level.block.entity.PointUniqueBlockEntity;

import java.util.Objects;

public abstract class AbstractCheckPointScreen<T extends AbstractContainerMenu> extends Screen implements MenuAccess<T> {

    public static final int TITLE_COLOR = 0xffffff;
    public static final int LABEL_COLOR = 0xa0a0a0;


    private final T menu;

    @SuppressWarnings("NotNullFieldNotInitialized")
    protected Button doneBtn;
    @SuppressWarnings("NotNullFieldNotInitialized")
    protected Button cancelBtn;

    public AbstractCheckPointScreen(T menu, @SuppressWarnings("unused") Inventory inventory, Component component) {
        super(component);
        this.menu = menu;
    }

    @Override
    public T getMenu() {return this.menu;}

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2, centerY = this.height / 2;
        this.doneBtn = this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, btn -> this.onDone())
                .bounds(centerX - 154, centerY / 2 + 132, 150, 20)
                .build());
        this.cancelBtn = this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, btn -> this.onCancel())
                .bounds(centerX + 4, centerY / 2 + 132, 150, 20)
                .build());
    }

    @Override
    public boolean keyPressed(int keyCode, int p_97668_, int p_97669_) {
        if (keyCode == 256 && this.shouldCloseOnEsc()){ // cancel on pressing ESC and quit
            this.onCancel();
            return true;
        } else if (super.keyPressed(keyCode, p_97668_, p_97669_)) {
            return true;
        } else if (keyCode == 257 || keyCode == 335) { // Enter or Numpad Enter
            this.onDone();
            return true;
        } else {
            return false;
        }
    }

    protected abstract void onDone();

    /**
     * Tells the server to remove the PointUniqueBlock if it's uninitialized by indirectly calling
     * {@link PointUniqueBlockEntity#removeIfInvalid()}
     */
    protected void onCancel() {
        Objects.requireNonNull(Objects.requireNonNull(this.minecraft).player).closeContainer();
        this.onClose(); // close on client side only
    }
}
