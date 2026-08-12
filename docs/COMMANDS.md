# Commands and Permissions

| Command | Permission | Purpose |
| --- | --- | --- |
| `/catdrugs` | `catdrugs.command` | Open the catalog |
| `/catdrugs list` | `catdrugs.command` | List substance IDs |
| `/catdrugs info <id>` | `catdrugs.command` | Show a short effect summary |
| `/catdrugs profile` | `catdrugs.command` | Show dependence, tolerance, and dealer reputation |
| `/catdrugs quest` | `catdrugs.command` | Show the active dealer quest |
| `/catdrugs give <player> <id> [amount]` | `catdrugs.admin.give` | Give a test item |
| `/catdrugs dealer spawn` | `catdrugs.admin.dealer` | Spawn a pharmacist |
| `/catdrugs dealer refresh` | `catdrugs.admin.dealer` | Convert the nearest eligible villager |
| `/catdrugs catitems install [force]` | `catdrugs.admin.catitems` | Install the optional CatItems add-on |
| `/catdrugs catitems verify` | `catdrugs.admin.catitems` | Verify all CatItems IDs |
| `/catdrugs catitems rebuild` | `catdrugs.admin.catitems` | Reload CatItems and rebuild its pack |
| `/catdrugs reload` | `catdrugs.admin.reload` | Reload supported configuration files |
| `/catdrugs status` | `catdrugs.admin.status` | Show diagnostics |
| `/catdrugs reset <player>` | `catdrugs.admin.reset` | Reset CatDrugs progression and the active dose |

Commands without permission are hidden from help and tab completion.

---
Made By CatgirlYannick
