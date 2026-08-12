# Features

## Consumables and Effects

Ninety consumable definitions use configurable multi-phase profiles with onset,
active, and comedown stages. Alongside Vanilla potion effects, CatDrugs includes
custom symptoms such as heartbeat, tremors, coordination loss, dehydration,
respiratory depression, perception shifts, sedation, fatigue, appetite,
overheating, and dizziness. Repeated consumption inside the dose window can
trigger negative reactions and damage.

The profiles are gameplay abstractions, not medical simulations or dosing and
safety advice.

## Acute Dose Reactions

Increasing dose points can escalate from nausea to vomiting and finally a
blackout. A blackout blinds, slows, and weakens the player. CatDrugs then checks
up to the configured number of candidates within a hard maximum of 150 blocks.
It teleports only when the ground, headroom, world border, and surroundings are
safe. Otherwise the player wakes at the original location.

## Survival Acquisition

- Each generated Vanilla village receives one persistent village pharmacist
  with normal villager AI and a structure marker that prevents duplicates.
- Eligible nitwit villagers outside recognized village structures can still be
  converted through the configurable fallback.
- Loot tables add rare finds to suitable Vanilla structures.
- Grass and crop interactions provide fantasy cultivation drops.
- Shapeless recipes combine deliberately fictional Minecraft ingredients.

## Persistent Gameplay Systems

- Dependence and tolerance persist per player; tolerance can shorten positive
  effect durations to a configurable minimum.
- Withdrawal can begin after prolonged abstinence, followed by gradual recovery.
- Mixing categories adds dose points, with stronger configurable reactions for
  selected combinations.
- Psychedelic, dissociative, and synthetic categories can trigger perception
  events using particles, darkness, and positional sounds.
- A portable alchemy lab performs selected fictional Minecraft transformations.
- Village pharmacists provide reputation, trade quests, tokens, and rewards.
- Random supply finds and configurable contraband patrols add world events.
- A sneaking player can use an antidote on another player to stabilize the
  active dose and acute negative effects.

## Optional Visuals and Fallback

The CatItems add-on provides original item textures, models, and a pharmacist
appearance. Every CatDrugs item also carries its own PDC identity, so all
gameplay remains functional with configured Vanilla materials.

---
Made By CatgirlYannick
