package com.batoni.badges;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.*;
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

    public static Player getPlayerArgument(CommandContext<CommandSourceStack> ctx, String name) {
        var server = Badges.getInstance().getServer();

        String playerName = StringArgumentType.getString(ctx, name);
        return server.getPlayerExact(playerName);
    }

    public static TagResolver insertingTextResolver(String name, String value) {
        return TagResolver.resolver(name, Tag.inserting(Component.text(value)));
    }

    public static TagResolver insertingTextResolver(Map.Entry<String, String> entry) {
        return insertingTextResolver(entry.getKey(), entry.getValue());
    }

    public static List<File> getDirectoryList(File file) {
        return Optional
                .ofNullable(file.listFiles(File::isDirectory))
                .map(Arrays::asList)
                .orElse(Collections.emptyList());
    }

    public static List<File> getFileList(File file) {
        return Optional
                .ofNullable(file.listFiles(File::isFile))
                .map(Arrays::asList)
                .orElse(Collections.emptyList());
    }

}
