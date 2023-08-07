package org.teacon.checkin.server.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import org.teacon.checkin.network.capability.CheckInPoints;
import org.teacon.checkin.network.capability.PathPointData;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class PointPathArgument implements ArgumentType<PathPointData.TeamPathID> {
    public static final SimpleCommandExceptionType ERROR_NOT_COMPLETE = new SimpleCommandExceptionType(Component.translatable("argument.check_in.point_path.not_complete"));
    public static final Dynamic2CommandExceptionType NO_POINT_FOUND = new Dynamic2CommandExceptionType((arg1, arg2) ->
            Component.translatable("argument.check_in.point_path.not_found", arg1, arg2));

    @Override
    public PathPointData.TeamPathID parse(StringReader reader) throws CommandSyntaxException {
        var head = reader.getCursor();
        var teamID = reader.readString();
        if (!teamID.isBlank() && reader.canRead() && reader.peek() == ' ') {
            reader.skip();
            var pathID = reader.readString();
            if (!pathID.isBlank()) return new PathPointData.TeamPathID(teamID, pathID);
        }
        reader.setCursor(head);
        throw ERROR_NOT_COMPLETE.createWithContext(reader);
    }

    public static CompletableFuture<Suggestions> suggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        var remaining = builder.getRemaining();
        var reader = new StringReader(remaining);
        String teamID = null;
        try {teamID = reader.readString();} catch (CommandSyntaxException ignored) {}
        if (teamID == null || teamID.isBlank() // when teamID is invalid or unfinished
                || !reader.canRead() || reader.peek() != ' ') { // or when there is not a space after teamID
            // suggest teamID if the pathID is not started
            var suggestions = StreamSupport.stream(context.getSource().getServer().getAllLevels().spliterator(), false)
                    .map(lvl -> CheckInPoints.of(lvl).resolve())
                    .filter(Optional::isPresent)
                    .flatMap(opt -> opt.get().getTeamPathIDs().stream())
                    .map(PathPointData.TeamPathID::teamID)
                    .map(PointUniqueArgument::escapeString)
                    .collect(Collectors.toSet());
            return SharedSuggestionProvider.suggest(suggestions, builder);
        } else { // suggest pathID when teamID is completed and pathID starts
            final String fteamID = teamID;
            var suggestions = StreamSupport.stream(context.getSource().getServer().getAllLevels().spliterator(), false)
                    .map(lvl -> CheckInPoints.of(lvl).resolve())
                    .filter(Optional::isPresent)
                    .flatMap(opt -> opt.get().getTeamPathIDs().stream())
                    .filter(id -> id.teamID().equals(fteamID))
                    .map(id -> remaining.substring(0, reader.getCursor()) + " " + PointUniqueArgument.escapeString(id.pathID()))
                    .collect(Collectors.toSet());
            return SharedSuggestionProvider.suggest(suggestions, builder);
        }
    }

    public static PathPointData.TeamPathID getPoint(CommandContext<CommandSourceStack> context, String id) throws CommandSyntaxException {
        var teamPathID = context.getArgument(id, PathPointData.TeamPathID.class);

        return StreamSupport.stream(context.getSource().getServer().getAllLevels().spliterator(), false)
                .map(lvl -> CheckInPoints.of(lvl).resolve())
                .filter(Optional::isPresent)
                .flatMap(opt -> opt.get().getTeamPathIDs().stream())
                .filter(p -> p.equals(teamPathID))
                .findAny()
                .orElseThrow(() -> NO_POINT_FOUND.create(teamPathID.teamID(), teamPathID.pathID()));
    }
}
