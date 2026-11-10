# Block and Tag Swapping

Block Swap Reforged provides two primary mechanisms for replacing blocks in the `swapper` table:
1. **Direct Block-to-Block Swapping**
2. **Tag-Based Mass Swapping**

---

## 1. Direct Block-to-Block Swapping

Direct mappings replace a specific block with a target block.

```json5
"swapper": {
  "minecraft:granite": "minecraft:diorite",
  "minecraft:oak_log": "minecraft:birch_log",
  "minecraft:oak_stairs": "minecraft:birch_stairs",
  "minecraft:oak_slab": "minecraft:birch_slab"
}
```

### Automatic Property Preservation

When swapping blocks directly, Block Swap Reforged inspects the source block state and automatically transfers matching properties to the replacement block:

- **Logs and Pillars (`axis`)**: An oak log oriented along the Z axis (`axis=z`) swapped to a birch log will maintain the Z axis orientation (`axis=z`).
- **Stairs (`facing`, `half`, `shape`, `waterlogged`)**: Upside-down inner stairs facing SOUTH will retain the upside-down half, inner-left shape, and south facing when converted to a different stair type.
- **Slabs (`type`, `waterlogged`)**: Top slabs and double slabs retain their exact slab type (`top`, `bottom`, `double`).
- **Waterlogged Blocks**: Any block with the `waterlogged` property retains its submerged status if the replacement block supports waterlogging.

Properties that do not exist on the target block are safely ignored, and target properties without an incoming equivalent receive the target block's default state.

---

## 2. Tag-Based Mass Swapping

Instead of listing dozens of individual block IDs, you can target entire Minecraft or NeoForge tags by prefixing the key with `#`.

### Example: Replacing All Ores

```json5
{
  "swapper": {
    "#c:ores": "minecraft:infested_cobblestone"
  }
}
```
With this single entry, every block registered under the `#c:ores` common convention tag (including coal ore, iron ore, gold ore, diamond ore, copper ore, redstone ore, lapis ore, and their deepslate variants) will be replaced by `minecraft:infested_cobblestone`.

### Popular Tag Examples

| Tag Key | Scope | Description |
| :--- | :--- | :--- |
| `"#c:ores"` | Common / Conventional | Matches all vanilla and modded ores |
| `"#minecraft:logs"` | Vanilla | Matches all wood log and wood bark blocks |
| `"#minecraft:leaves"` | Vanilla | Matches all tree leave blocks |
| `"#minecraft:dirt"` | Vanilla | Matches dirt, grass blocks, podzol, mycelium, and coarse dirt |
| `"#c:sand"` | Common / Conventional | Matches regular sand, red sand, and modded sands |
| `"#minecraft:wool"` | Vanilla | Matches all 16 colored wool blocks |

---

## 3. Tag Precedence and Overrides

You can combine tag mappings with direct block mappings. **Direct block mappings always take precedence over tag mappings.**

### Example: Custom Ore Replacement with Override

```json5
{
  "swapper": {
    // 1. Replace all ores with infested cobblestone by default
    "#c:ores": "minecraft:infested_cobblestone",

    // 2. Override specific ores to custom targets
    "minecraft:diamond_ore": "minecraft:emerald_block",
    "minecraft:deepslate_diamond_ore": "minecraft:emerald_block"
  }
}
```
**Outcome**:
- Coal, iron, gold, copper, lapis, and redstone ores become `minecraft:infested_cobblestone`.
- Diamond ores (both regular and deepslate) become `minecraft:emerald_block`.

---

## 4. Dynamic Datapack Tag Reloading

In Minecraft 1.21.1, tags are populated and modified by datapacks.

Block Swap Reforged automatically registers an event listener for `TagsUpdatedEvent`:
- If datapacks are loaded, refreshed, or reloaded via `/reload`, the mod re-evaluates all `#tag` definitions in real time.
- If a custom datapack introduces new blocks to `#c:ores` while the server is running, the new blocks are automatically included in the swap table without requiring a server reboot.
