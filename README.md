# CatDrugs

![CatDrugs logo](docs/assets/plugin-icon.png)

CatDrugs is a configurable, fictional survival-gameplay plugin for Paper 1.21
through 1.21.11. It provides 90 consumables, phased effects, persistent player
progression, fantasy acquisition systems, village pharmacists, quests, and
optional CatItems visuals.

All recipes and processing paths are deliberately fictional Minecraft systems.
They do not describe real-world production, dosing, or safety practices.

## Features

- 90 configurable consumables with staged onset, active, and comedown phases
- animated intake matched to smoking, snorting, drinking, eating, inhaling,
  injecting, rituals, and swallowing
- tolerance, dependence, withdrawal, recovery, and mixed-category risks
- configurable nausea, vomiting, blackout, and safe-location recovery events
- fantasy crafting, structure loot, cultivation drops, and portable lab actions
- persistent village pharmacists with normal villager AI and five saved offers
- dealer reputation, quests, tokens, patrol encounters, and player rescue
- MiniMessage/RGB names and messages rendered in Small Caps without replacing `ä`, `ö`, `ü` or `ß`
- optional CatItems definitions, textures, and pharmacist appearance
- complete Vanilla-item fallback when CatItems is not installed

## Requirements

- Java 21
- Paper 1.21 through 1.21.11
- No required plugin dependencies
- CatItems is optional and only supplies custom visuals and models

## Installation

1. Place `CatDrugs-0.8.4-ALPHA.jar` in the server's `plugins/` directory.
2. Optionally install CatItems before the first start.
3. Start Paper once and review the generated files in `plugins/CatDrugs/`.
4. When CatItems is installed, run `/catdrugs catitems verify`.
5. Use `/catdrugs status` to check the active configuration.

## Documentation

- [Getting started](START_HERE.md)
- [Features](docs/FEATURES.md)
- [Configuration](docs/CONFIGURATION.md)
- [Commands and permissions](docs/COMMANDS.md)
- [Optional CatItems setup](docs/CATITEMS_SETUP.md)
- [Download description](docs/DOWNLOAD_DESCRIPTION.md)
- [Changelog](CHANGELOG.md)

## Alpha Notice

This is an Alpha release. Back up server data, test configuration changes on a
staging server, and use a full Paper restart when replacing the JAR.

---
Made By CatgirlYannick
