# Block Swap Reforged

Block Swap Reforged is a Minecraft mod for NeoForge 1.21.1 that replaces one block with another based on configuration, both during world generation and when blocks are placed in an active world.

When swapping blocks, the mod automatically detects and preserves shared block state properties (such as rotation axis, facing direction, half, shape, and waterlogged status) from the original block to the replacement block.

---

## Features

- World Generation Replacement: Replaces blocks during chunk generation so targeted blocks never appear in newly created terrain.
- Live Placement Replacement: Intercepts block placement in real time (players, commands, dispensers) and immediately places the replacement block.
- Automatic Property Remapping: Automatically maps matching block state properties across blocks (for example: oak log axis to birch log axis, or stairs facing/half/shape).
- Explicit State Swapping: Configure exact state-to-state mappings with defined properties in the state swapper table.
- Standalone JSON5 Support: Config files support single-line comments, multi-line comments, and trailing commas without requiring external library mods.
- Server to Client Sync: The server automatically sends active swap rules to connecting clients to keep client prediction in sync.
- World Load Reloading: Configuration reloads automatically every time a world or server loads, allowing on-the-fly updates.
- Retro-Generation: Optional setting to scan and update blocks in existing, already-generated chunks.
- Missing Block ID Repair: Utility configuration to remap missing or removed mod block IDs in chunk storage palettes.

---

## Requirements

- Minecraft: 1.21.1
- Mod Loader: NeoForge 21.1.x (tested on 21.1.263+)
- Java: 21

---

## Installation

### Client
1. Install NeoForge for Minecraft 1.21.1.
2. Download the latest `blockswap-neoforge-1.21.1-x.x.x.x.jar` from the Releases page.
3. Place the jar file into your `.minecraft/mods` directory.

### Dedicated Server
1. Set up a NeoForge 1.21.1 server.
2. Place the jar file into the server's `mods` folder.
3. Start the server. The configuration folder and default config file will be created automatically.

---

## Configuration

Configuration files are located in `config/blockswap/`:

- `block_swap.json5`: Primary configuration for block and state replacements.
- `missing_block_ids.json5`: Secondary configuration for chunk palette ID repairs.

### Example: block_swap.json5

```json5
{
  // Simple block-to-block mappings with automatic property transfer
  "swapper": {
    "minecraft:granite": "minecraft:diorite",
    "minecraft:oak_log": "minecraft:birch_log",
    "minecraft:oak_stairs": "minecraft:birch_stairs"
  },

  // Explicit state mappings for targeted property configurations
  "state_swapper": [
    {
      "old": {
        "Name": "minecraft:gold_block"
      },
      "new": {
        "Name": "minecraft:emerald_block"
      }
    }
  ],

  // Whether blocks in already-generated chunks are scanned and updated
  "retro_gen": false,

  // Generates reference files for all block states in config/blockswap/known_states/
  "generate_all_known_states": false
}
```

---

## Building from Source

This project uses Gradle with ModDevGradle.

1. Clone the repository:
   ```bash
   git clone https://github.com/BlackBrood0/BlockSwap-Reforged.git
   cd BlockSwap-Reforged
   ```

2. Build the project with Java 21:
   ```bash
   ./gradlew build
   ```

3. The compiled jar file will be located in:
   ```
   build/libs/blockswap-neoforge-1.21.1-6.0.0.0.jar
   ```

---

## License and Attribution

- Original Author: CorgiTaco
- Original Project: https://github.com/CorgiTaco-MC/Block-Swap
- Ported to NeoForge 1.21.1 by BlackBrood0
- Licensed under the GNU Lesser General Public License v3.0 (LGPL-3.0).
