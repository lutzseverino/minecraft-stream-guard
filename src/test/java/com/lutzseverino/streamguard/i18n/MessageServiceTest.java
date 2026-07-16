package com.lutzseverino.streamguard.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

final class MessageServiceTest {

  @Test
  void rendersPlaceholderValuesAsTextInsteadOfMiniMessageMarkup() {
    MessageService messages = new MessageService(Map.of(), "en_US", "en_US");

    String rendered =
        PlainTextComponentSerializer.plainText()
            .serialize(
                messages.renderTemplate(
                    "<green>Channel: {channel}</green>",
                    Map.of("channel", "<red>not formatting</red>")));

    assertEquals("Channel: <red>not formatting</red>", rendered);
  }
}
