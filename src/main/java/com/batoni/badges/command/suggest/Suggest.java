package com.batoni.badges.command.suggest;

import com.batoni.badges.Badges;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class Suggest {
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

    public static SuggestionProvider<CommandSourceStack> suggestRegisteredBadges(){
        return (ctx, builder) -> {
            Badges.getRegisteredBadges().keySet().forEach(builder::suggest);
            return builder.buildFuture();
        };
    }

    public static SuggestionProvider<CommandSourceStack> suggestOwnedBadges() {
        return (ctx, builder) -> {
            var sender = ctx.getSource().getSender();
            if (!(sender instanceof Player player)) {
                return builder.buildFuture();
            }
            Badges.getBadgeStore()
                    .getOwnedBadges(player.getUniqueId())
                    .forEach(builder::suggest);
            return builder.buildFuture();
        };
    }

    public static SuggestionProvider<CommandSourceStack> suggestWornBadges() {
        return (ctx, builder) -> {
            var sender = ctx.getSource().getSender();
            if (!(sender instanceof Player player)) {
                return builder.buildFuture();
            }
            Badges.getBadgeStore()
                    .getWearingBadges(player.getUniqueId())
                    .forEach(builder::suggest);
            return builder.buildFuture();
        };
    }
}
