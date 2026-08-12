# Changelog

## 0.8.4-ALPHA - 2026-08-12

- Added animated intake sequences for every consumable.
- Added item- and category-specific smoke, snort, drink, eat, inhale, inject,
  ritual, and swallow profiles.
- Integrated the CatItems 0.3 public animation API with a standalone fallback.
- Delayed gameplay effects until the visible intake sequence finishes.

## 0.8.3-ALPHA - 2026-08-12

- Added central Small Caps rendering for messages and GUI text.
- Preserved real `ä`, `ö`, `ü` and `ß` characters without ASCII replacements.

## 0.8.2-ALPHA - 2026-08-12

- Made legacy configuration migration recursively add every missing leaf value.
- Preserved all existing administrator values during migration.
- Added regression coverage for nested YAML merges.

## 0.8.1-ALPHA - 2026-08-12

- Fixed migration of older nested server configuration files.
- Added missing functional items and recipes without overwriting custom values.

## 0.8.0-ALPHA - 2026-08-12

- Added persistent dependence, tolerance, withdrawal, and gradual recovery.
- Added mixed-category risk handling and perception events.
- Added a portable fantasy lab, dealer reputation, quests, and token rewards.
- Added supply events, contraband patrols, and antidote player rescue.
- Added public profile and quest commands plus administrative progression reset.

## 0.7.2-ALPHA - 2026-08-12

- Hardened English fallback messages for older server configurations.
- Added message regression and contract tests.

## 0.7.1-ALPHA - 2026-08-12

- Added configurable nausea, vomiting, and blackout thresholds.
- Added safe-location blackout recovery with a hard 150-block radius limit.

## 0.7.0-ALPHA - 2026-08-12

- Added onset, active, and comedown effect phases.
- Added configurable gameplay symptoms and category profiles for all 90 items.

## 0.6.3-ALPHA - 2026-08-12

- Enabled normal AI for naturally generated village pharmacists.
- Added migration for pharmacists created by 0.6.2.

## 0.6.2-ALPHA - 2026-08-12

- Added exactly one persistent pharmacist per generated Vanilla village.
- Added safe spawning when no eligible villager is loaded.

## 0.6.1-ALPHA - 2026-08-12

- Blocked Vanilla block interaction and placement for every CatDrugs item.

## 0.6.0-ALPHA - 2026-08-12

- Added configurable utensils and off-hand consumption requirements.
- Added preparation steps and utility items for survival gameplay.

## 0.5.0-ALPHA - 2026-08-12

- Expanded the catalog to 90 consumables.
- Added automatic fictional recipes and dealer pricing for enabled definitions.
- Added catalog pagination and persistent five-offer pharmacist inventories.

## 0.4.0-ALPHA - 2026-08-12

- Added 20 consumables, custom textures, and persistent custom NPC offers.

## 0.3.3-ALPHA - 2026-08-11

- Converted visible runtime text and CatItems definitions to English.
- Corrected generated resource-pack ZIP paths.

## 0.3.2-ALPHA - 2026-08-11

- Reworked CatDrugs assets into compact 3D-styled pixel art.

## 0.3.1-ALPHA - 2026-08-11

- Hardened right-click consumption across air, block, and off-hand events.

## 0.3.0-ALPHA - 2026-08-11

- Replaced ItemsAdder with optional CatItems integration.
- Added protected definition and asset installation plus verification commands.

## 0.2.0-ALPHA - 2026-08-11

- Expanded the initial catalog, loot, recipes, and pharmacist offers.

## 0.1.0-ALPHA - 2026-08-11

- Added the first consumables, effects, pharmacists, recipes, loot, and catalog.

---
Made By CatgirlYannick
