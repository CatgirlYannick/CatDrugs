package dev.catgirlyannick.catdrugs.model;

import java.util.List;

public record EffectProfile(String id, List<EffectPhaseDefinition> phases) {
}
