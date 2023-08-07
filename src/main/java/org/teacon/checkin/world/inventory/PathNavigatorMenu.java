package org.teacon.checkin.world.inventory;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.teacon.checkin.CheckMeIn;
import org.teacon.checkin.network.capability.PathPointData;
import org.teacon.checkin.network.protocol.game.PathNaviPageRequestPacket;
import org.teacon.checkin.network.protocol.game.PathNaviPageResponsePacket;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

public class PathNavigatorMenu extends AbstractContainerMenu {
    public static final int PAGE_SIZE = 5;

    /**
     * Server-side only
     */
    @Nullable
    private final ItemStack itemStack;

    private final Entry[][] pages;
    private int pageNo = 0;
    private final boolean isClient;

    @Nullable
    private Entry currentlyGuiding;

    public PathNavigatorMenu(int windowId, @SuppressWarnings("unused") Inventory inventory, FriendlyByteBuf buf) {
        super(CheckMeIn.PATH_NAVIGATOR_MENU.get(), windowId);
        this.itemStack = null;
        String teamId = null, pathId = null;
        if (buf.readBoolean()) {
            teamId = buf.readUtf();
            pathId = buf.readUtf();
        }

        this.pages = new Entry[buf.readInt()][];
        var entries = new ArrayList<Entry>();
        while (buf.isReadable()) {
            var entry = Entry.readFromBuf(buf);
            entries.add(entry);
            if (Objects.equals(entry.teamId, teamId) && Objects.equals(entry.pathId, pathId)) {
                this.currentlyGuiding = entry;
            }
        }
        this.pages[0] = entries.toArray(Entry[]::new);
        for (int i = 1; i < pages.length; i++) pages[i] = new Entry[]{};

        this.isClient = true;
    }

    public PathNavigatorMenu(int windowId, ItemStack stack, Collection<Entry> entries, @Nullable PathPointData.TeamPathID currentlyGuiding) {
        super(CheckMeIn.PATH_NAVIGATOR_MENU.get(), windowId);
        this.itemStack = stack;
        this.pages = new Entry[getPages(entries)][];

        var iter = entries.iterator();
        for (int i = 0; i < this.pages.length; i++) {
            this.pages[i] = new Entry[Math.min(PAGE_SIZE, entries.size() - i * PAGE_SIZE)];
            for (int j = 0; j < this.pages[i].length; j++) {
                var entry = iter.next();
                this.pages[i][j] = entry;
                if (currentlyGuiding != null) {
                    if (Objects.equals(entry.teamId, currentlyGuiding.teamID()) && Objects.equals(entry.pathId, currentlyGuiding.pathID())) {
                        this.currentlyGuiding = entry;
                    }
                }
            }
        }

        this.isClient = false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int p_38942_) {throw new UnsupportedOperationException();}

    @Override
    public boolean stillValid(Player player) {
        return player.level().isClientSide || player.getMainHandItem() == itemStack || player.getOffhandItem() == itemStack;
    }

    public Entry[] currentPageEntries() {
        return this.pages[this.pageNo];
    }

    public void flipPage(boolean forward) {
        this.pageNo = forward ? Math.min(pages.length - 1, pageNo + 1) : Math.max(0, pageNo - 1);
        var pages = this.currentPageEntries();
        if (this.isClient && pages.length == 0) new PathNaviPageRequestPacket(pageNo).send();
    }

    public int getPages() {return pages.length;}

    public int getPageNo() {return pageNo;}

    public boolean isClient() {return isClient;}

    public static int getPages(Collection<Entry> collection) {
        return (int) Math.ceil((double) collection.size() / PathNavigatorMenu.PAGE_SIZE);
    }

    public void sendPage(ServerPlayer target, int pageNoRaw) {
        if (!this.isClient && 0 <= pageNoRaw && pageNoRaw < this.pages.length) {
            this.pageNo = pageNoRaw;
            new PathNaviPageResponsePacket(pageNoRaw, Arrays.asList(this.currentPageEntries())).send(target);
        }
    }

    public void receivePage(int pageNo, Entry[] entries) {
        this.pages[pageNo] = entries;
    }

    public @Nullable Entry getCurrentlyGuiding() {
        return this.currentlyGuiding;
    }

    public void updateCurrentlyGuidingUnsafe(Entry guiding) {
        this.currentlyGuiding = guiding;
    }

    public void updateCurrentlyGuiding(String teamId, String pathId) {
        for (var page : pages) {
            for (var entry : page) {
                if (Objects.equals(entry.teamId, teamId) && Objects.equals(entry.pathId, pathId)) {
                    this.currentlyGuiding = entry;
                    break;
                }
            }
        }
    }

    public record Entry(String teamId, String pathId, String pointName, float distance, float progress) {
        public void writeToBuf(FriendlyByteBuf buf) {
            buf.writeUtf(this.teamId);
            buf.writeUtf(this.pathId);
            buf.writeUtf(this.pointName);
            buf.writeFloat(this.distance);
            buf.writeFloat(this.progress);
        }

        public static Entry readFromBuf(FriendlyByteBuf buf) {return new Entry(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readFloat(), buf.readFloat());}
    }
}
