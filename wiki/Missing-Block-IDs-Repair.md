# Missing Block IDs Repair

The **Missing Block IDs Repair** utility is an essential maintenance tool included with Block Swap Reforged. It allows administrators to salvage worlds when mods are removed or when modded block identifiers change between updates.

---

## The Problem: Removed Mods and World Corruption

In Minecraft, saved world chunks store block information inside serialized NBT data called a **Chunk Section Palette**.

When you remove a mod from an existing world:
1. The game attempts to load chunks containing block identifiers from the removed mod (e.g. `"techmod:generator"`).
2. Because the mod is no longer present, Minecraft cannot find the block in its active registry.
3. Depending on the situation, this can result in:
   - Chunk loading crashes
   - "Missing block" ghost textures
   - Unwanted conversion of whole structures into empty air

---

## How Block Swap Solves It

Block Swap Reforged hooks directly into the chunk deserializer (`ChunkSerializer.read`) before chunks are instantiated in memory.

When a chunk is loaded from disk, the mod inspects the raw section palette tags:
- If a palette entry matches an ID listed in `config/blockswap/missing_block_ids.json5`, the mod updates the entry in the palette to your designated replacement block.
- The chunk loads cleanly without errors, and the repaired palette is permanently saved back to the world region files.

---

## Configuration: `missing_block_ids.json5`

The file is located at:
```
config/blockswap/missing_block_ids.json5
```

### Syntax Example

```json5
{
  "id_remapper": {
    // Map missing machine blocks to iron blocks
    "techmod:steam_generator": "minecraft:iron_block",
    "techmod:electric_furnace": "minecraft:furnace",

    // Map removed decorative blocks to smooth stone
    "decorcraft:ornate_pillar": "minecraft:smooth_stone",

    // Clean up removed utility blocks with air
    "obsoletemod:broken_crate": "minecraft:air"
  }
}
```

---

## Differences: `block_swap.json5` vs `missing_block_ids.json5`

| Feature | `block_swap.json5` | `missing_block_ids.json5` |
| :--- | :--- | :--- |
| **Primary Purpose** | Gameplay block transformation & worldgen customization | Chunk salvage & removed mod cleanup |
| **Execution Point** | Live world generation and gameplay placement | Raw chunk NBT disk deserialization |
| **Requires Source Block in Registry?** | **Yes** (Both source and target must be registered) | **No** (Source block does NOT need to exist in the game) |
| **Affects Newly Placed Blocks?** | Yes | No (Only affects chunks loaded from disk) |
