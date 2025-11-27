# Bed Wars Center Lobby Core

Lobby core plugin for Bed Wars Center. Simple, usable, and open to additions.

## What It Does
- Lobby spawn and basic player safety (void, hunger, fall damage controls)
- Hotbar items: Quick Play, Profile, BedWars Menu, Shop, Collectibles, Visibility, Lobby Selector
- Parkour: checkpoint, reset, quit
- Player visibility toggle
- Sync tools: config/world/chunk/area/parkour/full via Redis + Mongo
- PlaceholderAPI expansion: `%bwlobby_player_*%`

## Requirements
- Server: Spigot/Paper `1.8.8`
- Java: `21`
- Plugins: `PlaceholderAPI`, `Phoenix (pxAPI)`
- Services: `Redis` + `MongoDB` for sync

## Install
1. Put `BedWarsLobby` jar in `plugins`.
2. Install `PlaceholderAPI` and `Phoenix`.
3. Start the server once; edit the generated `settings.yml`, `language.yml`, `sound.yml`, `items.yml`.
4. Fill `REDIS` and `MONGO` sections in `settings.yml` for sync.

## Commands
- `/bwl` (admin): `reload`, `parkour`, `setspawn`
- `/parkour`: `checkpoint`, `reset`, `quit`
- `/spawn` (`/stuck`)
- `/sync`: `config`, `chunk`, `area <radius>`, `world`, `parkour`, `full`

## Build
```bat
.\gradlew.bat clean shadowJar
```
Output: `build\libs\BedWarsLobby-v<version>.jar`

## Contribute
- Keep it simple and useful; small, focused PRs are best
- Ideas welcome: new hotbar items, parkour QoL, sync handlers (world/chunk/NPC/hologram), commands
- Compatibility: target Spigot/Paper 1.8.8 and Java 21
- Style: follow existing package layout and utility usage (`ColorUtil`, managers, configs)
- Test: run locally, verify commands, spawn, parkour, and sync events
- Open issues for bugs or feature proposals

## License
See `LICENSE`.
