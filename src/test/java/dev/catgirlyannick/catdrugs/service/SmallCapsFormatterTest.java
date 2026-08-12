package dev.catgirlyannick.catdrugs.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SmallCapsFormatterTest {
    @Test
    void preservesFormattingPlaceholdersAndGermanCharacters() {
        assertEquals("<aqua>ᴊäɢᴇʀ ꜰüʀ ɢʀößᴇ</aqua> {player}",
                SmallCapsFormatter.formatTemplate("<aqua>Jäger für Größe</aqua> {player}"));
    }
}
