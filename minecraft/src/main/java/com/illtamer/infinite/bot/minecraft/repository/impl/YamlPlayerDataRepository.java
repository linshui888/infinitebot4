package com.illtamer.infinite.bot.minecraft.repository.impl;

import com.illtamer.infinite.bot.api.util.Assert;
import com.illtamer.infinite.bot.minecraft.Bootstrap;
import com.illtamer.infinite.bot.minecraft.configuration.config.ConfigFile;
import com.illtamer.infinite.bot.minecraft.pojo.PlayerData;
import com.illtamer.infinite.bot.minecraft.repository.PlayerDataRepository;
import com.illtamer.infinite.bot.minecraft.util.Lambda;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class YamlPlayerDataRepository implements PlayerDataRepository {

    private final Logger log = Bootstrap.getInstance().getLogger();
    private final ConfigFile dataFile;
    private final Map<String, PlayerData> playerDataMap;

    public YamlPlayerDataRepository(ConfigFile dataFile) {
        this.dataFile = dataFile;
        final FileConfiguration config = dataFile.getConfig();
        Set<String> idSet = config.getKeys(false);
        this.playerDataMap = idSet.stream().collect(Collectors.toMap(id -> id, id -> {
            PlayerData playerData = new PlayerData();
            final ConfigurationSection section = config.getConfigurationSection(id);
            if (section == null) return playerData;
            playerData.setUuid(section.getString("uuid", null));
            playerData.setUserId(section.getLong("user_id") != 0 ? section.getLong("user_id") : null);
            playerData.setPermission(section.getStringList("permission"));
            return playerData;
        }));
    }

    @Override
    public boolean save(@NotNull PlayerData data) {
        Map.Entry<String, PlayerData> dataEntry = getEntryByUUIDorUserId(data);
        if (dataEntry != null) {
            log.warning(String.format("PlayerData(%s) is already exists!", dataEntry.getValue()));
            return false;
        }
        String id;
        do {
            id = UUID.randomUUID().toString();
        } while (playerDataMap.containsKey(id));
        playerDataMap.put(id, data);
        return true;
    }

    @Override
    public PlayerData queryByUUID(@NotNull UUID uuid) {
        return queryByUUID(uuid.toString());
    }

    @Override
    public @Nullable PlayerData queryByUUID(@NotNull String uuid) {
        return Lambda.nullableInvoke(Map.Entry::getValue, doQueryByUUID(uuid));
    }

    @Override
    public @Nullable PlayerData queryByUserId(@NotNull Long userId) {
        return Lambda.nullableInvoke(Map.Entry::getValue, doQueryByUserId(userId));
    }

    @Override
    @Nullable
    public PlayerData update(@NotNull PlayerData data) {
        Map.Entry<String, PlayerData> dataEntry = getEntryByUUIDorUserId(data);
        if (dataEntry == null) {
            log.warning(String.format("PlayerData(%s) can not be found!", data));
            return null;
        }
        final String key = dataEntry.getKey();
        playerDataMap.put(key, data);
        return dataEntry.getValue();
    }

    @Override
    @Nullable
    public PlayerData delete(@NotNull String uuid) {
        final Map.Entry<String, PlayerData> dataEntry = doQueryByUUID(uuid);
        if (dataEntry == null) return null;
        return playerDataMap.remove(dataEntry.getKey());
    }

    @Override
    @Nullable
    public PlayerData delete(@NotNull Long userId) {
        final Map.Entry<String, PlayerData> dataEntry = doQueryByUserId(userId);
        if (dataEntry == null) return null;
        return playerDataMap.remove(dataEntry.getKey());
    }

    @Override
    public void saveCacheData() {
        final FileConfiguration config = dataFile.getConfig();
        playerDataMap.forEach((key, value) -> {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null)
                section = config.createSection(key, value.toMap());
            else {
                section.set("uuid", value.getUuid());
                section.set("user_id", value.getUserId());
                section.set("permission", value.getPermission());
            }
        });
        dataFile.save();
    }

    @Nullable
    protected Map.Entry<String, PlayerData> getEntryByUUIDorUserId(PlayerData data) {
        Map.Entry<String, PlayerData> dataEntry = null;
        final String uuid = data.getUuid();
        if (uuid != null) {
            dataEntry = doQueryByUUID(uuid);
        }
        if (dataEntry == null) {
            Long userId = data.getUserId();
            Assert.notNull(userId, "At least one of 'uuid' and 'userId' can not be null");
            dataEntry = doQueryByUserId(userId);
        }
        return dataEntry;
    }

    @Nullable
    private Map.Entry<String, PlayerData> doQueryByUUID(@NotNull String uuid) {
        Optional<Map.Entry<String, PlayerData>> first = playerDataMap.entrySet().stream()
                .filter(entry -> {
                    final PlayerData data = entry.getValue();
                    return data != null && data.getUuid().equals(uuid);
                }).findFirst();
        return first.orElse(null);
    }

    @Nullable
    private Map.Entry<String, PlayerData> doQueryByUserId(long userId) {
        Optional<Map.Entry<String, PlayerData>> first = playerDataMap.entrySet().stream()
                .filter(entry -> {
                    final PlayerData data = entry.getValue();
                    return data != null && data.getUserId().equals(userId);
                }).findFirst();
        return first.orElse(null);
    }

}