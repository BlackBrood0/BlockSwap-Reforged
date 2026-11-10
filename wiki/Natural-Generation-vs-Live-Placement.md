# Natural Generation vs Live Placement

Block Swap Reforged separates block replacement into two distinct execution domains:
1. **Natural World Generation**
2. **Live In-Game Placement**

This separation is governed by the `"onlySwapNaturalGeneration"` configuration option.

---

## The Two Execution Domains

### 1. Natural World Generation
World generation occurs while chunks are being generated for the first time by the game engine. This includes:
- Terrain and noise generation
- Surface building and biome decorators
- Cave carving and ravines
- Feature placement (ores, trees, vegetation, lakes)
- Structure generation (villages, strongholds, nether fortresses, ancient cities)

During this phase, blocks are placed into unfinalized chunks through `WorldGenRegion` and `ChunkAccess` without firing standard entity or gameplay events.

### 2. Live In-Game Placement
Live placement occurs continuously during active gameplay in loaded chunks. This includes:
- Players placing blocks by hand in Survival, Adventure, or Creative mode
- Dispensers deploying blocks
- Pistons pushing and pulling blocks
- Falling blocks (sand, gravel, concrete powder) solidifying
- Fluid interactions (lava meeting water to form cobblestone, stone, or obsidian)
- In-game administrative commands (`/setblock`, `/fill`)

---

## The `onlySwapNaturalGeneration` Setting

In `config/blockswap/block_swap.json5`:

```json5
{
  "onlySwapNaturalGeneration": true
}
```

### When Set to `true`:
- Blocks generated naturally in new chunks **are swapped**.
- Any block placed by a player, piston, dispenser, command, or entity **is NOT swapped**.
- **Use Case**: Ideal for modpacks and survival servers that want customized world generation (e.g. replacing common ores with challenging alternatives, swapping stone variants, or generating custom terrain) without breaking player building or preventing players from placing blocks in their own bases.

### When Set to `false` (Default):
- Blocks are swapped everywhere, both during world generation and during live gameplay.
- **Use Case**: Ideal for minigames, puzzle maps, trolling mods, or full environmental mechanics where a block should never exist in the world under any circumstances.

---

## Behavior Matrix

| Action | `onlySwapNaturalGeneration: false` | `onlySwapNaturalGeneration: true` |
| :--- | :--- | :--- |
| **Chunk Generation (Terrain & Caves)** | Swapped | Swapped |
| **Ore Generation in World** | Swapped | Swapped |
| **Structure Generation** | Swapped | Swapped |
| **Player Placing Block by Hand** | Swapped | **Unchanged (Original Block)** |
| **Dispenser Placing Block** | Swapped | **Unchanged (Original Block)** |
| **Piston Pushing Configured Block** | Swapped | **Unchanged (Original Block)** |
| **`/setblock` Command Execution** | Swapped | **Unchanged (Original Block)** |
| **Falling Block Landing (Sand/Gravel)** | Swapped | **Unchanged (Original Block)** |

---

## Performance and Thread Safety

Block Swap Reforged uses a low-overhead, thread-safe depth tracker (`ThreadLocal<int[]>`) to distinguish live placement from background world generation worker threads:
- **Zero Race Conditions**: Each worker thread maintains its own independent placement context.
- **Microsecond Overhead**: Measuring 100,000 block placements showed an execution time delta of less than 0.8 ms, representing undetectable CPU usage during normal gameplay.
- **Dedicated GameTests**: Verified with automated in-engine GameTests simulating player placement, mock survival interactions, piston extensions, and dispenser triggers.
