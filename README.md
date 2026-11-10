# Block Swap Reforged

Block Swap Reforged is a Minecraft mod for NeoForge 1.21.1 that replaces blocks and block tags with other blocks or block states, both during natural world generation and during live in-game placement.

When swapping blocks, the mod automatically detects and preserves shared block state properties (such as rotation axis, facing direction, half, shape, and waterlogged status) from the original block to the replacement block.

---

## Documentation and Wiki

Complete documentation, guides, and tutorials are available in the repository **[Wiki](wiki/Home.md)**:

- **[Wiki Home](wiki/Home.md)** - Overview and table of contents
- **[Getting Started](wiki/Getting-Started.md)** - Requirements, installation for client and dedicated server
- **[Configuration Guide](wiki/Configuration-Guide.md)** - Complete reference for `block_swap.json5`
- **[Block and Tag Swapping](wiki/Block-and-Tag-Swapping.md)** - Block mappings, `#tag` syntax, and property preservation
- **[Dimension Conditions](wiki/Dimension-Conditions.md)** - Global and per-swap dimension filtering
- **[Natural Gen vs Live Placement](wiki/Natural-Generation-vs-Live-Placement.md)** - Worldgen vs live placement, `onlySwapNaturalGeneration` mode
- **[Commands and Hot Reload](wiki/Commands-and-Hot-Reload.md)** - The `/blockswap reload` command and live multiplayer sync
- **[State Swapper and Known States](wiki/State-Swapper-and-Known-States.md)** - Advanced state transformations and property reference files
- **[Missing Block IDs Repair](wiki/Missing-Block-IDs-Repair.md)** - Salvaging corrupted chunks with `missing_block_ids.json5`
- **[Developer and Build Guide](wiki/Developer-and-Build-Guide.md)** - Compiling from source, Mixin architecture, unit tests, and GameTests

---

## Features

- **Block and Tag Replacement**: Map individual blocks or entire tags (e.g. `"#c:ores": "minecraft:infested_cobblestone"`).
- **Dimension Conditions**: Restrict block swaps to specific dimensions (Overworld, Nether, End, or custom dimensions), configured globally or per-swap.
- **Natural Generation Only Mode (`onlySwapNaturalGeneration`)**: Restrict swaps exclusively to newly generated chunks, leaving all player building, pistons, dispensers, and falling blocks completely unchanged.
- **In-Game Hot Reload (`/blockswap reload`)**: Reload placement swap rules live without restarting the server or kicking players, while keeping world generation conditions untouched.
- **Automatic Property Preservation**: Automatically transfers shared block state properties (such as rotation axis, facing directions, slab types, stair shapes, and waterlogged values) to replacement blocks.
- **Explicit State Swapping**: Define exact block state configurations with precise properties using the state swapper table.
- **Standalone JSON5 Support**: Config files support single-line comments (`//`), multi-line comments (`/* */`), and trailing commas with zero external library mods required.
- **Server to Client Sync**: Dedicated servers automatically send active swap rules to connecting clients to keep prediction in sync.
- **Retro-Generation**: Optional setting to scan and update blocks in existing, already-generated chunks.
- **Palette Repair Utility**: Fix worlds with missing or renamed mod blocks using `missing_block_ids.json5`.

---

## Requirements

- **Minecraft**: `1.21.1`
- **Mod Loader**: **NeoForge** `21.1.0` or newer (tested on `21.1.263+`)
- **Java**: **Java 21**

---

## Installation

### Client
1. Install NeoForge for Minecraft 1.21.1.
2. Download the latest `blockswap-neoforge-1.21.1-x.x.x.x.jar` from the [Releases](https://github.com/BlackBrood0/BlockSwap-Reforged/releases) page.
3. Place the JAR file into your `.minecraft/mods` directory.

### Dedicated Server
1. Set up a NeoForge 1.21.1 server.
2. Place the JAR file into the server's `mods` folder.
3. Start the server. The configuration folder and default config file will be created automatically.

---

## Quick Example: `block_swap.json5`

```json5
{
  // Restrict all swaps in this file to the Overworld only
  "dimension": "minecraft:overworld",

  // Set to true if you only want naturally generated chunks to swap
  "onlySwapNaturalGeneration": false,

  "swapper": {
    // 1. Direct block mapping with automatic property preservation
    "minecraft:granite": "minecraft:diorite",

    // 2. Tag replacement (replaces all ores with infested cobblestone)
    "#c:ores": "minecraft:infested_cobblestone",

    // 3. Per-swap dimension condition (overrides global dimension)
    "minecraft:netherrack": {
      "replaceWith": "minecraft:gold_block",
      "dimension": "minecraft:the_nether"
    }
  }
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

2. Build the project and run verification tests with Java 21:
   ```bash
   ./gradlew build
   ```

3. The compiled JAR file will be located in:
   ```
   build/libs/blockswap-neoforge-1.21.1-1.2-beta.jar
   ```

---

## License and Attribution

- **Original Author**: CorgiTaco
- **Original Project**: https://github.com/CorgiTaco-MC/Block-Swap
- **Reforged and Maintained by**: BlackBrood0
- **License**: GNU Lesser General Public License v3.0 (LGPL-3.0)
