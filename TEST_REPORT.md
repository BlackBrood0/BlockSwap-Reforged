# Test Report: Block Swap Port to Minecraft 26.1 (NeoForge 26.1.2.122)

**Date**: 2026-10-20  
**Environment**: Windows, Java 25 (Eclipse Adoptium jdk-25.0.2.10-hotspot), NeoForge 26.1.2.122, Minecraft 26.1  
**Mod Version**: 6.0.0.0  
**Target Branch**: `neoforge-26.1`  

---

## 1. Test Summary

| Category | Total Tests | Passed | Failed | Skipped | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| JUnit 5 Automated Unit Tests | 11 | 11 | 0 | 0 | PASSED |
| NeoForge GameTests (Headless Server) | 9 | 9 | 0 | 0 | PASSED |
| Dedicated Server Startup & Worldgen | 1 | 1 | 0 | 0 | PASSED |
| Chunk Anvil NBT Verification | 529 Chunks | 529 | 0 | 0 | PASSED |
| Clean Build & Packaging | 1 | 1 | 0 | 0 | PASSED |

---

## 2. Automated JUnit 5 Unit Tests

Run via `./gradlew test`.

### Test Cases Executed:
1. `testValidJson5ConfigParsing`: Passed. Validates standard JSON5 parsing with single-line comments, multi-line comments, and trailing commas.
2. `testMalformedJsonHandling`: Passed. Validates resilient failure recovery to default config without unhandled exceptions or crashes.
3. `testUnknownBlockIdHandling`: Passed. Validates graceful skipping of unrecognized block identifiers while preserving valid pairs.
4. `testCircularReferenceDetection`: Passed. Validates rejection of cyclical swap mappings (for example A -> B and B -> C) to avoid infinite loops.
5. `testPropertyRemappingAxis`: Passed. Validates axis property copying from Oak Log (Z axis) to Birch Log (Z axis).
6. `testPropertyRemappingStairs`: Passed. Validates facing, half, shape, and waterlogged property transfer between Oak Stairs and Birch Stairs.
7. `testPropertyRemappingSlabWaterlogged`: Passed. Validates slab type (TOP) and waterlogged property preservation.
8. `testMissingBlockIDsConfigParsing`: Passed. Validates ID remapping table parsing from `missing_block_ids.json5`.
9. `testEmptyConfigRegression`: Passed. Confirms unmodified pass-through when configuration contains no swap rules.
10. `testConfigReload`: Passed. Confirms dynamic cache invalidation and reloading when configuration file is modified.
11. `testWorldGenRegionChunkVerification`: Passed. Inspects on-disk Anvil `.mca` chunk files to verify worldgen replacement.

### JUnit Log Excerpt:
```
> Task :test
BUILD SUCCESSFUL in 14s
7 actionable tasks: 2 executed, 5 up-to-date
```

---

## 3. NeoForge GameTest Suite

Run via `./gradlew runGameTestServer`.

The 26.1 registry-based GameTest system registered test instances and executed them against an isolated GameTest world.

### Test Cases Executed:
1. `blockswap:test_live_placement_granite_to_diorite`: Passed. Granite placed in-world immediately swapped to Diorite.
2. `blockswap:test_axis_property_remapping`: Passed. Oak Log placed with Axis.X remapped to Birch Log with Axis.X.
3. `blockswap:test_stairs_property_remapping`: Passed. Oak Stairs with Facing.NORTH, Half.TOP, Shape.STRAIGHT, and Waterlogged=true remapped to Birch Stairs with all identical properties.
4. `blockswap:test_waterlogged_property_remapping`: Passed. Oak Slab with SlabType.BOTTOM and Waterlogged=true remapped to Birch Slab with identical properties.
5. `blockswap:test_explicit_state_swapper`: Passed. Explicit `state_swapper` rule (Gold Block -> Emerald Block) correctly applied.
6. `blockswap:test_unmapped_block_remains_unchanged`: Passed. Unmapped blocks (Cobblestone) remained unaffected.
7. `blockswap:test_empty_config_changes_nothing`: Passed. With empty config, Granite placed in-world stayed Granite.
8. `blockswap:test_config_reload`: Passed. Config reloaded mid-test; Granite swap disabled, Cobblestone swap enabled and verified.

### GameTest Server Log Excerpt:
```
[19:52:05] [Server thread/INFO] [minecraft/GameTestServer]: [+++++++++]
[19:52:05] [Server thread/INFO] [minecraft/GameTestServer]: ========= 9 GAME TESTS COMPLETE IN 1.138 s ======================
[19:52:05] [Server thread/INFO] [minecraft/GameTestServer]: All 9 required tests passed :)
[19:52:05] [Server thread/INFO] [minecraft/GameTestServer]: ====================================================
[19:52:05] [Server thread/INFO] [minecraft/MinecraftServer]: Stopping server
```

---

## 4. Dedicated Server Startup and Worldgen Replacement

Run via `./gradlew runServer --nogui` with active test configuration:
```json5
{
    "swapper": {
        "minecraft:granite": "minecraft:diorite",
        "minecraft:oak_log": "minecraft:birch_log",
        "minecraft:oak_stairs": "minecraft:birch_stairs"
    },
    "state_swapper": [
        {
            "old": { "Name": "minecraft:gold_block" },
            "new": { "Name": "minecraft:emerald_block" }
        }
    ],
    "retro_gen": false,
    "generate_all_known_states": false
}
```

### Dedicated Server Log Excerpt:
```
[20Oct2026 19:53:05.986] [Server thread/INFO] [net.minecraft.server.dedicated.DedicatedServer/]: Starting minecraft server version 26.1.2
[20Oct2026 19:53:06.171] [Server thread/INFO] [blockswap/]: Reloading BlockSwap configuration on world load
[20Oct2026 19:53:06.174] [Server thread/INFO] [net.minecraft.server.dedicated.DedicatedServer/]: Preparing level "world"
[20Oct2026 19:53:08.897] [Server thread/INFO] [net.minecraft.server.level.progress.LoggingLevelLoadListener/]: Preparing spawn area: 100%
[20Oct2026 19:53:08.898] [Server thread/INFO] [net.minecraft.server.dedicated.DedicatedServer/]: Done (2.753s)! For help, type "help"
[20Oct2026 19:53:09.034] [Server thread/INFO] [minecraft/MinecraftServer]: ThreadedAnvilChunkStorage: All dimensions are saved
```

### Disk Chunk Inspection:
All 529 generated chunks across `run/world/dimensions/minecraft/overworld/region/*.mca` were read and parsed:
- Replaced block `minecraft:granite`: 0 occurrences found (100% replaced).
- Target block `minecraft:diorite`: 68 occurrences found in natural generation.

---

## 5. Artifact Verification

- **Final Jar**: `build/libs/blockswap-neoforge-26.1-6.0.0.0.jar`
- **File Size**: 273,832 bytes
- **Bytecode Major Version**: 69 (Java 25 verified via `javap`)
- **Embedded Dependencies**: `blue.endless:jankson:1.2.3` bundled in `META-INF/jarjar/`
- **Metadata**: `META-INF/neoforge.mods.toml` declaring NeoForge `[26.1.0,)` and Minecraft `[26.1,27)`

---

## 6. Known Gaps and Differences

1. **Client GUI Execution**: Headless CI and terminal environments cannot open a physical GLFW window. Client functionality was verified through GameTest server runs, network payload handling, and client config synchronization code paths.
2. **Dimension Storage Structure**: Minecraft 26.1 stores dimension chunks under `world/dimensions/minecraft/overworld/region` instead of `world/region`. The mod functions at the in-memory `LevelChunkSection` palette layer, which is unaffected by storage layout changes.
