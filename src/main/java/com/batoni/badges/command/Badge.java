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
        var messageService = Badges.getMessageService();

        Player player = Util.getPlayerArgument(ctx, "player");
        String badgeId = StringArgumentType.getString(ctx, "badge");

        if (player == null) {
            messageService.sendMessage(
                    sender,
                    "obj_not_found",
                    Map.of("obj", "Player"));
            return Command.SINGLE_SUCCESS;
        }

        if (!Badges.getRegisteredBadges().containsKey(badgeId)) {
            messageService.sendMessage(
                    sender,
                    "obj_not_found",
                    Map.of("obj", badgeId));
            return Command.SINGLE_SUCCESS;
        }

        var badgeStore = Badges.getBadgeStore();

        if (badgeStore.getOwnedBadges(player.getUniqueId()).contains(badgeId)) {
            messageService.sendMessage(
                    sender,
                    "badge_grant_already_owned",
                    Map.of(
                            "player", player.getName(),
                            "badge", badgeId
                    )
            );
            return Command.SINGLE_SUCCESS;
        }

        badgeStore.addOwnedBadges(player.getUniqueId(), badgeId);
        messageService.sendMessage(
                sender,
                "badge_grant_success",
                Map.of(
                        "player", player.getName(),
                        "badge", badgeId
                )
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int takeOwned(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        var messageService = Badges.getMessageService();

        Player player = Util.getPlayerArgument(ctx, "player");
        String badgeId = StringArgumentType.getString(ctx, "badge");

        if (player == null) {
            messageService.sendMessage(
                    sender,
                    "obj_not_found",
                    Map.of("obj", "Player"));
            return Command.SINGLE_SUCCESS;
        }

        if (!Badges.getRegisteredBadges().containsKey(badgeId)) {
            messageService.sendMessage(
                    sender,
                    "obj_not_found",
                    Map.of("obj", badgeId));
            return Command.SINGLE_SUCCESS;
        }

        Badges.getBadgeStore().removeOwnedBadges(player.getUniqueId(), badgeId);
        Badges.getBadgeStore().removeWearingBadges(player.getUniqueId(), badgeId);
        messageService.sendMessage(
                sender,
                "badge_take_success",
                Map.of("player", player.getName(),
                        "badge", badgeId)
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        var messageService = Badges.getMessageService();

        messageService.sendMessage(
                sender,
                "badge_list_all",
                Collections.emptyMap()
        );

        Badges.getRegisteredBadges()
                .forEach((badgeId, badge) ->
                        messageService.sendMessage(
                                sender,
                                "badge_list_entry",
                                Map.of("badge_id", badgeId,
                                        "badge_name", badge)
                        )
                );

        if (Badges.getRegisteredBadges().isEmpty()) {
            messageService.sendMessage(
                    sender,
                    "badge_list_all_none",
                    Collections.emptyMap()
            );
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int listPlayer(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        var messageService = Badges.getMessageService();
        Map<String, String> badgeRegistry = Badges.getRegisteredBadges();

        Player player = Util.getPlayerArgument(ctx, "player");
        if (player == null) {
            messageService.sendMessage(
                    sender,
                    "obj_not_found",
                    Map.of("obj", "Player")
            );
            return Command.SINGLE_SUCCESS;
        }
        String playerName = player.getName();

        messageService.sendMessage(
                sender,
                "badge_list_player_owned",
                Map.of("player", playerName)
        );
        for (String badgeId : Badges.getBadgeStore().getOwnedBadges(player.getUniqueId())) {
            var badge = badgeRegistry.getOrDefault(badgeId, "unknown");
            messageService.sendMessage(
                    sender,
                    "badge_list_entry",
                    Map.of("badge_id", badgeId,
                            "badge_name", badge)
            );
        }

        messageService.sendMessage(
                sender,
                "badge_list_player_worn",
                Map.of("player", playerName)
        );
        for (String badgeId : Badges.getBadgeStore().getWearingBadges(player.getUniqueId())) {
            var badge = badgeRegistry.getOrDefault(badgeId, "unknown");
            messageService.sendMessage(
                    sender,
                    "badge_list_entry",
                    Map.of("badge_id", badgeId,
                            "badge_name", badge)
            );
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int wearInterface(CommandContext<CommandSourceStack> ctx) {
        return Command.SINGLE_SUCCESS;
    }

    private static int wearPickSlot(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        var messageService = Badges.getMessageService();

        if (!(sender instanceof Player player)) {
            messageService.sendMessage(sender, "badge_wear_only_players", Collections.emptyMap());
            return Command.SINGLE_SUCCESS;
        }

        String badgeId = StringArgumentType.getString(ctx, "badge");
        if (!Badges.getRegisteredBadges().containsKey(badgeId)) {
            messageService.sendMessage(sender, "obj_not_found", Map.of("obj", badgeId));
            return Command.SINGLE_SUCCESS;
        }

        var badgeStore = Badges.getBadgeStore();
        var ownedBadges = badgeStore.getOwnedBadges(player.getUniqueId());
        if (ownedBadges.isEmpty()) {
            messageService.sendMessage(sender, "badge_wear_none_owned", Collections.emptyMap());
            return Command.SINGLE_SUCCESS;
        }

        if (!ownedBadges.contains(badgeId)) {
            messageService.sendMessage(sender, "badge_wear_not_owned", Map.of("badge", badgeId));
            return Command.SINGLE_SUCCESS;
        }

        var wornBadges = badgeStore.getWearingBadges(player.getUniqueId());
        int slot = IntegerArgumentType.getInteger(ctx, "slot");
        if (slot > wornBadges.size() + 1) {
            messageService.sendMessage(
                    sender,
                    "badge_wear_invalid_slot",
                    Map.of("max_slots", String.valueOf(wornBadges.size() + 1))
            );
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

        messageService.sendMessage(
                sender,
                "badge_wear_slot_updated",
                Map.of("slot", String.valueOf(slot))
        );

        return Command.SINGLE_SUCCESS;
    }

    private static int removeWearing(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        var messageService = Badges.getMessageService();

        if (!(sender instanceof Player player)) {
            messageService.sendMessage(sender, "badge_wear_only_players", Collections.emptyMap());
            return Command.SINGLE_SUCCESS;
        }

        var badgeStore = Badges.getBadgeStore();
        String badgeId = StringArgumentType.getString(ctx, "badge");
        if (!Badges.getRegisteredBadges().containsKey(badgeId)) {
            messageService.sendMessage(sender, "obj_not_found", Map.of("obj", badgeId));
            return Command.SINGLE_SUCCESS;
        }

        return Command.SINGLE_SUCCESS;
    }
}
