package org.teacon.checkin.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import org.teacon.checkin.network.capability.CheckInPoints;
import org.teacon.checkin.network.capability.PathPointData;
import org.teacon.checkin.network.capability.UniquePointData;
import org.teacon.checkin.utils.TextComponent;
import org.teacon.checkin.world.level.block.PointPathBlock;
import org.teacon.checkin.world.level.block.PointUniqueBlock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Function;

public class CheckMeInCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, @SuppressWarnings("unused") CommandBuildContext buildContext) {
        //  /checkmein list unique_point point_name|team_id
        dispatcher.register(Commands.literal("checkmein").requires(sourceStack -> sourceStack.hasPermission(2))
                .then(Commands.literal("list")
                        .then(Commands.literal(PointUniqueBlock.NAME)
                                .then(Commands.literal("point_name").executes(context -> listUniquePoints(context, UniquePointData::pointName)))
                                .then(Commands.literal("team_id").executes(context -> listUniquePoints(context, UniquePointData::teamID))))
                        .then(Commands.literal(PointPathBlock.NAME)
                                .then(Commands.literal("point_name").executes(context -> listPathPoints(context, PathPointData::pointName)))
                                .then(Commands.literal("team_id").executes(context -> listPathPoints(context, PathPointData::teamID)))
                                .then(Commands.literal("path_id").executes(context -> listPathPoints(context, PathPointData::pathID)))
                        )));
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
            output.append(Component.translatable("commands.check_in.no_found", Component.translatable("block.check_in.point_unique")));
        } else {
            output.append("\n").append(Component.translatable("commands.check_in.found_total", components.size(), Component.translatable("block.check_in.point_unique")));
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
            output.append(Component.translatable("commands.check_in.no_found", Component.translatable("block.check_in.point_unique")));
        } else {
            output.append("\n").append(Component.translatable("commands.check_in.found_total", components.size(), Component.translatable("block.check_in.point_unique")));
        }

        context.getSource().sendSuccess(() -> output, true);
        return components.size();
    }
}
