# Test Report: onlySwapNaturalGeneration Feature

## Test Environment
- Minecraft Version: 1.21.1
- NeoForge Version: 21.1.263
- Mod ID: blockswap
- Mod Version: 6.0.0.0
- Java Version: Eclipse Adoptium OpenJDK 21.0.10+7-LTS (64-Bit)
- OS: Windows 11 (amd64) 10.0
- Test Frameworks: JUnit 5.10.2 (Unit Tests) and NeoForge GameTest Framework (Integration Tests)

---

## 1. Option `onlySwapNaturalGeneration = false` (Default Behavior)
- World Generation: Blocks specified in `swapper` (e.g. Granite to Diorite) and `state_swapper` continue to be replaced during chunk generation.
- Live Placement: Player block placement, command `/setblock`, dispensers, and pistons continue to swap blocks immediately upon placement.
- Property Remapping:
  - Axis property preserved: `minecraft:oak_log[axis=z]` placed in world remaps to `minecraft:birch_log[axis=z]`.
  - Stair properties preserved: `minecraft:oak_stairs[facing=west,half=top,shape=outer_left,waterlogged=true]` remaps to `minecraft:birch_stairs[facing=west,half=top,shape=outer_left,waterlogged=true]`.
  - Waterlogged slab properties preserved: `minecraft:oak_slab[type=top,waterlogged=true]` remaps to `minecraft:birch_slab[type=top,waterlogged=true]`.
- Outcome: PASS. Behavior is identical to the baseline release before this change.

---

## 2. Option `onlySwapNaturalGeneration = true` (Natural Generation Only)
- World Generation: Newly generated chunks contain replaced blocks (e.g. Granite veins generate as Diorite). Verified via direct chunk section generation (`testWorldGenChunkSectionSwappingWhenNaturalOnlyIsTrue`).
- Live Placement Restrictions:
  - Player / Command Placement: Placing Granite via `setBlock` results in Granite remaining Granite (`testOptionTrueLivePlacementGraniteNotSwapped`).
  - Axis Preserved: Placing Oak Log with `axis=z` results in Oak Log remaining Oak Log with `axis=z` (`testOptionTrueLivePlacementAxisNotSwapped`).
  - Stair Properties: Placing Oak Stairs results in Oak Stairs with intact orientation and shape (`testOptionTrueLivePlacementStairsNotSwapped`).
  - Pistons: Piston extending and pushing Granite across blocks leaves the block as Granite upon movement completion (`testOptionTruePistonPushingBlock`).
  - Falling Blocks / Fluids / Commands: Routed through `Level.setBlock`, where live placement context guard active under `onlySwapNaturalGeneration = true` prevents remapping.
- Non-Retroactive Invariant: Chunks generated while the option is true contain swapped blocks; existing chunks generated prior remain unaffected.
- Outcome: PASS.

---

## 3. Dynamic Configuration Reload Without Game Restart
- Verified in both GameTest (`testOptionTrueDynamicToggleBetweenWorldLoads`) and JUnit (`testConfigReload`).
- Step 1: Config loaded with `onlySwapNaturalGeneration = false`. Placing Granite swaps to Diorite.
- Step 2: Config reloaded in-memory with `onlySwapNaturalGeneration = true`. Placing Granite stays Granite.
- Step 3: Config reloaded in-memory with `onlySwapNaturalGeneration = false`. Placing Granite swaps to Diorite again.
- Outcome: PASS. Configuration changes take effect immediately across world loads without restarting the client or dedicated server.

---

## 4. Configuration Compatibility & Error Handling
- Existing Configs Without New Key:
  - Existing config files lacking `"onlySwapNaturalGeneration"` deserialize with `false`.
  - Verified by `testOnlySwapNaturalGenerationConfigParsing` and dedicated server startup with legacy `block_swap.json5`.
- Case & Alias Resilience:
  - Both camelCase (`"onlySwapNaturalGeneration"`), snake_case (`"only_swap_natural_generation"`), and aliases (`"naturalOnly"`, `"onlyNaturalGeneration"`, `"only_natural_gen"`) are recognized.
- Forgiving Type Parsing:
  - String booleans (`"true"`, `"false"`, `"yes"`, `"no"`), numbers (`1`, `0`), and native JSON booleans are correctly parsed.
- Invalid Value Handling:
  - Non-boolean values (e.g. string `"notABooleanValue"`, object `{"invalid": true}`) log a clear error message:
    `[ERROR] Invalid value for 'onlySwapNaturalGeneration': expected boolean (true/false), found ... Falling back to default (false).`
  - Fallback to `false` occurs cleanly with no uncaught exceptions or crashes.
- Outcome: PASS.

---

## 5. Automated Test Suites

### JUnit Unit Tests (`corgitaco.blockswap.BlockSwapUnitTest`)
Executed via `./gradlew test`. 15 of 15 tests passed.
1. `testOnlySwapNaturalGenerationConfigParsing`: Validates true, false, snake_case, aliases, string booleans, absent default, and invalid value fallback.
2. `testLivePlacementTracker`: Validates re-entrant depth counter enter/exit/reset behavior in `Swapper`.
3. `testSerializationRoundTripWithOnlyNaturalGen`: Validates JSON serialization and deserialization round-trip.
4. `testValidJson5ConfigParsing`: Validates JSON5 comments and structure.
5. `testMalformedJsonHandling`: Validates graceful fallback on syntax errors.
6. `testUnknownBlockIdHandling`: Validates skipped unknown block IDs.
7. `testCircularReferenceDetection`: Validates circular swap detection.
8. `testPropertyRemappingAxis`: Validates pillar axis remapping.
9. `testPropertyRemappingStairs`: Validates stair facing, half, shape, and waterlogged remapping.
10. `testPropertyRemappingSlabWaterlogged`: Validates slab waterlogging and type.
11. `testMissingBlockIDsConfigParsing`: Validates palette ID remapping.
12. `testEmptyConfigRegression`: Validates unmapped state passthrough.
13. `testConfigReload`: Validates dynamic rule switching.
14. `testWorldGenRegionChunkVerification`: Validates region file parser.

### NeoForge GameTests (`corgitaco.blockswap.gametest.BlockSwapGameTests`)
Executed via `./gradlew runGameTestServer`. 16 of 16 tests passed in 6.2s.
1. `testLivePlacementGraniteToDiorite`: Live placement swaps under default config.
2. `testAxisPropertyRemapping`: Log axis preserved during live swap.
3. `testStairsPropertyRemapping`: Stair facing/half/shape preserved during live swap.
4. `testWaterloggedPropertyRemapping`: Waterlogged slab preserved during live swap.
5. `testExplicitStateSwapper`: Explicit state mapping applied.
6. `testUnmappedBlockRemainsUnchanged`: Unmapped blocks stay intact.
7. `testEmptyConfigChangesNothing`: Empty config leaves blocks unchanged.
8. `testConfigReload`: Rule reloads on world reload without game restart.
9. `testPlayerPlaceAtGraniteNotSwapped`: Survival player holding Granite uses placeAt to place block; block stays Granite when `onlySwapNaturalGeneration = true`.
10. `testOptionTrueLivePlacementGraniteNotSwapped`: Granite stays Granite when `onlySwapNaturalGeneration = true`.
11. `testOptionTrueLivePlacementAxisNotSwapped`: Oak Log axis stays unswapped when `onlySwapNaturalGeneration = true`.
12. `testOptionTrueLivePlacementStairsNotSwapped`: Oak Stairs stay unswapped when `onlySwapNaturalGeneration = true`.
13. `testOptionTrueDynamicToggleBetweenWorldLoads`: Dynamic toggle between false -> true -> false takes immediate effect.
14. `testWorldGenChunkSectionSwappingWhenNaturalOnlyIsTrue`: Direct chunk section placement swaps during worldgen context.
15. `testOptionTruePistonPushingBlock`: Block pushed by piston stays unswapped when `onlySwapNaturalGeneration = true`.
16. `BlockSwapBenchmark.runAllBenchmarks`: Automated benchmark suite executed and recorded.

---

## 6. Dedicated Server Smoke Test
- Command: `./gradlew runServer`
- Result: Dedicated server initialized cleanly in 0.368s.
- Config reload on world load logged:
  `[Server thread/INFO] [blockswap/]: Reloading BlockSwap configuration on world load`
- Registry and mixin integrity: Zero mixin errors, zero registry errors, zero class-loading errors.
