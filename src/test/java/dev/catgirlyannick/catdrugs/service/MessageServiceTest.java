package dev.catgirlyannick.catdrugs.service;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageServiceTest {

    @Test
    void missingLegacyMessageUsesBundledEnglishDefault() {
        YamlConfiguration legacyMessages = new YamlConfiguration();
        legacyMessages.set("config-version", 1);
        MessageService service = new MessageService(legacyMessages);

        assertEquals("<yellow>{drug}: the onset begins.</yellow>",
                service.resolve("consumption.phases.onset", "missing"));
        assertEquals("<gold>{drug}: the main effects are now strongest.</gold>",
                service.resolve("consumption.phases.peak", "missing"));
        assertEquals("<dark_aqua>{drug}: the effects are wearing off.</dark_aqua>",
                service.resolve("consumption.phases.comedown", "missing"));
    }

    @Test
    void administratorMessageStillOverridesBundledDefault() {
        YamlConfiguration customizedMessages = new YamlConfiguration();
        customizedMessages.set("consumption.phases.peak", "<green>Custom peak</green>");
        MessageService service = new MessageService(customizedMessages);

        assertEquals("<green>Custom peak</green>",
                service.resolve("consumption.phases.peak", "missing"));
    }

    @Test
    void unknownPathUsesEnglishFinalFallback() {
        MessageService service = new MessageService(new YamlConfiguration());

        assertEquals("final fallback", service.resolve("unknown.path", "final fallback"));
    }
}
