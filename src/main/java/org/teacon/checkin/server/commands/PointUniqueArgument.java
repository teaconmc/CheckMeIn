package org.teacon.checkin.server.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import org.teacon.checkin.network.capability.CheckInPoints;
import org.teacon.checkin.network.capability.UniquePointData;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class PointUniqueArgument implements ArgumentType<String> {
    public static final DynamicCommandExceptionType NO_POINT_FOUND = new DynamicCommandExceptionType(teamID ->
            Component.translatable("argument.check_in.point_unique.not_found", teamID));

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        return reader.readString();
    }

    public static CompletableFuture<Suggestions> suggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(StreamSupport.stream(context.getSource().getServer().getAllLevels().spliterator(), false)
                .map(lvl -> CheckInPoints.of(lvl).resolve())
                .filter(Optional::isPresent)
                .flatMap(opt -> opt.get().getAllUniquePoints().stream())
                .map(UniquePointData::teamID)
                .map(PointUniqueArgument::escapeString)
                .collect(Collectors.toSet()), builder);
    }

    public static UniquePointData getPoint(CommandContext<CommandSourceStack> context, String id) throws CommandSyntaxException {
        var teamID = context.getArgument(id, String.class);
        return StreamSupport.stream(context.getSource().getServer().getAllLevels().spliterator(), false)
                .map(lvl -> CheckInPoints.of(lvl).resolve())
                .filter(Optional::isPresent)
                .flatMap(opt -> opt.get().getAllUniquePoints().stream())
                .filter(p -> p.teamID().equals(teamID))
                .findAny()
                .orElseThrow(() -> NO_POINT_FOUND.create(teamID));
    }

    protected static String escapeString(String str) {
        var chars = str.toCharArray();
        var quote = false;
        for (var chr : chars) {
            if (!StringReader.isAllowedInUnquotedString(chr)) {
                quote = true;
                break;
            }
        }
        if (!quote) return str;

        var sb = new StringBuilder().append('"');
        for (var chr : chars) {
            if (chr == '"') sb.append('"');
            sb.append(chr);
        }
        sb.append('"');
        return sb.toString();
    }
}
