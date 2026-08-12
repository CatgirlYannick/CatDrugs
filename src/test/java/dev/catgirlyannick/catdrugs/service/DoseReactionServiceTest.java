package dev.catgirlyannick.catdrugs.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DoseReactionServiceTest {
    @Test
    void reactionsOnlyFireWhenCrossingTheirThreshold() {
        assertEquals(DoseReactionService.Reaction.NONE,
                DoseReactionService.selectReaction(2, 5, 6, 10, 16));
        assertEquals(DoseReactionService.Reaction.NAUSEA,
                DoseReactionService.selectReaction(5, 7, 6, 10, 16));
        assertEquals(DoseReactionService.Reaction.VOMITING,
                DoseReactionService.selectReaction(8, 12, 6, 10, 16));
        assertEquals(DoseReactionService.Reaction.BLACKOUT,
                DoseReactionService.selectReaction(13, 18, 6, 10, 16));
        assertEquals(DoseReactionService.Reaction.NONE,
                DoseReactionService.selectReaction(16, 20, 6, 10, 16));
    }

    @Test
    void highestCrossedReactionWinsWhenPointsJump() {
        assertEquals(DoseReactionService.Reaction.BLACKOUT,
                DoseReactionService.selectReaction(5, 18, 6, 10, 16));
    }
}
