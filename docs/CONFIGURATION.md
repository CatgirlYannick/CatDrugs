# Configuration

| File | Purpose |
| --- | --- |
| `config.yml` | Global features, dose reactions, logging, and CatItems integration |
| `drugs.yml` | Items, effects, cooldowns, categories, and dose points |
| `survival.yml` | Dealers, loot, cultivation, fantasy recipes, and advanced gameplay |
| `gui.yml` | Catalog presentation |
| `messages.yml` | MiniMessage/RGB messages |
| `realistic-effects.yml` | Multi-phase profiles and custom gameplay symptoms |

## Intake Animations

`config.yml > consumption-animations` maps categories and individual drugs to
CatItems animation IDs and controls their duration. Durations are expressed in
ticks. CatDrugs accepts values from 8 through 120 ticks and delays item removal
and gameplay effects until the sequence is complete. The actual CatItems
keyframes are editable in `plugins/CatItems/animations.yml`.

## Multi-Phase Effects

`realistic-effects.yml` maps consumables to a default category profile. A
definition under `drug-overrides` can select a different profile for one item.
Each phase has a delay and lists of `potion-effects` and `custom-effects`.
`replace-legacy-effects: true` replaces the old immediate and after-effects from
`drugs.yml`; `false` runs both systems. `enabled: false` disables phased effects.

Custom effects are implemented by CatDrugs rather than Vanilla potion effects.
Available types include heartbeat, tremor, coordination, respiration, sedation,
camera drift, visual echo, auditory distortion, focus pulse, time distortion,
and muscle tension. PacketEvents is optional and used only for camera drift;
all remaining custom effects use Paper directly.

## Dose Reactions

`config.yml` contains separate thresholds for nausea, vomiting, and blackout.
Each event fires when the active dose first crosses its threshold. Blackout
duration, search radius, and candidate count are configurable. The plugin hard
limits the radius to 150 blocks for performance and safety.

`dose-reactions.vomiting` controls burst count and interval, particle density,
food/saturation/exhaustion loss, and horizontal movement retention. The visual
stream is a fictional gameplay effect and does not leave blocks or items behind.

`item.catitems-id` is the optional custom-item ID. When CatItems or that ID is
missing, CatDrugs uses `item.fallback-material`. Legacy `item.itemsadder-id`
entries are read only for migration.

## CatItems Add-on

The `catitems.addon` section provides:

- `auto-install`: install definitions and assets during startup
- `rebuild-after-install`: reload CatItems after changes
- `overwrite-customized-files`: intentionally replace administrator changes

The overwrite option defaults to `false`.

## Village Pharmacists

`village-dealers.natural-village-spawning.enabled` creates exactly one
persistent pharmacist per generated Vanilla village. The village structure
stores the marker, so chunk reloads cannot create duplicates. `ai-enabled`
controls normal villager AI for naturally generated pharmacists.

## Advanced Gameplay

Every module under `survival.yml > advanced-gameplay` can be disabled separately:

- `progression`: tolerance floor and warning level
- `withdrawal`: delay, check interval, and recovery
- `mixing`: category window and additional dose points
- `perception`: chance and duration of perception events
- `lab`: tool, duration, and fictional item transformations
- `dealer-quests`: target pool, trade amount, token, and reward pool
- `enforcement`: patrol chance, size, duration, and confiscation
- `random-events`: supply-find chance and item pool

Player progression is stored in the player's Persistent Data Container and
survives restarts. `/catdrugs reset <player>` removes only CatDrugs progression
and the active dose.

All recipes are fictional Minecraft combinations and not real instructions.

---
Made By CatgirlYannick
