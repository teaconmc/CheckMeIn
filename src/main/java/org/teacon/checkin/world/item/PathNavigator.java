package org.teacon.checkin.world.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.teacon.checkin.network.capability.CheckInPoints;
import org.teacon.checkin.network.capability.CheckProgress;
import org.teacon.checkin.network.capability.PathPointData;
import org.teacon.checkin.world.inventory.PathNavigatorMenu;

import java.util.*;

public class PathNavigator extends Item {
    public static final String NAME = "nvg_path";
    /**
     * sorting: (in progress < not started < finished).then(larger progress < smaller progress).then(closer < further)
     */
    private static final Comparator<PathNavigatorMenu.Entry> SORTER = Comparator
            .<PathNavigatorMenu.Entry>comparingInt(e -> e.progress() >= 1 ? 2 : e.progress() <= 0 ? 1 : 0)
            .thenComparingDouble(e -> -e.progress())
            .thenComparingDouble(PathNavigatorMenu.Entry::distance)
            .thenComparing(PathNavigatorMenu.Entry::pointName);

    public PathNavigator(Properties prop) {super(prop);}

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);

        if (!level.isClientSide) openScreen((ServerPlayer) player, stack);

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static void openScreen(ServerPlayer player, ItemStack stack) {
        CheckProgress.of(player).ifPresent(progress -> {
            var entries = getMenuEntries(player.level(), player, progress);
            NetworkHooks.openScreen(player,
                    new SimpleMenuProvider((id, inv, pl) -> new PathNavigatorMenu(id, stack, entries, progress.getCurrentlyGuiding()), Component.literal(PathNavigator.NAME)),
                    buf -> {
                        var currentlyGuiding = progress.getCurrentlyGuiding();
                        if (currentlyGuiding != null) {
                            buf.writeBoolean(true);
                            buf.writeUtf(currentlyGuiding.teamID());
                            buf.writeUtf(currentlyGuiding.pathID());
                        } else {
                            buf.writeBoolean(false);
                        }
                        buf.writeInt(PathNavigatorMenu.getPages(entries));
                        int n = 0;
                        for (PathNavigatorMenu.Entry entry : entries) {
                            if (n >= PathNavigatorMenu.PAGE_SIZE) break;
                            entry.writeToBuf(buf);
                            n++;
                        }
                    });
        });
    }

    @NotNull
    private static List<PathNavigatorMenu.Entry> getMenuEntries(Level level, Player player, CheckProgress progress) {
        // brute-force loop, may lead to lags in large-scale usage
        // don't optimize now, but prioritize it when mspt is high
        var levelCaps = new ArrayList<Pair<Level, CheckInPoints>>();
        var ids = new HashSet<PathPointData.TeamPathID>();
        for (var lvl : ((ServerLevel) level).getServer().getAllLevels()) {
            var pointsOpt = CheckInPoints.of(lvl).resolve();
            if (pointsOpt.isPresent()) {
                levelCaps.add(Pair.of(lvl, pointsOpt.get()));
                ids.addAll(pointsOpt.get().getTeamPathIDs());
            }
        }

        var list = new ArrayList<PathNavigatorMenu.Entry>();
        var playerPos = player.position();
        for (var id : ids) {
            var teamID = id.teamID();
            var pathID = id.pathID();

            var lastChecked = progress.lastCheckedOrd(teamID, pathID);
            if (lastChecked == null) lastChecked = -1;
            int totalOrds = 0;
            PathPointData first = null, next = null;
            Level nextLvl = null;
            short checked = 0;

            for (var lvlCap : levelCaps) {
                var lvl = lvlCap.getLeft();
                var points = lvlCap.getRight().nonnullOrdPathPoints(teamID, pathID);
                totalOrds += points.size();
                for (var data : points) {
                    short ord = Objects.requireNonNull(data.ord());
                    if (ord <= lastChecked) {
                        checked++;
                    } else if (next == null || Objects.requireNonNull(next.ord()) > ord) {
                        next = data;
                        nextLvl = lvl;
                    }
                    if (first == null || ord < Objects.requireNonNull(first.ord())) {
                        first = data;
                    }
                }
            }
            if (totalOrds > 0) {
                var dist = next == null ? Float.NaN : nextLvl != level ? Float.POSITIVE_INFINITY : (float) Vec3.atCenterOf(next.pos()).distanceTo(playerPos);
                list.add(new PathNavigatorMenu.Entry(teamID, pathID, first.pointName(), dist, (float) checked / (float) totalOrds));
            }
        }
        return list.stream().sorted(SORTER).toList();
    }
}
