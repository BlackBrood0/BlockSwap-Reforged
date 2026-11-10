# Developer and Build Guide

This document covers the internal architecture of Block Swap Reforged, how to compile the project from source, and how to execute the automated test suites.

---

## 1. Prerequisites

To build and develop Block Swap Reforged, ensure you have:
- **Java 21 JDK**: Eclipse Adoptium JDK 21 or equivalent.
- **Git**: Installed and configured on your system.
- An IDE with Gradle and Lombok/Mixin support (IntelliJ IDEA recommended).

---

## 2. Building from Source

Clone the repository and compile using the included Gradle wrapper:

```bash
# 1. Clone repository
git clone https://github.com/BlackBrood0/BlockSwap-Reforged.git
cd BlockSwap-Reforged

# 2. Compile the mod JAR and execute tests
./gradlew build
```

The compiled JAR files will be output to:
```
build/libs/blockswap-neoforge-1.21.1-1.2-beta.jar
```

---

## 3. Running Automated Tests

Block Swap Reforged includes two layers of automated testing:

### A. JUnit Unit Tests
Fast, headless unit tests verifying JSON parsing, tag binding, property transfer, circular reference detection, and dimension resolution without launching a full game server:

```bash
./gradlew test
```
*Current test suite: 24/24 passing.*

### B. NeoForge GameTests
Runs a full headless Minecraft server instance that executes in-engine GameTests simulating player interactions, piston movements, dispenser deployments, dimension checks, and in-game `/blockswap reload` executions:

```bash
./gradlew runGameTestServer
```
*Current test suite: 20/20 passing.*

---

## 4. Run Configurations

ModDevGradle provides pre-configured run tasks:

- **`./gradlew runClient`**: Launches the Minecraft client with the mod loaded for interactive testing.
- **`./gradlew runServer`**: Launches a dedicated development server in the `run/` directory.
- **`./gradlew runGameTestServer`**: Executes the GameTest test suite.

---

## 5. Mod Architecture and Mixin Overview

Block Swap Reforged hooks into Minecraft's block placement pipeline using SpongePowered Mixins:

```
[Incoming Block Placement / Generation]
                 │
  ┌──────────────┴──────────────┐
  ▼                             ▼
[Live Level Placement]   [World Generation]
(Level.setBlock)         (WorldGenRegion / ChunkStep)
  │                             │
  ├─ Swapper.enterLivePlacement()├─ DimensionHolder.setDimension()
  ├─ Swapper.pushDimension()    ├─ Swapper.pushDimension()
  │                             │
  ▼                             ▼
MixinLevel                    MixinChunkStep / MixinWorldGenRegion
  │                             │
  └──────────────┬──────────────┘
                 ▼
      [LevelChunkSection.setBlockState]
                 │
                 ▼
         MixinChunkSection
                 │
                 ├─ Check live vs worldgen config
                 ├─ Resolve dimension from DimensionHolder / Swapper
                 ├─ Evaluate SwapRule applicability
                 ▼
         [Swapper.remapState]
                 │
                 ▼
     [Target BlockState Placed]
```

### Key Classes

- **`BlockSwapConfig`**:
  Manages configuration loading, JSON5 parsing, tag expansion, and dual configuration states (`WORLD_GEN_CONFIG` and `PLACEMENT_CONFIG`).
- **`SwapRule<T>`**:
  Encapsulates a replacement target (block or state) along with its allowed dimension set.
- **`Swapper`**:
  Core state remapping engine. Handles property transfer, thread-local live placement tracking, and dimension stack tracking (`pushDimension`, `popDimension`).
- **`DimensionHolder`**:
  Interface injected into `ChunkAccess` and `LevelChunkSection` providing persistent dimension context across asynchronous generation threads.

### Mixin Reference

| Mixin Class | Target Class | Purpose |
| :--- | :--- | :--- |
| `MixinChunkSection` | `LevelChunkSection` | Intercepts block state changes at the section level; routes between worldgen and placement configs |
| `MixinLevel` | `Level` | Intercepts `Level.setBlock`; tracks live placement depth and pushes level dimension |
| `MixinChunkAccess` | `ChunkAccess` | Implements `DimensionHolder` on chunks; propagates dimensions to sections |
| `MixinProtoChunk` | `ProtoChunk` | Ensures sections created during generation receive the chunk's dimension |
| `MixinLevelChunk` | `LevelChunk` | Tracks level dimension on live chunks |
| `MixinChunkStep` | `ChunkStep` | Injects dimension from `WorldGenContext` during chunk status generation tasks |
| `MixinWorldGenRegion` | `WorldGenRegion` | Tracks dimension during feature and structure generation |
| `MixinChunkSerializer` | `ChunkSerializer` | Performs raw NBT palette repairs from `missing_block_ids.json5` |
| `MixinChunkHolder` | `ChunkHolder` | Executes retroactive generation when `retro_gen` is enabled |

---

## 6. License and Attribution

- **Mod ID**: `blockswap`
- **License**: GNU Lesser General Public License v3.0 (LGPL-3.0)
- **Original Block Swap Creator**: CorgiTaco
- **Maintainer**: BlackBrood0
