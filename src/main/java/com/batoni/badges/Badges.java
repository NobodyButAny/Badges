package com.batoni.badges;

import com.batoni.badges.command.Badge;
import com.batoni.badges.data.YmlBadgeStore;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public final class Badges extends JavaPlugin {

    private static Badges instance;

    private static LuckPerms luckPerms;
    private static YmlBadgeStore badgeStore;
    private static Map<String, String> registeredBadges = new HashMap<>();

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
        registerCommands();
    }

    private void loadDependencies() {
        var provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider == null) {
            getLogger().severe("[Badges] LuckPerms not found!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        luckPerms = provider.getProvider();
    }

    private void loadConfig() {
        saveDefaultConfig();
        var badgesSection = getConfig().getConfigurationSection("badges");
        if (badgesSection == null) {
            getLogger().warning("[Badges] Is the config corrupted?");
            getLogger().warning("[Badges] No 'badges' section found!");
            getLogger().warning("[Badges] Loading 0 badges");
        } else {
            for (String key : badgesSection.getKeys(false)) {
                if (badgesSection.getString(key) == null) continue;
                registeredBadges.put(key, badgesSection.getString(key));
            }
        }
    }

    private void loadDataStore() {
        try {
            badgeStore = new YmlBadgeStore(getDataFolder(), "data.yml");
        } catch (IOException e) {
            getLogger().severe("[Badges] Failed to initialize badge data.yml store!");
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    private void registerCommands() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(Badge.buildCommand());
        });
    }

    @Override
    public void onDisable() {
        badgeStore.save();
    }
}
