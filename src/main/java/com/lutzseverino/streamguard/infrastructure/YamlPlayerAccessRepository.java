package com.lutzseverino.streamguard.infrastructure;

import com.lutzseverino.streamguard.application.PlayerAccessRepository;
import com.lutzseverino.streamguard.domain.BypassGrant;
import com.lutzseverino.streamguard.domain.PlayerAccessRecord;
import com.lutzseverino.streamguard.domain.StreamLink;
import com.lutzseverino.streamguard.domain.StreamProviderId;
import com.lutzseverino.streamguard.domain.VerificationStatus;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class YamlPlayerAccessRepository implements PlayerAccessRepository {

    private final File file;
    private final Logger logger;
    private YamlConfiguration yaml;

    public YamlPlayerAccessRepository(File file, Logger logger) {
        this.file = file;
        this.logger = logger;
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public synchronized PlayerAccessRecord getOrCreate(UUID playerId, String playerName) {
        return find(playerId).orElseGet(() -> PlayerAccessRecord.empty(playerId, playerName));
    }

    @Override
    public synchronized Optional<PlayerAccessRecord> find(UUID playerId) {
        ConfigurationSection section = yaml.getConfigurationSection("players." + playerId);
        if (section == null) {
            return Optional.empty();
        }
        String name = section.getString("name", "");
        StreamLink link = readLink(section.getConfigurationSection("stream.link"));
        VerificationStatus status = readVerification(section.getConfigurationSection("stream.status"));
        BypassGrant grant = readBypass(playerId, section.getConfigurationSection("bypass"));
        return Optional.of(new PlayerAccessRecord(playerId, name, link, status, grant));
    }

    @Override
    public synchronized void save(PlayerAccessRecord accessRecord) {
        String root = "players." + accessRecord.playerId();
        yaml.set(root + ".name", accessRecord.playerName());
        accessRecord.streamLinkOptional().ifPresentOrElse(link -> {
            yaml.set(root + ".stream.link.platform", link.providerId().value());
            yaml.set(root + ".stream.link.channel", link.channel());
        }, () -> yaml.set(root + ".stream.link", null));
        accessRecord.verificationStatusOptional().ifPresentOrElse(status -> {
            yaml.set(root + ".stream.status.live", status.live());
            yaml.set(root + ".stream.status.platform", status.verifiedProviderId().map(StreamProviderId::value).orElse(null));
            yaml.set(root + ".stream.status.checked-at", status.checkedAt().toEpochMilli());
            yaml.set(root + ".stream.status.detail", status.detail());
        }, () -> yaml.set(root + ".stream.status", null));
        accessRecord.bypassGrantOptional().ifPresentOrElse(grant -> {
            yaml.set(root + ".bypass.granted-by", grant.grantedBy() == null ? null : grant.grantedBy().toString());
            yaml.set(root + ".bypass.granted-at", grant.grantedAt().toEpochMilli());
            yaml.set(root + ".bypass.expires-at", grant.expiresAtOptional().map(Instant::toEpochMilli).orElse(null));
            yaml.set(root + ".bypass.reason", grant.reason());
        }, () -> yaml.set(root + ".bypass", null));
        persist();
    }

    public synchronized void reload() {
        yaml = YamlConfiguration.loadConfiguration(file);
    }

    private StreamLink readLink(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        Optional<StreamProviderId> providerId = StreamProviderId.parse(section.getString("platform"));
        String channel = section.getString("channel", "");
        if (providerId.isEmpty() || channel.isBlank()) {
            return null;
        }
        return new StreamLink(providerId.get(), channel);
    }

    private VerificationStatus readVerification(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        Instant checkedAt = Instant.ofEpochMilli(section.getLong("checked-at", 0L));
        String detail = section.getString("detail", "");
        if (!section.getBoolean("live", false)) {
            return VerificationStatus.unverified(checkedAt, detail);
        }
        Optional<StreamProviderId> providerId = StreamProviderId.parse(section.getString("platform"));
        return providerId
                .map(value -> VerificationStatus.live(value, checkedAt, detail))
                .orElseGet(() -> VerificationStatus.unverified(checkedAt, detail));
    }

    private BypassGrant readBypass(UUID playerId, ConfigurationSection section) {
        if (section == null || !section.isLong("granted-at")) {
            return null;
        }
        UUID grantedBy = null;
        String grantedByRaw = section.getString("granted-by");
        if (grantedByRaw != null && !grantedByRaw.isBlank()) {
            try {
                grantedBy = UUID.fromString(grantedByRaw);
            } catch (IllegalArgumentException ignored) {
                // Keep corrupt legacy sender IDs from breaking all player data reads.
            }
        }
        Instant expiresAt = section.isLong("expires-at")
                ? Instant.ofEpochMilli(section.getLong("expires-at"))
                : null;
        return new BypassGrant(
                playerId,
                grantedBy,
                Instant.ofEpochMilli(section.getLong("granted-at")),
                expiresAt,
                section.getString("reason", "")
        );
    }

    private void persist() {
        try {
            yaml.save(file);
        } catch (IOException exception) {
            logger.log(Level.SEVERE, "Could not save StreamGuard player data.", exception);
        }
    }
}
