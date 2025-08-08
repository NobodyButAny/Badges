package com.batoni.badges;

import io.papermc.paper.command.brigadier.CommandSourceStack;

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
}
