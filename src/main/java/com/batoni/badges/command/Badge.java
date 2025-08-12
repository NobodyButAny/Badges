package com.batoni.badges.command;

import com.batoni.badges.Badges;
import com.batoni.badges.Util;
import com.batoni.badges.command.suggest.Suggest;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class Badge {
    public static LiteralCommandNode<CommandSourceStack> buildCommand() {
        var grantCommand = Commands.literal("grant")
                .requires(Util.checkPermission("badges.grant"))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(Suggest.suggestOnlinePlayers())
                        .then(Commands.argument("badge", StringArgumentType.word())
                                .suggests(Suggest.suggestRegisteredBadges())
                                .executes(Badge::grantBadge)));

        var takeCommand = Commands.literal("take")
                .requires(Util.checkPermission("badges.take"))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(Suggest.suggestRegisteredPlayers())
                        .then(Commands.argument("badge", StringArgumentType.word())
                                .suggests(Suggest.suggestRegisteredBadges())
                                .executes(Badge::takeOwned)));

        var wearCommand = Commands.literal("wear")
                .requires(Util.checkPermission("badges.wear"))
                .executes(Badge::wearInterface)
                .then(Commands.argument("slot", IntegerArgumentType.integer())
                        .then(Commands.argument("badge", StringArgumentType.word())
                                .suggests(Suggest.suggestOwnedBadges())
                                .executes(Badge::wearPickSlot)));

        var removeCommand = Commands.literal("remove")
                .requires(Util.checkPermission("badges.remove"))
                .then(Commands.argument("badge", StringArgumentType.word())
                        .suggests(Suggest.suggestWornBadges())
                        .executes(Badge::removeWearing));

        var listCommand = Commands.literal("list")
                .requires(Util.checkPermission("badges.list"))
                .executes(Badge::list)
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(Suggest.suggestOnlinePlayers())
                        .executes(Badge::listPlayer));

        return Commands.literal("badge")
                .then(grantCommand)
                .then(takeCommand)
                .then(wearCommand)
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

    private static int wearInterface(CommandContext<CommandSourceStack> ctx) {
        return Command.SINGLE_SUCCESS;
    }

    private static int wearPickSlot(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can wear badges!");
            return Command.SINGLE_SUCCESS;
        }

        String badgeId = StringArgumentType.getString(ctx, "badge");
        if (!Badges.getRegisteredBadges().containsKey(badgeId)) {
            sender.sendMessage(badgeId + " not found!");
            return Command.SINGLE_SUCCESS;
        }

        var badgeStore = Badges.getBadgeStore();
        var ownedBadges = badgeStore.getOwnedBadges(player.getUniqueId());
        if (ownedBadges.isEmpty()) {
            sender.sendMessage("You don't own any badges!");
            return Command.SINGLE_SUCCESS;
        }

        if (!ownedBadges.contains(badgeId)) {
            sender.sendMessage("You don't own " + badgeId);
            return Command.SINGLE_SUCCESS;
        }

        var wornBadges = badgeStore.getWearingBadges(player.getUniqueId());
        int slot = IntegerArgumentType.getInteger(ctx, "slot");
        if (slot > wornBadges.size() + 1) {
            sender.sendMessage("You can assign only up to %s slots".formatted(wornBadges.size() + 1));
            return Command.SINGLE_SUCCESS;
        }

        if (!wornBadges.contains(badgeId)) {
            wornBadges.add(slot - 1, badgeId);
        } else {
            int curr = wornBadges.indexOf(badgeId);
            Collections.swap(wornBadges, curr, slot - 1);
        }

        badgeStore.setWearingBadges(player.getUniqueId(), wornBadges);

        // TODO: call to update LP-managing service

        sender.sendMessage("Successfully updated slot %s".formatted(slot));

        return Command.SINGLE_SUCCESS;
    }

    private static int removeWearing(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can wear badges!");
            return Command.SINGLE_SUCCESS;
        }

        var badgeStore = Badges.getBadgeStore();
        String badgeId = StringArgumentType.getString(ctx, "badgeId");
        if (!Badges.getRegisteredBadges().containsKey(badgeId)) {
            return Command.SINGLE_SUCCESS;
        }

        return Command.SINGLE_SUCCESS;
    }
}
