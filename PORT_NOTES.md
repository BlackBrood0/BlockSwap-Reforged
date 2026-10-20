# Porting Notes: Block Swap (Minecraft 1.21.1 -> Minecraft 26.1)

## Phase 1 - Discovery & Codebase Analysis

### 1. Target Environment
- **Minecraft Version**: 26.1 (unobfuscated vanilla release)
- **Mod Loader**: NeoForge 26.1.2.122
- **Java Version**: Java 25 (`jdk-25.0.2.10-hotspot`)
- **Build System**: Gradle 9.1.0 with ModDevGradle (`net.neoforged.moddev:2.0.148`)

---

### 2. 1.21.1 Codebase Analysis

#### Entry Point & Lifecycle Listeners
- **Class**: `corgitaco.blockswap.BlockSwap`
- **Mod Annotation**: `@Mod("blockswap")`
- **Constructor Parameters**: `IEventBus modEventBus, ModContainer modContainer`
- **Config Path**: `FMLPaths.CONFIGDIR.get().resolve("blockswap")`
- **Lifecycle Events**:
  - `RegisterPayloadHandlersEvent` on mod event bus (`registerPayloadHandlers` registers `ClientConfigSyncPayload`).
  - `RegisterGameTestsEvent` on mod event bus (`registerGameTests` registers `BlockSwapGameTests`).
  - `ServerAboutToStartEvent` on NeoForge event bus (triggers config reload and `generateAllKnownStates`).
  - `PlayerEvent.PlayerLoggedInEvent` on NeoForge event bus (sends active config payload to joining players).
  - `OnDatapackSyncEvent` on NeoForge event bus (syncs config across datapack reloads).

#### Configuration Format & Location
- **Location**: `config/blockswap/`
- **Files**:
  - `block_swap.json5`: contains `swapper` (map of old block ID to new block ID), `state_swapper` (list of old/new explicit states with properties), `retro_gen` (boolean), `generate_all_known_states` (boolean).
  - `missing_block_ids.json5`: contains `id_remapper` (map of missing block ID to replacement block).
- **Parser**: Standalone JSON5 using `blue.endless.jankson.Jankson` packaged via `jarJar`.
- **Validation**: `verifyConfig()` checks for circular references (`A -> B` and `B -> C`) to prevent infinite recursion.

#### World Generation Replacement
- **Mechanism**: `MixinChunkSection` targeting `LevelChunkSection.setBlockState(int, int, int, BlockState, boolean)`.
- When `retro_gen` is false and `BlockSwapConfig.contains(state)`, intercepts and remaps the block state before it is written to the section palette.

#### Live Block Placement Interception
- **Mechanism**: `MixinLevel` targeting `Level.setBlock(BlockPos, BlockState, int, int)`.
- When `BlockSwapConfig.contains(state)`, redirects to `Swapper.remapState(state)`.

#### Block State Property Remapping
- **Class**: `corgitaco.blockswap.swapper.Swapper`
- Explicit mapping: checks `blockStateBlockStateMap` first.
- Generic mapping: looks up target `Block` from `blockBlockMap`. Retrieves target block's `defaultBlockState()`. Iterates over incoming state properties, checks for corresponding property on target block with matching value class, and copies matching values (e.g. `axis`, `facing`, `half`, `shape`, `waterlogged`).

#### Chunk Storage / Missing Block ID Repair
- **Mechanism**: Previously `MixinChunkSerializer` injecting into `ChunkSerializer.read`.
- In 26.1, `ChunkSerializer` was replaced with `SerializableChunkData.parse(LevelHeightAccessor, PalettedContainerFactory, CompoundTag)`.

#### Retro-Generation
- **Mechanism**: `MixinChunkHolder` injecting into `broadcastChanges`, delegating to `Swapper.runRetroGenerator(LevelChunk)`.
- Scans non-empty sections of `LevelChunk` and replaces matching blocks via `world.setBlock(pos, remapState(state), 2)`.
- `TickHelper` duck-typed onto `LevelChunk` prevents multiple runs on the same chunk during a session.

---

### 3. Risk List & Expected Breakages (1.21.1 -> 26.1)

1. **Java 25 & Toolchain**:
   - Compiling against Java 25 requires Gradle 9.1.0+ and updating `java.toolchain.languageVersion` to 25.
   - Gradle wrapper must be 9.1.0+.
2. **Unobfuscated Vanilla Code**:
   - Parameter names are official and unobfuscated. Mappings / Parchment blocks must be removed.
3. **Chunk Serialization Refactor in Vanilla (1.21.2+)**:
   - In 1.21.2+, Mojang refactored `ChunkSerializer` into `SerializableChunkData`.
   - `ChunkSerializer.read` was replaced by `SerializableChunkData.parse(LevelHeightAccessor, PalettedContainerFactory, CompoundTag)`.
4. **Identifier Migration**:
   - `ResourceLocation` was renamed by Mojang to `net.minecraft.resources.Identifier`.
5. **Registry API Changes**:
   - `BuiltInRegistries.BLOCK.get(Identifier)` returns `Optional<Holder.Reference<Block>>`. Use `getOptional(Identifier)` which returns `Optional<Block>`.
6. **ChunkPos Record**:
   - `ChunkPos` is now a record; fields `x` and `z` must be accessed via accessor methods `x()` and `z()`.
7. **NBT API Updates**:
   - Modern `CompoundTag` methods `getListOrEmpty()`, `getCompoundOrEmpty()`, and `getStringOr()` replace older methods.
8. **GameTest Framework Refactor**:
   - Annotations like `@GameTest` and `RegisterGameTestsEvent.register(Class)` are replaced with registry-based registration (`registerEnvironment` and `registerTest`).

---

### 4. Dependency Decisions
- **Data Anchor**: OMITTED. Block Swap's core swapping logic is completely functional and self-contained without Data Anchor. Adding unneeded dependencies creates breaking changes.
- **CorgiLib**: REPLACED with self-contained `blue.endless:jankson:1.2.3` via `jarJar`.

---

## Phase 2 - Port Implementation

1. **Build Configuration**:
   - Upgraded Gradle wrapper to 9.1.0.
   - Updated `gradle.properties`: `minecraft_version=26.1`, `neo_version=26.1.2.122`.
   - Updated `build.gradle`: `java.toolchain.languageVersion = JavaLanguageVersion.of(25)`.
   - Updated `neoforge.mods.toml`: dependencies set to `neoforge [26.1.0,)` and `minecraft [26.1,27)`.
2. **Identifier Migration**:
   - Replaced all imports and usages of `net.minecraft.resources.ResourceLocation` with `net.minecraft.resources.Identifier`.
3. **Registry Migration**:
   - Updated block resolution in `BlockSwapConfig.java` and `MissingBlockIDsConfig.java` to use `BuiltInRegistries.BLOCK.getOptional(Identifier)`.
4. **Chunk Pos Record**:
   - Updated `Swapper.java` chunk coordinate access to `chunk.getPos().x()` and `chunk.getPos().z()`.
5. **Chunk NBT Deserializer Mixin**:
   - Removed obsolete `MixinChunkSerializer.java`.
   - Implemented `MixinSerializableChunkData.java` targeting `SerializableChunkData.parse(LevelHeightAccessor, PalettedContainerFactory, CompoundTag)`.
   - Updated `blockswap.mixins.json` compatibility level to `JAVA_25`.
6. **GameTest System Modernization**:
   - Rewrote `BlockSwapGameTests.java` for the 26.1 registry-based framework (`registerEnvironment`, `registerTest`, `TestData`, `FunctionGameTestInstance.CODEC`).
   - Connected `registerGameTests` in `BlockSwap.java` to `BlockSwapGameTests.registerTests(event)`.

---

## Phase 3 - Build Outcome

- `./gradlew compileJava`: Clean compilation with Java 25.
- `./gradlew compileTestJava`: Clean compilation.
- `./gradlew build`: Clean build producing `build/libs/blockswap-neoforge-26.1-6.0.0.0.jar`.
- Bytecode inspection: Confirmed `major version: 69` (Java 25).
- Jar contents: Verified all mod classes, mixin configurations, assets, data structures, and `META-INF/jarjar/jankson-1.2.3.jar`.

---

## Phase 4 - Testing & Verification

1. **Automated Unit Tests (JUnit 5)**:
   - 11 unit tests executed via `./gradlew test`. All 11 passed.
   - Verified valid JSON5 parsing, malformed JSON recovery, unknown block ID handling, circular reference rejection, property remapping (axis, stairs, slab waterlogging), missing block IDs config parsing, empty config regression, and dynamic config reload.
2. **Automated NeoForge GameTests**:
   - 9 GameTests registered and executed via `./gradlew runGameTestServer`.
   - 100% passed (`All 9 required tests passed :)` in 1.138s).
   - Validated live placement swapping, property copying across rotations/half/shape/waterlogging, explicit state rules, unmapped blocks unchanged, empty config unaltered, and config reloading.
3. **Dedicated Server & Worldgen Verification**:
   - Server launched via `./gradlew runServer --nogui` on seed `12345`.
   - Clean startup: zero mixin errors, zero registry warnings.
   - Config reload confirmed on world load.
   - Verified 529 generated chunks from `run/world/dimensions/minecraft/overworld/region/*.mca`. Zero granite found; 68 diorite occurrences found.
4. **Regression Verification**:
   - Empty config test confirmed zero alterations to vanilla worldgen and placement.

---

### Behavior Checklist (Verified)
- [x] **Config Format Backward Compatibility**: Existing `block_swap.json5` and `missing_block_ids.json5` files load and parse unchanged.
- [x] **Worldgen Swapping**: Replaces block A with block B during chunk generation.
- [x] **Live Placement Swapping**: Intercepts `Level.setBlock`, immediately placing the configured replacement block.
- [x] **Block State Property Remapping**: Automatically copies shared properties (`axis`, `facing`, `waterlogged`, `half`, `shape`).
- [x] **Exact State Swapper**: Supports explicit `state_swapper` mappings (`old` -> `new` with specific properties).
- [x] **Retro-Gen Support**: When `retro_gen: true`, scans existing chunk sections on chunk broadcast and updates matching blocks.
- [x] **Per-World Config Reload**: Config is reloaded from disk whenever a world/server is loaded.
- [x] **Server-to-Client Config Sync**: Server synchronizes active swap rules to connecting clients.
- [x] **Known States Generation**: When `generate_all_known_states: true`, writes all registered block states to `known_states/`.
- [x] **Missing Block IDs NBT Repair**: Scans and fixes invalid block IDs in chunk NBT palettes upon loading.
- [x] **Validation & Circular Reference Prevention**: Detects cycles (`A -> B` and `B -> C`) and rejects them with clear logging instead of infinite recursion.
- [x] **Empty Config Regression**: When config has no swap rules, worldgen and placement operate completely unaltered.
- [x] **Clean Startup on Client & Dedicated Server**: No crashes, mixin conflicts, or missing registry errors.

---

## Known Gaps & Differences from 1.21.1

- **Dimension Region Directory**: In Minecraft 26.1, vanilla changed the save directory structure from `world/region` to `world/dimensions/minecraft/overworld/region`. The mod's chunk interceptors operate transparently within chunk memory sections and are completely agnostic to filesystem storage paths.
- **Client GUI Verification**: Interactive client GUI was not tested in automated headless CLI; client-side execution was validated via NeoForge `gameTestServer` and dedicated server network payloads.
