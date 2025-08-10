package com.batoni.badges.data;

import com.batoni.badges.Badges;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class YmlBadgeStore {
    private File file;
    private FileConfiguration fileConfiguration;

    public YmlBadgeStore(File dataFolder, String storeName) throws IOException {
        this.file = new File(dataFolder, storeName);
        if (!file.exists()) {
            if (!dataFolder.mkdirs() && !dataFolder.exists()) {
                throw new IOException("Failed to create path to " + storeName);
            }
            file.createNewFile();
        }

        this.fileConfiguration = YamlConfiguration.loadConfiguration(file);
    }

    public boolean save() {
        try {
            fileConfiguration.save(file);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void tryUpdatePlayerName(UUID uuid) {
        Player player = Badges.getInstance().getServer().getPlayer(uuid);
        if (player != null) {
            fileConfiguration.set(uuid + ".player", player.getName());
        }
    }

    public List<UUID> getRegisteredIds() {
        return fileConfiguration.getKeys(false)
                .stream()
                .map(UUID::fromString)
                .toList();
    }

    public boolean hasBadge(UUID uuid, String badgeId) {
        return getOwnedBadges(uuid).contains(badgeId);
    }

    public List<String> getOwnedBadges(UUID uuid) {
        return fileConfiguration.getStringList(uuid + ".owned");
    }

    public boolean addOwnedBadges(UUID uuid, Collection<String> badgeIds) {
        List<String> ownedBadges = getOwnedBadges(uuid);
        ownedBadges.addAll(badgeIds);
        fileConfiguration.set(uuid + ".owned", ownedBadges);

        tryUpdatePlayerName(uuid);
        return save();
    }

    public boolean addOwnedBadges(UUID uuid, String... badgeIds) {
        return addOwnedBadges(uuid, Arrays.asList(badgeIds));
    }

    public boolean setOwnedBadges(UUID uuid, Collection<String> badgeIds) {
        fileConfiguration.set(uuid + ".owned", badgeIds);
        tryUpdatePlayerName(uuid);
        return save();
    }


    public boolean removeOwnedBadges(UUID uuid, Collection<String> badgeIds) {
        List<String> ownedBadges = getOwnedBadges(uuid);
        ownedBadges.removeAll(badgeIds);
        fileConfiguration.set(uuid + ".owned", ownedBadges);
        tryUpdatePlayerName(uuid);
        return save();
    }

    public boolean removeOwnedBadges(UUID uuid, String... badgeIds) {
        return removeOwnedBadges(uuid, Arrays.asList(badgeIds));
    }

    public List<String> getWearingBadges(UUID uuid) {
        return fileConfiguration.getStringList(uuid + ".wearing");
    }

    public boolean setWearingBadges(UUID uuid, List<String> badgeIds) {
        fileConfiguration.set(uuid + ".wearing", badgeIds);
        tryUpdatePlayerName(uuid);
        return save();
    }

    public boolean removeWearingBadges(UUID uuid, List<String> badgeIds) {
        List<String> wearingBadges = getWearingBadges(uuid);
        wearingBadges.removeAll(badgeIds);
        fileConfiguration.set(uuid+".wearing", wearingBadges);
        tryUpdatePlayerName(uuid);
        return save();
    }

    public boolean removeWearingBadges(UUID uuid, String... badgeIds) {
        return removeWearingBadges(uuid, Arrays.asList(badgeIds));
    }
}
