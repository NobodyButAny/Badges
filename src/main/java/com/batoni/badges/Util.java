package com.batoni.badges;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.function.Predicate;

public class Util {
    public static Predicate<CommandSourceStack> checkPermission(String permission) {
        return ctx -> {
            if (!ctx.getSender().hasPermission(permission)) {
                ctx.getSender().sendMessage("You don't have permission to use that command!");
                return false;
            }
            return true;
        };
    }

    public static SuggestionProvider<CommandSourceStack> suggestOnlinePlayers() {
        return (ctx, builder) -> {
            var players = Badges.getInstance().getServer().getOnlinePlayers();
            players.stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                    .forEach(builder::suggest);
            return builder.buildFuture();
        };
    }

    public static SuggestionProvider<CommandSourceStack> suggestRegisteredPlayers() {
        return (ctx, builder) -> {
            var players = Badges.getBadgeStore().
                    getRegisteredIds()
                    .stream()
                    .map(Bukkit::getPlayer);

            players.map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                    .forEach(builder::suggest);

            return builder.buildFuture();
        };
    }

    public static Player getPlayerArgument(CommandContext<CommandSourceStack> ctx, String name) {
        var server = Badges.getInstance().getServer();

        String playerName = StringArgumentType.getString(ctx, name);
        return server.getPlayerExact(playerName);
    }
}
