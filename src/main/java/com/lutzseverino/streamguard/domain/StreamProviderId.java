package com.lutzseverino.streamguard.domain;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public record StreamProviderId(String value) {

    private static final Pattern VALID_ID = Pattern.compile("[a-z0-9][a-z0-9_-]{0,31}");

    public static final StreamProviderId TWITCH = new StreamProviderId("twitch");
    public static final StreamProviderId YOUTUBE = new StreamProviderId("youtube");
    public static final StreamProviderId MANUAL = new StreamProviderId("manual");

    public StreamProviderId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("provider id cannot be blank");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
        if (!VALID_ID.matcher(value).matches()) {
            throw new IllegalArgumentException("provider id must match " + VALID_ID.pattern());
        }
    }

    public static Optional<StreamProviderId> parse(String value) {
        try {
            return Optional.of(new StreamProviderId(value));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public String displayName() {
        return switch (value) {
            case "twitch" -> "Twitch";
            case "youtube" -> "YouTube";
            case "manual" -> "Manual";
            default -> titleCase(value.replace('_', ' ').replace('-', ' '));
        };
    }

    private static String titleCase(String input) {
        StringBuilder result = new StringBuilder(input.length());
        boolean nextUpper = true;
        for (char character : input.toCharArray()) {
            if (Character.isWhitespace(character)) {
                nextUpper = true;
                result.append(character);
            } else if (nextUpper) {
                result.append(Character.toUpperCase(character));
                nextUpper = false;
            } else {
                result.append(character);
            }
        }
        return result.toString();
    }
}
