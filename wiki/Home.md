# Block Swap Reforged Wiki

Welcome to the official documentation and wiki for **Block Swap Reforged**, a Minecraft mod for NeoForge 1.21.1.

Block Swap Reforged allows server administrators, modpack creators, and players to replace any block or block tag with another block or state, both during natural world generation and during live in-game block placement.

---

## Key Features

- **Block and Tag Replacement**: Map individual blocks or entire block tags (e.g. `#c:ores` or `#minecraft:logs`) to target blocks.
- **Dimension Conditions**: Restrict block swaps to specific dimensions (Overworld, Nether, End, or custom dimensions), configured globally or per-swap.
- **Natural Generation Only Mode (`onlySwapNaturalGeneration`)**: Restrict swaps exclusively to newly generated chunks (terrain, features, structures), leaving player building, pistons, dispensers, and falling blocks completely untouched.
- **In-Game Hot Reload (`/blockswap reload`)**: Reload placement swap rules live without restarting the server or kicking players, while keeping world generation conditions untouched.
- **Automatic Property Preservation**: Automatically transfers shared block state properties (such as rotation axis, facing directions, slab types, stair shapes, and waterlogged values) to replacement blocks.
- **Explicit State Swapping**: Define exact block state configurations with precise properties using the state swapper table.
- **Retro-Generation**: Optionally scan and swap blocks in existing, previously generated chunks.
- **Palette Repair Utility**: Fix worlds with missing or renamed mod blocks using `missing_block_ids.json5`.
- **Zero External Dependencies**: Pure JSON5 parsing and NeoForge networking with no library mod prerequisites.

---

## Wiki Contents

1. **[Getting Started](Getting-Started.md)**
   System requirements, client installation, dedicated server installation, and directory structure.

2. **[Configuration Guide](Configuration-Guide.md)**
   Full breakdown of `block_swap.json5`, configuration syntax, comments, options, and default templates.

3. **[Block and Tag Swapping](Block-and-Tag-Swapping.md)**
   Detailed guide to block mappings, tag syntax (`#c:ores`), tag precedence, and automatic block state property preservation.

4. **[Dimension Conditions](Dimension-Conditions.md)**
   How to configure global dimension conditions, per-swap conditions, multi-dimension arrays, and dimension overrides.

5. **[Natural Generation vs Live Placement](Natural-Generation-vs-Live-Placement.md)**
   Understanding chunk generation swapping vs live placement, `onlySwapNaturalGeneration` mode, and performance characteristics.

6. **[Commands and Hot Reload](Commands-and-Hot-Reload.md)**
   Using `/blockswap reload`, permission requirements, multiplayer network synchronization, and datapack reload behavior.

7. **[State Swapper and Known States](State-Swapper-and-Known-States.md)**
   Advanced block state transformations, syntax, and generating the `known_states/` reference directory.

8. **[Missing Block IDs Repair](Missing-Block-IDs-Repair.md)**
   Salvaging worlds and chunks when mod blocks are removed or IDs change via `missing_block_ids.json5`.

9. **[Developer and Build Guide](Developer-and-Build-Guide.md)**
   Compiling from source with Gradle, Mixin architecture, running JUnit unit tests, and executing NeoForge GameTests.

---

## Project Information

- **Current Version**: `1.2-beta` (NeoForge 1.21.1)
- **Maintainer**: BlackBrood0
- **Original Mod**: Block Swap by CorgiTaco
- **License**: GNU Lesser General Public License v3.0 (LGPL-3.0)
- **Repository**: [https://github.com/BlackBrood0/BlockSwap-Reforged](https://github.com/BlackBrood0/BlockSwap-Reforged)
- **Issue Tracker**: [https://github.com/BlackBrood0/BlockSwap-Reforged/issues](https://github.com/BlackBrood0/BlockSwap-Reforged/issues)
