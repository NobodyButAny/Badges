package com.batoni.badges.command;

import com.batoni.badges.Badges;
import com.batoni.badges.Util;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import org.bukkit.entity.Player;

import java.util.AbstractMap;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class Badge {
    public static LiteralCommandNode<CommandSourceStack> buildCommand() {
        var grantCommand = Commands.literal("grant")
                .requires(Util.checkPermission("badges.grant"))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(Util.suggestOnlinePlayers())
                        .then(Commands.argument("badge", StringArgumentType.word())
                                .executes(Badge::grantBadge)));

        var takeCommand = Commands.literal("take")
                .requires(Util.checkPermission("badges.take"))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(Util.suggestRegisteredPlayers())
                        .then(Commands.argument("badge", StringArgumentType.word())
                                .executes(Badge::takeOwned)));

        var listCommand = Commands.literal("list")
                .executes(Badge::list)
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(Util.suggestOnlinePlayers())
                        .executes(Badge::listPlayer));

        return Commands.literal("badge")
                .then(grantCommand)
                .then(takeCommand)
                .then(listCommand)
                .build();
    }

    private static int grantBadge(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();

        Player player = Util.getPlayerArgument(ctx, "player");
        String badgeId = StringArgumentType.getString(ctx, "badge");

        if (player == null) {
            sender.sendMessage("Player not found");
            return Command.SINGLE_SUCCESS;
        }

        if (!Badges.getRegisteredBadges().containsKey(badgeId)) {
            sender.sendMessage(badgeId + " not found");
            return Command.SINGLE_SUCCESS;
        }

        var badgeStore = Badges.getBadgeStore();

        if (badgeStore.getOwnedBadges(player.getUniqueId()).contains(badgeId)) {
            sender.sendMessage("%s already owns %s".formatted(player.getName(), badgeId));
            return Command.SINGLE_SUCCESS;
        }

        badgeStore.addOwnedBadges(player.getUniqueId(), badgeId);
        sender.sendMessage("Successfully granted %s to %s".formatted(badgeId, player.getName()));
        return Command.SINGLE_SUCCESS;
    }

    private static int takeOwned(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();

        Player player = Util.getPlayerArgument(ctx, "player");
        String badgeId = StringArgumentType.getString(ctx, "badge");

        if (player == null) {
            sender.sendMessage("Player not found");
            return Command.SINGLE_SUCCESS;
        }

        if (!Badges.getRegisteredBadges().containsKey(badgeId)) {
            sender.sendMessage(badgeId + " not found");
            return Command.SINGLE_SUCCESS;
        }

        Badges.getBadgeStore().removeOwnedBadges(player.getUniqueId(), badgeId);
        Badges.getBadgeStore().removeWearingBadges(player.getUniqueId(), badgeId);
        sender.sendMessage("Successfully taken %s from %s".formatted(badgeId, player.getName()));
        return Command.SINGLE_SUCCESS;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        sender.sendMessage("Currently loaded badges: ");

        Badges.getRegisteredBadges()
                .forEach((badgeId, badge) ->
                        sender.sendMessage(String.format("- %s (%s)", badgeId, badge)));

        if (Badges.getRegisteredBadges().isEmpty()) {
            sender.sendMessage("None");
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int listPlayer(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        Map<String, String> badgeRegistry = Badges.getRegisteredBadges();

        Player player = Util.getPlayerArgument(ctx, "player");
        if (player == null) {
            ctx.getSource().getSender().sendMessage("Player not found");
            return Command.SINGLE_SUCCESS;
        }
        String playerName = player.getName();

        sender.sendMessage("Currently owned badges (%s): ".formatted(playerName));
        for (String badgeId : Badges.getBadgeStore().getOwnedBadges(player.getUniqueId())) {
            var badge = badgeRegistry.getOrDefault(badgeId, "unknown");
            sender.sendMessage("- %s (%s)".formatted(badgeId, badge));
        }

        sender.sendMessage("Currently worn badges (%s): ".formatted(playerName));
        for (String badgeId : Badges.getBadgeStore().getWearingBadges(player.getUniqueId())) {
            var badge = badgeRegistry.getOrDefault(badgeId, "unknown");
            sender.sendMessage("- %s (%s)".formatted(badgeId, badge));
        }

        return Command.SINGLE_SUCCESS;
    }
}
