# PortalWarp

<p align="center">
  <strong>Portal warp plugin for Paper / Folia</strong><br>
  Build linked portal frames in your world and create cross-server transfers with a single command.
</p>

<p align="center">
  <a href="README_RU.md">Русский</a> ·
  <a href="#installation">Installation</a> ·
  <a href="#commands">Commands</a> ·
  <a href="#configuration">Config</a> ·
  <a href="CHANGELOG.md">Changelog</a> ·
  <a href="LICENSE">MIT License</a>
</p>

---

| | |
|---|---|
| **Version** | `1.0.0` |
| **Author** | **Goblin_Thug** |
| **Platform** | Paper / Folia (API `26.2`) |
| **Java** | 25+ |
| **Build** | Maven |
| **License** | MIT |

---

## Description

**PortalWarp** is a plugin for **Paper** and **Folia** servers that lets you build block frames and turn them into portals.

Two modes are supported:

1. **Local warp** — two frames in the same or different worlds. Entering one teleports you to the other and vice versa.
2. **Cross-server portal** — a single frame. Entering it sends the player to the configured server address (`ip:port` or hostname).

Frames are selected with a **recovery compass** (`RECOVERY_COMPASS`). The plugin analyzes the outline, fills the interior, and starts particle effects.

---

## Features

- Named two-way local portals
- Admin-only cross-server portals
- Automatic frame detection on **XY / XZ / YZ** planes
- Interior fill (default: `CYAN_STAINED_GLASS_PANE`)
- Configurable portal particles (color, size, interval)
- Post-teleport cooldown
- Interior size limit
- Portal persistence in `config.yml`
- Brigadier commands (name suggestions on delete)
- Full **Folia** support (`folia-supported: true`)
- Separate permissions for players and admins

---

## Requirements

| Component | Requirement |
|-----------|-------------|
| Server | Paper or Folia with API **26.2** |
| Java | **25** or newer |
| Build (developers) | Maven 3.9+ |
| Creation item | Recovery Compass |

> The plugin depends on Folia API (`dev.folia:folia-api`) with `provided` scope — no extra dependency jars are required on the server.

---

## Installation

1. Build the jar (see [Building](#building)) or download the `PortalWarp-1.0.0.jar` release.
2. Place the file into the server `plugins/` folder.
3. Restart the server (or load via a compatible plugin manager).
4. Edit `plugins/PortalWarp/config.yml` if needed.
5. Run `/portal reload` (requires `portalwarp.admin`).

After the first start:

```text
plugins/
└── PortalWarp/
    └── config.yml
```

---

## Quick start

### Local warp portal

1. Build **two** closed frames (e.g. obsidian) with an empty interior.
2. Hold a **recovery compass** in your main hand.
3. Run:
   ```text
   /portal warp create my_portal
   ```
4. Right-click the **first** frame with the compass (exit / origin side).
5. Right-click the **second** frame (destination side).
6. Done: walk into the filled interior to teleport to the other side.

### Cross-server portal

1. Build **one** frame.
2. Hold a recovery compass.
3. Run (requires `portalwarp.admin`):
   ```text
   /portal connect create hub "play.example.com:25565"
   ```
4. Right-click the frame with the compass.
5. Entering the portal transfers the player to the given address.

---

## Commands

Root command: `/portal`  
No aliases are registered by the plugin (you may add server-side aliases if desired).

| Command | Description | Permission |
|---------|-------------|------------|
| `/portal` | Command help | `portalwarp.use` |
| `/portal warp create <name>` | Create a local portal (2 frames) | `portalwarp.use` |
| `/portal connect create <name> <address>` | Create a cross-server portal (1 frame) | `portalwarp.admin` |
| `/portal delete <name>` | Delete a portal | `portalwarp.use` |
| `/portal list` | List all portals | `portalwarp.use` |
| `/portal reload` | Reload config and portals | `portalwarp.admin` |

### Examples

```text
/portal warp create spawn_to_shop
/portal connect create to_survival "survival.myserver.net:25565"
/portal delete spawn_to_shop
/portal list
/portal reload
```

> For addresses with a colon (`ip:port`), quote the argument.

---

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `portalwarp.use` | `true` (everyone) | Basic commands: warp creation, list, delete, help |
| `portalwarp.admin` | `op` | Cross-server portals and `/portal reload` |

LuckPerms example:

```text
/lp user <name> permission set portalwarp.admin true
```

---

## Configuration

File: `plugins/PortalWarp/config.yml`

```yaml
# Cooldown after teleport in ticks (20 ticks = 1 second)
teleport-cooldown: 20

# Maximum portal interior size (blocks)
max-interior-size: 100

# Portal particle settings
particles:
  interval: 3
  count: 6
  spread: 0.4
  color:
    red: 60
    green: 140
    blue: 255
  size: 1.5

# Fill material and ambient effects
portal:
  fill-material: CYAN_STAINED_GLASS_PANE
  ambient-particle: ENCHANT
  ambient-count: 2

# Portals (managed automatically — do not edit by hand)
portals: []
```

### Options

| Key | Type | Description |
|-----|------|-------------|
| `teleport-cooldown` | int | Cooldown after TP (ticks) |
| `max-interior-size` | int | Max blocks inside the frame |
| `particles.interval` | int | Particle spawn interval |
| `particles.count` | int | Particles per cycle |
| `particles.spread` | double | Particle spread |
| `particles.color.*` | int 0–255 | Dust RGB color |
| `particles.size` | double | Particle size |
| `portal.fill-material` | Material | Interior fill material |
| `portal.ambient-particle` | Particle | Extra ambient effect |
| `portal.ambient-count` | int | Ambient particle count |
| `portals` | list | Portal data (auto-saved) |

After editing, run `/portal reload`.

---

## How frame creation works

1. The player starts a create command and holds a **recovery compass**.
2. Right-clicks a frame block or air inside the frame.
3. `FrameAnalyzer` searches for a closed outline on one plane.
4. Coordinates are checked against existing portals.
5. The interior is filled, teleport points and orientation are stored.
6. Particle tasks start (Folia-compatible).

Analyzer limits keep huge builds from overloading the server. `max-interior-size` from the config applies as well.

---

## Building

### Clone

```bash
git clone https://github.com/Goblin_Thug/PortalWarp.git
cd PortalWarp
```

> Replace the URL with your actual repository address.

### Maven

```bash
mvn clean package
```

Output artifact:

```text
target/PortalWarp-1.0.0.jar
```

(exact name depends on `finalName` and the version in `pom.xml`).

### Build environment

- JDK **25**
- Maven with access to the [PaperMC](https://repo.papermc.io/repository/maven-public/) repository

---

## Project layout

```text
PortalWarp/
├── pom.xml
├── README.md                 # Documentation (EN, primary)
├── README_RU.md              # Documentation (RU, additional)
├── LICENSE
├── CHANGELOG.md
├── CONTRIBUTING.md
├── SECURITY.md
├── .gitignore
├── .gitattributes
├── .editorconfig
└── src/main/
    ├── java/ru/goblin_thug/portalwarp/
    │   ├── PortalWarp.java              # Main plugin class
    │   ├── commands/CommandHandler.java # /portal handling
    │   ├── listeners/
    │   │   ├── CompassListener.java     # Frame selection
    │   │   └── PortalListener.java      # Teleport / connect
    │   ├── managers/
    │   │   ├── PortalManager.java       # Storage, particles, save
    │   │   └── Portal.java              # Portal model
    │   └── utils/
    │       ├── FrameAnalyzer.java       # Frame detection
    │       ├── FrameData.java           # Frame data
    │       └── Utils.java
    └── resources/
        ├── paper-plugin.yml
        └── config.yml
```

---

## Compatibility notes

- Uses the modern Paper plugin loader (`paper-plugin.yml`).
- `folia-supported: true` — stick to region schedulers; do not move logic onto legacy global schedulers without adapting it.
- The `portals` section is **managed by the plugin**. Manual edits may break loading.
- Cross-server transfer depends on the client/proxy stack. Ensure the address is reachable by players.

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).  
Security issues: [SECURITY.md](SECURITY.md).

---

## License

Released under the **MIT** License.  
Copyright (c) 2026 **Goblin_Thug**.

Full text: [LICENSE](LICENSE).

---

## Author

**Goblin_Thug**  
Plugin version: **1.0.0**

If this project helps you, star the repo and report bugs via Issues.
