package com.lutzseverino.streamguard.platform.bukkit;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

public final class DurationParser {

    private DurationParser() {
    }

    public static Optional<Duration> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("permanent".equals(normalized) || "forever".equals(normalized) || "persist".equals(normalized)) {
            return Optional.of(Duration.ZERO);
        }
        int index = 0;
        while (index < normalized.length() && Character.isDigit(normalized.charAt(index))) {
            index++;
        }
        if (index == 0) {
            return Optional.empty();
        }
        long amount;
        try {
            amount = Long.parseLong(normalized.substring(0, index));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
        String unit = normalized.substring(index);
        return switch (unit) {
            case "s", "sec", "secs", "second", "seconds" -> Optional.of(Duration.ofSeconds(amount));
            case "m", "min", "mins", "minute", "minutes" -> Optional.of(Duration.ofMinutes(amount));
            case "h", "hr", "hrs", "hour", "hours" -> Optional.of(Duration.ofHours(amount));
            case "d", "day", "days" -> Optional.of(Duration.ofDays(amount));
            default -> Optional.empty();
        };
    }
}
