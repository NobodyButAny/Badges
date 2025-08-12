package com.batoni.badges.format;

import com.batoni.badges.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MessageService {
    private final MiniMessage MM = MiniMessage.miniMessage();

    private String currentLocale;
    private final String fallbackLocale;
    private final Map<String, MessageLocale> locales;
    private final Map<String, MiniMessage> formatters;

    public String getCurrentLocale() {
        return this.currentLocale;
    }

    public void setCurrentLocale(String currentLocale) {
        if (!locales.keySet().contains(currentLocale)) {
            throw new IllegalArgumentException("Locale %s doesn't exist".formatted(currentLocale));
        }
        this.currentLocale = currentLocale;
    }

    public MessageService(String defaultLocale, String currentLocale, Collection<MessageLocale> locales) {
        if (locales.isEmpty()) {
            throw new IllegalArgumentException("No locales provided!");
        }

        this.locales = new ConcurrentHashMap<>();
        for (MessageLocale locale : locales) {
            this.locales.put(locale.language(), locale);
        }

        if (!this.locales.containsKey(defaultLocale)) {
            this.fallbackLocale = this.locales.containsKey("en")
                    ? "en"
                    : this.locales.keySet().stream().findFirst().get();
        } else {
            this.fallbackLocale = defaultLocale;
        }

        this.currentLocale = (currentLocale == null || currentLocale.isEmpty() || !this.locales.containsKey(currentLocale))
                ? this.fallbackLocale
                : currentLocale;

        this.formatters = new ConcurrentHashMap<>();
        this.locales.values().forEach(locale -> {
            var tagResolverBuilder = TagResolver.builder();
            locale.aliases().forEach((k, v) -> {
                tagResolverBuilder.tag(k, Tag.inserting(MM.deserialize(v)));
            });
            var formatter = MiniMessage.builder().tags(tagResolverBuilder.build()).build();
            this.formatters.put(locale.language(), formatter);
        });
    }

    public static MessageService fromLocaleFolder(String defaultLocale,
                                                  String currentLocale,
                                                  File localeFolder,
                                                  MiniMessage formatter) throws IOException {
        if (!localeFolder.exists() || !localeFolder.isDirectory()) {
            throw new IllegalArgumentException("No locale folder provided!");
        }

        File[] localeFiles = localeFolder.listFiles();
        if (localeFiles == null || localeFiles.length == 0) {
            throw new IllegalArgumentException("Locale folder is empty!");
        }

        List<MessageLocale> locales = new ArrayList<>();
        for (File localeFile : localeFiles) {
            var newLocale = MessageLocale.fromYamlConfiguration(localeFile);
            locales.add(newLocale);
        }

        return new MessageService(defaultLocale, currentLocale, locales);
    }

    public Component getFormatted(String messageId, String localeId, Map<String, String> placeholders) {
        if (localeId == null) localeId = this.currentLocale;
        var locale = this.locales.get(localeId).rawStrings().containsKey(messageId)
                ? this.locales.get(localeId)
                : this.locales.get(fallbackLocale);
        var formatter = this.formatters.get(locale.language());
        var rawMessage = locale.rawStrings().get(messageId);

        var placeholderResolver = TagResolver.resolver(
                placeholders.entrySet()
                        .stream()
                        .map(Util::insertingTextResolver)
                        .toArray(TagResolver[]::new));

        return formatter.deserialize(rawMessage, placeholderResolver);
    }

    public Component getFormatted(String messageId, Map<String, String> placeholders) {
        return this.getFormatted(messageId, null, placeholders);
    }

    public void sendMessage(CommandSender sender, String messageId, Map<String, String> placeholders) {
        sender.sendMessage(this.getFormatted(messageId, placeholders));
    }
}
