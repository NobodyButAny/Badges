package com.batoni.badges;

import com.batoni.badges.command.Badge;
import com.batoni.badges.data.YmlBadgeStore;
import com.batoni.badges.format.MessageService;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("UnstableApiUsage")
public final class Badges extends JavaPlugin {

    private static final List<String> LOCALE_NAMES = List.of("en");

    private static Badges instance;

    private static LuckPerms luckPerms;
    private static YmlBadgeStore badgeStore;
    private static MessageService messageService;
    private static Map<String, String> registeredBadges = new ConcurrentHashMap<>();
    private static String defaultLocale;

    public static LuckPerms getLuckPerms() {
        return luckPerms;
    }

    public static YmlBadgeStore getBadgeStore() {
        return badgeStore;
    }

    public static Map<String, String> getRegisteredBadges() {
        return registeredBadges;
    }

    public static Badges getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        loadDependencies();
        loadConfig();
        loadDataStore();
        loadMessageService();
        registerCommands();
    }

    private void loadDependencies() {
        var provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider == null) {
            getLogger().severe("LuckPerms not found!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        luckPerms = provider.getProvider();
    }

    private void loadConfig() {
        saveDefaultConfig();
        var badgesSection = getConfig().getConfigurationSection("badges");
        if (badgesSection == null) {
            getLogger().warning("Is the config corrupted?");
            getLogger().warning("No 'badges' section found!");
            getLogger().warning("Loading 0 badges");
        } else {
            for (String key : badgesSection.getKeys(false)) {
                if (badgesSection.getString(key) == null) continue;
                registeredBadges.put(key, badgesSection.getString(key));
            }
        }

        defaultLocale = getConfig().getString("language");
        if (defaultLocale == null) {
            getLogger().warning("Is the config corrupted?");
            getLogger().warning("Plugin language not specified!");
            getLogger().warning("Will fallback to (en >> any present locale)");
        }
    }

    private void loadDataStore() {
        try {
            badgeStore = new YmlBadgeStore(getDataFolder(), "data.yml");
        } catch (IOException e) {
            getLogger().severe("Failed to initialize badge data.yml store!");
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    private void registerCommands() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(Badge.buildCommand());
        });
    }

    private void loadMessageService() {
        File dataFolder = getDataFolder();
        List<File> directories = Util.getDirectoryList(dataFolder);

        File localeFolder = new File(dataFolder, "locale");
        boolean hasLocaleFolder = directories.stream().anyMatch(f -> f.getName().equals("locale"));
        if (directories.isEmpty() || !hasLocaleFolder) {
            localeFolder.mkdirs();
        }

        List<File> localeFiles = Util.getFileList(localeFolder);
        if (localeFiles.isEmpty()) {
            loadDefaultLocales();
            localeFiles = Util.getFileList(localeFolder);
            if (localeFiles.isEmpty()) {
                getLogger().severe("Internal error! Loaded 0 default locales");
                Bukkit.getPluginManager().disablePlugin(this);
            }
        }

        try {
            messageService = MessageService.fromLocaleFolder(defaultLocale,
                    defaultLocale,
                    localeFolder,
                    buildFormatter());
        } catch (IOException e) {
            getLogger().severe("Internal error! MessageService load error!");
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    private void loadDefaultLocales() {
        for (String name : LOCALE_NAMES) {
            File defaultLocaleN = new File(getDataFolder(), "locale/%s.yml".formatted(name));
            try {
                Files.copy(
                        getResource("locale/%s.yml".formatted(name)),
                        defaultLocaleN.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                getLogger().severe("Failed to load default locale (%s)!".formatted(name));
                Bukkit.getPluginManager().disablePlugin(this);
            }
        }
    }

    private static MiniMessage buildFormatter() {
        return MiniMessage.miniMessage();
    }

    @Override
    public void onDisable() {
        if (badgeStore != null) {
            badgeStore.save();
        }
    }
}
