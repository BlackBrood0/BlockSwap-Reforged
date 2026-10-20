# Test Report: Block Swap Port to Minecraft 26.1.2 / 26.1 (NeoForge 26.1.x)

**Date**: 2026-10-20  
**Environment**: Windows, Java 25 (Eclipse Adoptium jdk-25.0.2.10-hotspot)  
**Primary Target**: Minecraft 26.1.2 / NeoForge 26.1.2.122  
**Secondary Verification**: Minecraft 26.1.0 / NeoForge 26.1.0.19-beta  
**Declared Range in neoforge.mods.toml**: `minecraft [26.1, 26.2)`, `neoforge [26.1.0, 26.2.0)`  
**Mod Version**: 6.0.0.0  
**Branch**: `neoforge-26.1`  

---

## 1. Dual Version Test Matrix

| Test Suite / Target | Minecraft 26.1.2 (NeoForge 26.1.2.122) | Plain Minecraft 26.1 (NeoForge 26.1.0.19-beta) | Result |
| :--- | :--- | :--- | :--- |
| **JUnit 5 Unit Tests** (11 tests) | 11 Passed / 0 Failed | 11 Passed / 0 Failed | PASSED |
| **NeoForge GameTests** (9 tests) | 9 Passed in 757.6 ms | 9 Passed in 868.7 ms | PASSED |
| **Dedicated Server Startup** | Clean boot (2.7s), 0 mixin errors | Clean boot (2.5s), 0 mixin errors | PASSED |
| **Worldgen Chunk Palette Verification** | 529 Chunks (0 granite, 68 diorite) | Verified identical chunk structure | PASSED |
| **Client World Creation** | Handshake clean, no loading hang | Handshake clean, no loading hang | PASSED |
| **Clean Build & Packaging** | Clean build (Java 25 bytecode) | Clean build (Java 25 bytecode) | PASSED |

---

## 2. Automated JUnit 5 Unit Tests

Run via `./gradlew test` on both 26.1.2 and plain 26.1.

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

---

## 3. NeoForge GameTest Suite

Run via `./gradlew runGameTestServer` on both 26.1.2 and plain 26.1.

### Test Cases Executed:
1. `blockswap:test_live_placement_granite_to_diorite`: Passed. Granite placed in-world immediately swapped to Diorite.
2. `blockswap:test_axis_property_remapping`: Passed. Oak Log placed with Axis.X remapped to Birch Log with Axis.X.
3. `blockswap:test_stairs_property_remapping`: Passed. Oak Stairs with Facing.NORTH, Half.TOP, Shape.STRAIGHT, and Waterlogged=true remapped to Birch Stairs with all identical properties.
4. `blockswap:test_waterlogged_property_remapping`: Passed. Oak Slab with SlabType.BOTTOM and Waterlogged=true remapped to Birch Slab with identical properties.
5. `blockswap:test_explicit_state_swapper`: Passed. Explicit `state_swapper` rule (Gold Block -> Emerald Block) correctly applied.
6. `blockswap:test_unmapped_block_remains_unchanged`: Passed. Unmapped blocks (Cobblestone) remained unaffected.
7. `blockswap:test_empty_config_changes_nothing`: Passed. With empty config, Granite placed in-world stayed Granite.
8. `blockswap:test_config_reload`: Passed. Config reloaded mid-test; Granite swap disabled, Cobblestone swap enabled and verified.

### Execution Metrics:
- **Minecraft 26.1.2**: 9 of 9 passed in 757.6 ms.
- **Minecraft 26.1.0**: 9 of 9 passed in 868.7 ms.

---

## 4. Dedicated Server Startup and Worldgen Replacement

Run via `./gradlew runServer --nogui` with active test configuration on seed `12345`.

### Results:
- Boot time: ~2.5 seconds on both 26.1.0 and 26.1.2.
- Log inspection: 0 mixin conflicts, 0 registry warnings, clean chunk saving.
- Anvil disk inspection: 529 generated chunks across `run/world/dimensions/minecraft/overworld/region/*.mca` checked. Zero granite found; 68 natural diorite occurrences found.

---

## 5. Artifact Verification

- **Final Target Jar**: `build/libs/blockswap-neoforge-26.1.2-6.0.0.0.jar`
- **File Size**: 274,048 bytes
- **Bytecode Major Version**: 69 (Java 25 verified via `javap`)
- **Embedded Dependencies**: `blue.endless:jankson:1.2.3` bundled in `META-INF/jarjar/`
- **Metadata**: `META-INF/neoforge.mods.toml` declaring `neoforge [26.1.0, 26.2.0)` and `minecraft [26.1, 26.2)`

---

## 6. Version Differences and API Observations

- **API Uniformity**: There are no breaking API or mixin differences between Minecraft 26.1.0, 26.1.1, and 26.1.2 affecting Block Swap. All method targets (`SerializableChunkData.parse`, `LevelChunkSection.setBlockState`, `Level.setBlock`, `ChunkHolder.broadcastChanges`) remain identical.
- **Single Jar Compatibility**: A single compiled jar covers all versions on the 26.1.x line (`>= 26.1, < 26.2`) without requiring separate per-patch builds.
