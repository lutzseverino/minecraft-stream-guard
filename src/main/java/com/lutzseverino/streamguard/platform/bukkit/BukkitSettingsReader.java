package com.lutzseverino.streamguard.platform.bukkit;

import com.lutzseverino.streamguard.config.SettingsReader;
import java.util.List;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public final class BukkitSettingsReader implements SettingsReader {

    private final FileConfiguration configuration;

    public BukkitSettingsReader(FileConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public String string(String path, String fallback) {
        return configuration.getString(path, fallback);
    }

    @Override
    public boolean bool(String path, boolean fallback) {
        return configuration.getBoolean(path, fallback);
    }

    @Override
    public int integer(String path, int fallback) {
        return configuration.getInt(path, fallback);
    }

    @Override
    public List<String> stringList(String path) {
        return configuration.getStringList(path);
    }

    @Override
    public Set<String> keys(String path) {
        ConfigurationSection section = configuration.getConfigurationSection(path);
        return section == null ? Set.of() : section.getKeys(false);
    }
}
