package com.batoni.badges.format;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public record MessageLocale(
        String language,
        Map<String, String> rawStrings,
        Map<String, String> aliases
) {
    public static MessageLocale fromYamlConfiguration(File file) throws IOException {
        if (!file.exists()) {
            throw new IOException("Failed to create a locale from file %s!".formatted(file.getName()));
        }
        YamlConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(file);

        HashMap<String, String> rawStrings = new HashMap<>();
        var messagesSection = fileConfiguration.getConfigurationSection("messages");
        if (messagesSection == null) {
            throw new IOException("Malformed locale! %s doesn't contain messages section!".formatted(file.getName()));
        }
        for (String key : messagesSection.getKeys(false)) {
            String msg = messagesSection.getString(key);
            if (msg != null) rawStrings.put(key, msg);
        }

        HashMap<String, String> aliases = new HashMap<>();
        if (fileConfiguration.contains("aliases")) {
            var aliasesSection = fileConfiguration.getConfigurationSection("aliases");
            for (String key : aliasesSection.getKeys(false)) {
                String alias = aliasesSection.getString(key);
                if (alias != null) aliases.put(key, alias);
            }
        }

        return new MessageLocale(
                file.getName().replaceFirst("\\.yml$", ""),
                Collections.unmodifiableMap(rawStrings),
                Collections.unmodifiableMap(aliases)
        );
    }
}
