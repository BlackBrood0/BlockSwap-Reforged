# Configuration Guide

Block Swap Reforged uses **JSON5** for its primary configuration file located at `config/blockswap/block_swap.json5`.

JSON5 provides enhanced readability compared to standard JSON by supporting:
- Single-line comments (`// comment`)
- Multi-line block comments (`/* comment */`)
- Trailing commas after lists and object elements
- Cleaner syntax without requiring strict quote escapes everywhere

---

## Configuration File Structure

Below is an overview of the fields supported in `config/blockswap/block_swap.json5`:

```json5
{
  // 1. Optional Global Dimension Condition
  // Restricts all swaps in this file to one or more dimensions.
  "dimension": "minecraft:overworld",

  // 2. Block and Tag Swapper
  // Maps source blocks or tags (#tag) to replacement blocks.
  "swapper": {
    "minecraft:granite": "minecraft:diorite",
    "#c:ores": "minecraft:infested_cobblestone"
  },

  // 3. Explicit State Swapper
  // Maps exact source block states with properties to target block states.
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

  // 4. Retroactive Generation
  // When true, scans and updates blocks in existing chunks when loaded.
  "retro_gen": false,

  // 5. Generate All Known States
  // When true, creates JSON5 reference files for all registered block states.
  "generate_all_known_states": true,

  // 6. Natural Generation Only Mode
  // When true, swaps apply ONLY during chunk generation (terrain, features, structures).
  // Live player placements, pistons, and dispensers remain unchanged.
  "onlySwapNaturalGeneration": false
}
```

---

## Field Reference

### `dimension` / `dimensions` (Optional)
- **Type**: String or Array of Strings
- **Default**: Omitted (applies to all dimensions)
- **Accepted Keys**: `dimension`, `dimensions`, `dimension_condition`, `dimensionCondition`, `dim`, `dims`
- **Description**: Sets a global dimension condition for all swaps defined in the configuration file. If specified, swaps will only execute when the current world dimension matches one of the specified IDs.
- **Examples**:
  - `"dimension": "minecraft:overworld"`
  - `"dimensions": ["minecraft:overworld", "minecraft:the_nether"]`
  - Shorthand names without `minecraft:` are automatically resolved (e.g. `"the_nether"` resolves to `minecraft:the_nether`).

---

### `swapper`
- **Type**: Object (Key-Value map)
- **Default**: `{}`
- **Description**: Defines block-to-block and tag-to-block replacements.
  - **Key**: A registry block ID (e.g. `"minecraft:dirt"`) or a block tag starting with `#` (e.g. `"#c:ores"` or `"#minecraft:logs"`).
  - **Value**:
    - **String**: The target block ID (e.g. `"minecraft:sponge"`).
    - **Object**: Advanced entry specifying a target block and per-swap conditions:
      ```json5
      "minecraft:stone": {
        "replaceWith": "minecraft:granite",
        "dimension": "minecraft:the_nether"
      }
      ```
    - **Array**: Multiple rules for the same key across different dimensions:
      ```json5
      "minecraft:stone": [
        { "replaceWith": "minecraft:granite", "dimension": "minecraft:the_nether" },
        { "replaceWith": "minecraft:diorite", "dimension": "minecraft:the_end" }
      ]
      ```

---

### `state_swapper`
- **Type**: Array of Objects
- **Default**: `[]`
- **Description**: Allows pinpoint replacement of exact block state combinations. When an incoming block state matches all properties of the `old` state, it is replaced by the `new` state.
- **Object Schema**:
  - `old`: Serialized Minecraft block state object containing `"Name"` and optional `"Properties"`.
  - `new`: Serialized Minecraft block state object containing `"Name"` and optional `"Properties"`.
  - `dimension` (optional): Restricts the state swap to a specific dimension.

---

### `onlySwapNaturalGeneration`
- **Type**: Boolean (`true` or `false`)
- **Default**: `false`
- **Accepted Aliases**: `onlySwapNaturalGeneration`, `onlyNaturalGeneration`, `onlyNaturalGen`, `naturalOnly`, `naturalGenerationOnly`
- **Description**:
  - When `true`: Swaps apply **only** during chunk generation (terrain, biomes, surface, features, and structures). Any block placed by a player, command, piston, dispenser, or falling block will NOT be swapped.
  - When `false`: Swaps apply universally across both chunk generation and live in-game actions.

---

### `retro_gen`
- **Type**: Boolean (`true` or `false`)
- **Default**: `false`
- **Description**: When enabled, existing chunks that were generated before Block Swap was installed or configured will be scanned and updated when loaded.
- **Recommendation**: Leave `false` unless you intentionally want to convert existing, already-built areas.

---

### `generate_all_known_states`
- **Type**: Boolean (`true` or `false`)
- **Default**: `true`
- **Description**: When `true`, the mod creates reference files for every block in the game under `config/blockswap/known_states/<namespace>/<block_name>.json5`. This makes it easy to look up property names and values when configuring `state_swapper`.

---

## Safety and Circular Reference Prevention

Block Swap Reforged includes automatic circular reference protection:
- If block `A` maps to block `B`, and block `B` is also configured to map to block `C` in the same dimension:
  ```json5
  "swapper": {
    "minecraft:dirt": "minecraft:stone",
    "minecraft:stone": "minecraft:diamond_block" // Circular mapping detected!
  }
  ```
- The mod detects the circular mapping during startup, outputs a clear warning in the server logs, and removes the circular loop to prevent infinite recursion and server crashes.
