# CatItems Integration

CatItems is a separate, optional plugin. CatDrugs starts and remains fully
playable when CatItems is not installed.

## Automatic Installation

On the first shared start, CatDrugs creates `plugins/CatItems/items/catdrugs.yml`
and copies its item and pharmacist assets into
`plugins/CatItems/pack/assets/`. CatItems then reloads its registry and rebuilds
the shared resource pack.

Customized administrator files are protected by default. Available commands:

- `/catdrugs catitems install`
- `/catdrugs catitems verify`
- `/catdrugs catitems rebuild`
- `/catdrugs catitems install force` - intentionally overwrite customizations

Resource-pack delivery is configured only in CatItems. CatDrugs does not host a
second pack.

## Without CatItems

CatDrugs creates PDC-marked Vanilla items from each `fallback-material`.
Consumption, effects, recipes, loot, and dealers remain active.

---
Made By CatgirlYannick
