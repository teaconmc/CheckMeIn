package org.teacon.checkin.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import org.teacon.checkin.network.capability.CheckInPoints;
import org.teacon.checkin.network.capability.UniquePointData;

import java.util.ArrayList;
import java.util.function.Function;

public class CheckMeInCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        //  /checkmein list unique_point point_name|team_id
        dispatcher.register(Commands.literal("checkmein").requires(sourceStack -> sourceStack.hasPermission(2))
                .then(Commands.literal("list")
                        .then(Commands.literal("unique_point")
                                .then(Commands.literal("point_name").executes(context -> listUniquePoints(context, UniquePointData::pointName)))
                                .then(Commands.literal("team_id").executes(context -> listUniquePoints(context, UniquePointData::teamID))))));
    }

    private static int listUniquePoints(CommandContext<CommandSourceStack> context, Function<UniquePointData, String> displayFunc) {
        var components = new ArrayList<Component>();
        for (var level : context.getSource().getServer().getAllLevels()) {
            var capOpt = level.getCapability(CheckInPoints.Provider.CAPABILITY).resolve();
            if (capOpt.isPresent()) {
                for (var data : capOpt.get().getAllUniquePoints()) {
                    int x = data.pos().getX(), y = data.pos().getY(), z = data.pos().getZ();
                    String dim = level.dimensionTypeId().location().toString();
                    components.add(Component.literal(displayFunc.apply(data)).withStyle(Style.EMPTY
                            .withHoverEvent(new HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT, Component.translatable("commands.check_in.unique_point_hover",
                                    Component.translatable("container.check_in.team_id"), data.teamID(),
                                    Component.translatable("container.check_in.point_name"), data.pointName(),
                                    x, y, z, dim)))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    "/execute as @p in %s run teleport %d %d %d".formatted(dim, x, y, z)))));
                }
            }
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
