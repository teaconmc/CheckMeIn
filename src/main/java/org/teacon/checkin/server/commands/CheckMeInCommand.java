package org.teacon.checkin.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import org.teacon.checkin.network.capability.CheckInPoints;
import org.teacon.checkin.network.capability.CheckProgress;
import org.teacon.checkin.network.capability.PathPointData;
import org.teacon.checkin.network.capability.UniquePointData;
import org.teacon.checkin.utils.TextComponent;
import org.teacon.checkin.world.level.block.PointPathBlock;
import org.teacon.checkin.world.level.block.PointUniqueBlock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.Function;

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
                                .then(literal("point_name").executes(context -> listPathPoints(context, PathPointData::pointName)))
                                .then(literal("team_id").executes(context -> listPathPoints(context, PathPointData::teamID)))
                                .then(literal("path_id").executes(context -> listPathPoints(context, PathPointData::pathID))))
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
        var components = new ArrayList<Component>();
        for (var level : context.getSource().getServer().getAllLevels()) {
            var capOpt = level.getCapability(CheckInPoints.Provider.CAPABILITY).resolve();
            capOpt.ifPresent(checkInPoints -> checkInPoints.getAllPathPoints()
                    .stream().sorted(Comparator.comparing(displayFunc).thenComparingInt(d -> d.ord() == null ? -1 : d.ord()))
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
            output.append(Component.translatable("commands.check_in.list.success.none", Component.translatable("block.check_in.point_path")));
        } else {
            output.append("\n").append(Component.translatable("commands.check_in.list.success.some", components.size(), Component.translatable("block.check_in.point_path")));
        }

        context.getSource().sendSuccess(() -> output, true);
        return components.size();
    }
}
