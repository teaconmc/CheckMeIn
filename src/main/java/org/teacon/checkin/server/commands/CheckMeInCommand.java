package org.teacon.checkin.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.teacon.checkin.network.capability.CheckInPoints;
import org.teacon.checkin.network.capability.CheckProgress;
import org.teacon.checkin.network.capability.PathPointData;
import org.teacon.checkin.network.capability.UniquePointData;
import org.teacon.checkin.utils.TextComponent;
import org.teacon.checkin.world.level.block.PointPathBlock;
import org.teacon.checkin.world.level.block.PointUniqueBlock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.minecraft.commands.Commands.*;

public class CheckMeInCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, @SuppressWarnings("unused") CommandBuildContext buildContext) {
        //  /checkmein list unique_point point_name|team_id
        dispatcher.register(literal("checkmein").requires(sourceStack -> sourceStack.hasPermission(2))
                .then(literal("list")
                        .then(literal(PointUniqueBlock.NAME)
                                .then(literal("point_name").executes(context -> listUniquePoints(context, UniquePointData::pointName)))
                                .then(literal("team_id").executes(context -> listUniquePoints(context, UniquePointData::teamID))))
                        .then(literal(PointPathBlock.NAME)
                                .then(literal("point_name")
                                        .then(argument("point_name", StringArgumentType.string())
                                                .executes(context -> listPathPointsGroupingByPathIdOnly(context, null, context.getArgument("point_name", String.class))))
                                        .executes(context -> listPathPointsGroupingByPathIdOnly(context, null, null)))
                                .then(literal("team_id").executes(context -> listPathPoints(context, PathPointData::teamID)))
                                .then(literal("path_id")
                                        .then(argument("path_id", StringArgumentType.string())
                                                .executes(context -> listPathPointsGroupingByPathIdOnly(context, context.getArgument("path_id", String.class), null)))
                                        .executes(context -> listPathPointsGroupingByPathIdOnly(context, null, null))))
                ).then(literal("reset")
                        .then(literal(PointUniqueBlock.NAME)
                                .then(argument("targets", EntityArgument.players())
                                        .then(argument("points", new PointUniqueArgument()).suggests(PointUniqueArgument::suggestions)
                                                .executes(context -> resetUniquePointProgress(context,
                                                        EntityArgument.getPlayers(context, "targets"), PointUniqueArgument.getPoint(context, "points"))))))
                        .then(literal(PointPathBlock.NAME)
                                .then(argument("targets", EntityArgument.players())
                                        .then(argument("points", new PointPathArgument()).suggests(PointPathArgument::suggestions)
                                                .executes(context -> resetPathProgress(context,
                                                        EntityArgument.getPlayers(context, "targets"), PointPathArgument.getPoint(context, "points"))))))
                ).then(literal("reset-all")
                        .then(literal(PointUniqueBlock.NAME)
                                .then(argument("targets", EntityArgument.players())
                                        .executes(context -> resetAllUniquePointProgress(context, EntityArgument.getPlayers(context, "targets")))))
                        .then(literal(PointPathBlock.NAME)
                                .then(argument("targets", EntityArgument.players())
                                        .executes(context -> resetAllPathProgress(context, EntityArgument.getPlayers(context, "targets"))))
                        )
                )
        );
    }

    private static int resetAllUniquePointProgress(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets) {
        targets.forEach(target -> CheckProgress.of(target).ifPresent(CheckProgress::resetAllUniquePoints));

        if (targets.size() == 1) {
            context.getSource().sendSuccess(() -> Component.translatable("commands.check_in.reset.unique.success.all.single",
                    targets.iterator().next().getDisplayName()), true);
        } else {
            context.getSource().sendSuccess(() -> Component.translatable("commands.check_in.reset.unique.success.all.multiple"), true);
        }
        return targets.size();
    }

    private static int resetUniquePointProgress(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets, UniquePointData data) {
        targets.forEach(target -> CheckProgress.of(target).ifPresent(progress -> progress.resetUniquePoint(data.teamID())));

        if (targets.size() == 1) {
            context.getSource().sendSuccess(() -> Component.translatable("commands.check_in.reset.unique.success.single",
                    data.teamID(), targets.iterator().next().getDisplayName()), true);
        } else {
            context.getSource().sendSuccess(() -> Component.translatable("commands.check_in.reset.unique.success.multiple", data.teamID()), true);
        }
        return targets.size();
    }

    private static int resetAllPathProgress(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets) {
        targets.forEach(target -> CheckProgress.of(target).ifPresent(CheckProgress::resetAllPaths));

        if (targets.size() == 1) {
            context.getSource().sendSuccess(() -> Component.translatable("commands.check_in.reset.path.success.all.single",
                    targets.iterator().next().getDisplayName()), true);
        } else {
            context.getSource().sendSuccess(() -> Component.translatable("commands.check_in.reset.path.success.all.multiple"), true);
        }
        return targets.size();
    }

    private static int resetPathProgress(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets, PathPointData.TeamPathID data) {
        targets.forEach(target -> CheckProgress.of(target).ifPresent(progress -> progress.resetPath(data.teamID(), data.pathID())));

        if (targets.size() == 1) {
            context.getSource().sendSuccess(() -> Component.translatable("commands.check_in.reset.path.success.single",
                    data.teamID(), data.pathID(), targets.iterator().next().getDisplayName()), true);
        } else {
            context.getSource().sendSuccess(() -> Component.translatable("commands.check_in.reset.path.success.multiple", data.teamID(), data.pathID()), true);
        }
        return targets.size();
    }

    /**
     * List all points with chosen displaying string <b>sorted</b>
     *
     * @param displayFunc the function for getting the string to display for each point
     * @return number of points listed
     */
    private static int listUniquePoints(CommandContext<CommandSourceStack> context, Function<UniquePointData, String> displayFunc) {
        var components = new ArrayList<Component>();
        for (var level : context.getSource().getServer().getAllLevels()) {
            var capOpt = level.getCapability(CheckInPoints.Provider.CAPABILITY).resolve();
            capOpt.ifPresent(checkInPoints -> checkInPoints.getAllUniquePoints()
                    .stream().sorted(Comparator.comparing(displayFunc))
                    .forEachOrdered(data -> components.add(Component.literal(displayFunc.apply(data)).withStyle(Style.EMPTY
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, data.toTextComponent(level)))
                            .withClickEvent(TextComponent.teleportTo(data.pos(), level))))));
        }
        var output = Component.literal("");
        for (int i = 0; i < components.size(); i++) {
            output.append(components.get(i));
            if (i < components.size() - 1) output.append(", ");
        }
        if (components.isEmpty()) {
            output.append(Component.translatable("commands.check_in.list.success.none", Component.translatable("block.check_in.point_unique")));
        } else {
            output.append("\n").append(Component.translatable("commands.check_in.list.success.some", components.size(), Component.translatable("block.check_in.point_unique")));
        }

        context.getSource().sendSuccess(() -> output, true);
        return components.size();
    }

    private static int listPathPoints(CommandContext<CommandSourceStack> context, Function<PathPointData, String> displayFunc) {
        var output = Component.literal("");
        int totalCount = 0;
        for (var level : context.getSource().getServer().getAllLevels()) {
            var capOpt = level.getCapability(CheckInPoints.Provider.CAPABILITY).resolve();
            if (capOpt.isPresent()) {
                var pointsByTeamId = capOpt.get().getAllPathPoints()
                        .stream()
                        .sorted(Comparator.comparing(displayFunc).thenComparingInt(d -> d.ord() == null ? -1 : d.ord()))
                        .collect(Collectors.groupingBy(PathPointData::teamID));
                for (var entry : pointsByTeamId.entrySet()) {
                    output.append(Component.literal(entry.getKey()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                            .append("\n");
                    totalCount += entry.getValue().size();
                    var pointsByPathId = entry.getValue().stream().collect(Collectors.groupingBy(PathPointData::pathID));
                    for (var pathEntry : pointsByPathId.entrySet()) {
                        output.append("  - ")
                                .append(Component.literal(pathEntry.getKey()).withStyle(ChatFormatting.GOLD))
                                .append(": ");
                        var points = new ArrayList<>(pathEntry.getValue());
                        points.sort(Comparator.comparingInt(d -> d.ord() == null ? -1 : d.ord()));
                        for (int i = 0; i < points.size(); i++) {
                            var data = points.get(i);
                            output.append(Component.literal(data.pointName()).withStyle(Style.EMPTY
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, data.toTextComponent(level)))
                                    .withClickEvent(TextComponent.teleportTo(data.pos(), level))));
                            if (i < points.size() - 1) {
                                output.append(", ");
                            } else {
                                output.append("\n");
                            }
                        }
                    }

                }
            }
        }
        if (totalCount <= 0) {
            output.append(Component.translatable("commands.check_in.list.success.none", Component.translatable("block.check_in.point_path")));
        } else {
            output.append("------------\n").append(Component.translatable("commands.check_in.list.success.some", totalCount, Component.translatable("block.check_in.point_path")));
        }

        context.getSource().sendSuccess(() -> output, true);
        return totalCount;
    }

    private static int listPathPointsGroupingByPathIdOnly(CommandContext<CommandSourceStack> context, @Nullable String onlyThisPathId, @Nullable String onlyThisPointName) {
        var output = Component.literal("");
        int totalCount = 0;
        for (var level : context.getSource().getServer().getAllLevels()) {
            var capOpt = level.getCapability(CheckInPoints.Provider.CAPABILITY).resolve();
            if (capOpt.isPresent()) {
                var pointsByPathId = capOpt.get().getAllPathPoints()
                        .stream()
                        .filter(p -> (onlyThisPathId == null || onlyThisPathId.equals(p.pathID())) && (onlyThisPointName == null || onlyThisPointName.equals(p.pointName())))
                        .sorted(Comparator.comparing(PathPointData::pathID).thenComparingInt(d -> d.ord() == null ? -1 : d.ord()))
                        .collect(Collectors.groupingBy(PathPointData::pathID));
                for (var pathEntry : pointsByPathId.entrySet()) {
                    totalCount += pathEntry.getValue().size();
                    output.append("- ")
                            .append(Component.literal(pathEntry.getKey()).withStyle(ChatFormatting.GOLD))
                            .append(": ");
                    var points = new ArrayList<>(pathEntry.getValue());
                    points.sort(Comparator.comparingInt(d -> d.ord() == null ? -1 : d.ord()));
                    for (int i = 0; i < points.size(); i++) {
                        var data = points.get(i);
                        output.append(Component.literal(data.pointName()).withStyle(Style.EMPTY
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, data.toTextComponent(level)))
                                .withClickEvent(TextComponent.teleportTo(data.pos(), level))));
                        if (i < points.size() - 1) {
                            output.append(", ");
                        } else {
                            output.append("\n");
                        }
                    }

                }
            }
        }
        if (totalCount <= 0) {
            output.append(Component.translatable("commands.check_in.list.success.none", Component.translatable("block.check_in.point_path")));
        } else {
            output.append("------------\n").append(Component.translatable("commands.check_in.list.success.some", totalCount, Component.translatable("block.check_in.point_path")));
        }

        context.getSource().sendSuccess(() -> output, true);
        return totalCount;
    }
}
