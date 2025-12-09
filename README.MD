# Bed Wars Center Lobby Core

Modern, high-performance lobby core plugin for Bed Wars Center built with Java 21 and Guice dependency injection.

## Features

### Core Functionality
- **Lobby Spawn** - Safe spawn handling with void, hunger, and fall damage protection
- **Hotbar Items** - Quick Play, Profile, BedWars Menu, Shop, Collectibles, Visibility Toggle, Lobby Selector
- **Parkour System** - Multi-checkpoint parkour with timing, leaderboards, and rewards
- **Player Visibility** - Toggle to show/hide other players
- **Cross-Lobby Sync** - Real-time synchronization via Redis + MongoDB

### Display Systems
- **Scoreboard** - Animated sidebar with PlaceholderAPI support
- **Tablist** - Custom tab list with sorting and formatting
- **Nametag** - Rank-based colored nametags with prefixes

### Architecture
- **Guice DI** - Full dependency injection for testability and maintainability
- **Service Pattern** - Clean service interfaces with lifecycle management
- **Async Operations** - Non-blocking database operations with CompletableFuture
- **NMS Integration** - Direct packet manipulation for optimal performance

## Requirements

| Requirement | Version |
|-------------|---------|
| Server | Spigot/Paper `1.8.8` |
| Java | `21` |
| Plugins | `PlaceholderAPI`, `Phoenix (pxAPI)` |
| Services | `Redis`, `MongoDB` |

## Installation

1. Place `BedWarsLobby.jar` in your `plugins` folder
2. Install required plugins: `PlaceholderAPI`, `Phoenix`
3. Start the server once to generate configuration files
4. Configure `settings.yml`:
   ```yaml
   REDIS:
     REDIS_HOST: "localhost"
     REDIS_PORT: 6379
     REDIS_PASSWORD: ""
     REDIS_DATABASE: 0
   
   MONGO:
     MONGO_URI: "mongodb://localhost:27017"
     MONGO_DATABASE: "bedwars"
   ```
5. Restart the server

## Configuration Files

| File | Purpose |
|------|---------|
| `settings.yml` | Core settings, database connections, lobby ID |
| `language.yml` | All player-facing messages |
| `sound.yml` | Sound effects configuration |
| `items.yml` | Hotbar item definitions |
| `scoreboard.yml` | Scoreboard layout and animation |
| `tablist.yml` | Tab list format and sorting |
| `nametag.yml` | Rank-based nametag colors |

## Commands

### Admin Commands (`/bwl`)
| Command | Permission | Description |
|---------|------------|-------------|
| `/bwl reload` | `bwl.admin` | Reload all configurations |
| `/bwl setspawn` | `bwl.admin` | Set lobby spawn point |
| `/bwl parkour create <name>` | `bwl.admin` | Create a new parkour |

### Player Commands
| Command | Aliases | Description |
|---------|---------|-------------|
| `/spawn` | `/stuck` | Teleport to spawn |
| `/parkour checkpoint` | `/pk cp` | Teleport to last checkpoint |
| `/parkour reset` | `/pk r` | Reset parkour progress |
| `/parkour quit` | `/pk q` | Leave current parkour |

### Sync Commands (`/sync`)
| Command | Description |
|---------|-------------|
| `/sync config` | Sync configuration to other lobbies |
| `/sync chunk` | Sync current chunk |
| `/sync area <radius>` | Sync area around player |
| `/sync world` | Sync world settings |
| `/sync parkour` | Sync all parkours |
| `/sync full` | Full lobby synchronization |

## Architecture

### Service Layer
All functionality is built on injectable services:

```java
// Services are injected via Guice
@Inject
public MyListener(IScoreboardService scoreboardService) {
    this.scoreboardService = scoreboardService;
}
```

### Available Services

| Interface | Description |
|-----------|-------------|
| `IConfigurationService` | Configuration loading and saving |
| `IRedisService` | Redis pub/sub and key-value |
| `IMongoService` | MongoDB document operations |
| `IScoreboardService` | Sidebar scoreboard management |
| `ITablistService` | Tab list management |
| `INametagService` | Player nametag management |
| `IParkourService` | Parkour system |
| `IHotbarService` | Hotbar item management |
| `IPlayerVisibilityService` | Player visibility toggling |
| `INMSService` | NMS availability checking |

### Data Flow

```
Player Join
    ↓
ListenerService (PlayerJoinListener)
    ↓
┌─────────────────────────────────────┐
│  HotbarService.giveLobbyHotbar()    │
│  ScoreboardService.createScoreboard()│
│  TablistService.createTablist()      │
│  NametagService.createNametag()      │
│  PlayerSyncService.handlePlayerJoin()│
└─────────────────────────────────────┘
```

## PlaceholderAPI Expansion

Register placeholders with prefix `bwlobby`:

| Placeholder | Description |
|-------------|-------------|
| `%bwlobby_player_level%` | Player's BedWars level |
| `%bwlobby_player_rank%` | Player's rank name |
| `%bwlobby_player_coins%` | Player's coin balance |
| `%bwlobby_lobby_players%` | Players in this lobby |
| `%bwlobby_lobby_id%` | Current lobby identifier |

## Building

```bash
# Windows
.\gradlew.bat clean shadowJar

# Linux/Mac
./gradlew clean shadowJar
```

Output: `build/libs/BedWarsLobby-v<version>.jar`

## Development

### Project Structure
```
src/main/java/center/bedwars/lobby/
├── command/           # Command handlers
├── configuration/     # Configuration classes
├── database/          # Redis & MongoDB services
├── dependency/        # External plugin integrations
├── hotbar/            # Hotbar item service
├── injection/         # Guice module and service manager
├── listener/          # Event listeners
├── nametag/           # Nametag service
├── nms/               # NMS utilities and netty
├── parkour/           # Parkour system
├── scoreboard/        # Scoreboard service
├── service/           # Base service interfaces
├── sync/              # Cross-lobby synchronization
├── tablist/           # Tab list service
├── util/              # Utility classes
└── visibility/        # Player visibility service
```

### Creating a New Service

1. Create interface in appropriate package:
   ```java
   public interface IMyService extends IService {
       void doSomething(Player player);
   }
   ```

2. Create implementation:
   ```java
   @Singleton
   public class MyService extends AbstractService implements IMyService {
       @Inject
       public MyService(Lobby plugin) { }
       
       @Override
       protected void onEnable() { }
       
       @Override
       protected void onDisable() { }
       
       @Override
       public void doSomething(Player player) { }
   }
   ```

3. Register in `LobbyModule`:
   ```java
   bind(IMyService.class).to(MyService.class).in(Singleton.class);
   ```

## Contributing

- Keep code simple and focused
- Follow existing package structure
- Use dependency injection over static access
- Add Javadoc to public methods
- Test locally before submitting

## License

See `LICENSE` file.
