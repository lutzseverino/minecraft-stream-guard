package com.lutzseverino.streamguard.i18n;

import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;

public final class MessageService {

  private final MiniMessage miniMessage = MiniMessage.miniMessage();
  private final LegacyComponentSerializer legacyText = LegacyComponentSerializer.legacySection();
  private final PlainTextComponentSerializer plainText = PlainTextComponentSerializer.plainText();
  private final Map<String, YamlConfiguration> bundles;
  private final String defaultLocale;
  private final String fallbackLocale;

  public MessageService(File langDirectory, String defaultLocale, String fallbackLocale) {
    this(
        Map.of(
            "en_US", YamlConfiguration.loadConfiguration(new File(langDirectory, "en_US.yml")),
            "es_ES", YamlConfiguration.loadConfiguration(new File(langDirectory, "es_ES.yml"))),
        defaultLocale,
        fallbackLocale);
  }

  MessageService(
      Map<String, YamlConfiguration> bundles, String defaultLocale, String fallbackLocale) {
    this.defaultLocale = normalize(defaultLocale);
    this.fallbackLocale = normalize(fallbackLocale);
    this.bundles = Map.copyOf(bundles);
  }

  public Component render(String locale, String key, Map<String, String> placeholders) {
    String template = lookup(normalize(locale), key);
    for (Map.Entry<String, String> entry : placeholders.entrySet()) {
      template = replacePlaceholder(template, entry);
    }
    return miniMessage.deserialize(template);
  }

  public Component renderDefault(String key, Map<String, String> placeholders) {
    return render(defaultLocale, key, placeholders);
  }

  public String renderPlainDefault(String key, Map<String, String> placeholders) {
    return plainText.serialize(renderDefault(key, placeholders));
  }

  public String renderLegacyDefault(String key, Map<String, String> placeholders) {
    return legacyText.serialize(renderDefault(key, placeholders));
  }

  public String renderLegacyTemplate(String template, Map<String, String> placeholders) {
    return legacyText.serialize(renderTemplate(template, placeholders));
  }

  public Component renderTemplate(String template, Map<String, String> placeholders) {
    String rendered = template == null ? "" : template;
    for (Map.Entry<String, String> entry : placeholders.entrySet()) {
      rendered = replacePlaceholder(rendered, entry);
    }
    return miniMessage.deserialize(rendered);
  }

  private String lookup(String locale, String key) {
    YamlConfiguration bundle = bundles.get(locale);
    if (bundle != null && bundle.isString(key)) {
      return bundle.getString(key, key);
    }
    YamlConfiguration fallback = bundles.get(fallbackLocale);
    if (fallback != null && fallback.isString(key)) {
      return fallback.getString(key, key);
    }
    YamlConfiguration english = bundles.get("en_US");
    if (english != null && english.isString(key)) {
      return english.getString(key, key);
    }
    return "<red>" + key + "</red>";
  }

  private String replacePlaceholder(String template, Map.Entry<String, String> placeholder) {
    String key = Objects.requireNonNull(placeholder.getKey(), "placeholder key");
    String value = Objects.requireNonNull(placeholder.getValue(), "placeholder value");
    return template.replace("{" + key + "}", miniMessage.escapeTags(value));
  }

  private static String normalize(String locale) {
    if (locale == null || locale.isBlank()) {
      return Locale.US.toString();
    }
    return locale.replace('-', '_');
  }
}
