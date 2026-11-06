# Feature Notes: Restrict Swapping to Natural Generation Only

## Overview
This document outlines the discovery findings and architectural design for adding the `onlySwapNaturalGeneration` configuration option to Block Swap on Minecraft 1.21.1 (NeoForge 21.1.x).

---

## Phase 1: Discovery & Architecture Analysis

### 1. Existing Swapping Hooks in Block Swap 1.21.1
Inspection of the codebase revealed two mixin injection points responsible for swapping blocks:

1. **`MixinLevel` (`net.minecraft.world.level.Level`)**:
   - Injects via `@ModifyVariable` into `setBlock(BlockPos, BlockState, int, int)` at `HEAD`.
   - Modifies the incoming `BlockState` argument before the block is written to the chunk.
   - Context: `Level` represents active world levels (`ServerLevel` on server, `ClientLevel` on client).
   - All live gameplay block placement enters through `Level.setBlock`:
     - Player block placement (`ServerGamePacketListenerImpl` / `ClientLevel`)
     - Pistons moving blocks (`PistonMovingBlockEntity`)
     - Falling blocks solidifying (`FallingBlockEntity`)
     - Fluids placing/updating blocks (`FlowingFluid`)
     - Bonemeal growth (`BoneMealItem`)
     - Dispensers placing blocks (`DispenseItemBehavior`)
     - Commands (`/setblock`, `/fill`)
     - Mob interactions (Enderman placing blocks)
   - World generation does **not** invoke `Level.setBlock`.

2. **`MixinChunkSection` (`net.minecraft.world.level.chunk.LevelChunkSection`)**:
   - Injects via `@ModifyVariable` into `setBlockState(int, int, int, BlockState, boolean)` at `HEAD`.
   - Context: `LevelChunkSection` is used during chunk generation (`NoiseBasedChunkGenerator`, `SurfaceSystem`, `OreFeature`, `ProtoChunk`, `WorldGenRegion`).
   - **Crucial Finding**: `LevelChunkSection.setBlockState` is a **shared hook**. When `Level.setBlock` is called during live gameplay, `Level` delegates to `LevelChunk.setBlockState`, which in turn delegates to `LevelChunkSection.setBlockState`.

### 2. The Shared Hook Challenge
If swapping were only disabled in `MixinLevel` during live placement:
1. `Level.setBlock` would retain the original block state.
2. `Level.setBlock` calls `LevelChunk.setBlockState`.
3. `LevelChunk.setBlockState` calls `LevelChunkSection.setBlockState`.
4. `MixinChunkSection` would swap the block state directly in the chunk storage palette.
5. In `Level.setBlock`, the post-placement check `if (blockstate1 == blockState)` would fail or cause block synchronization inconsistencies between client and server.

Therefore, live placement and world generation share `LevelChunkSection.setBlockState`, and `MixinChunkSection` must be aware of whether the call originated from a live `Level.setBlock` invocation.

---

## Phase 2: Design Decision & Implementation Strategy

### 1. Context Discrimination Mechanism
Because `Level.setBlock` is the exclusive entry point for live block changes, we distinguish live placement from world generation using a lightweight, thread-local depth tracker:

- In `Swapper.java`:
  ```java
  private static final ThreadLocal<int[]> LIVE_DEPTH = ThreadLocal.withInitial(() -> new int[1]);

  public static void enterLivePlacement() {
      LIVE_DEPTH.get()[0]++;
  }

  public static void exitLivePlacement() {
      int[] depth = LIVE_DEPTH.get();
      if (depth[0] > 0) {
          depth[0]--;
      }
  }

  public static boolean isLivePlacement() {
      return LIVE_DEPTH.get()[0] > 0;
  }
  ```
- In `MixinLevel.java`:
  - `@Inject(at = @At("HEAD"))`: If `onlySwapNaturalGeneration` is enabled, call `Swapper.enterLivePlacement()`.
  - `@Inject(at = @At("RETURN"))`: If `onlySwapNaturalGeneration` is enabled, call `Swapper.exitLivePlacement()`.
  - `@ModifyVariable`: If `onlySwapNaturalGeneration` is enabled, return `state` unchanged (skip remapping).
- In `MixinChunkSection.java`:
  - In `replaceState`: If `config.onlySwapNaturalGeneration() && Swapper.isLivePlacement()`, return `state` unchanged. Otherwise, remap the state.

### 2. Zero-Overhead Invariant When `onlySwapNaturalGeneration == false`
When the option is `false` (default):
- `config.onlySwapNaturalGeneration()` evaluates to `false`.
- In `MixinLevel`, neither `enterLivePlacement()` nor `exitLivePlacement()` is called. The `ThreadLocal` is never touched.
- In `MixinChunkSection`, `config.onlySwapNaturalGeneration()` short-circuits before `Swapper.isLivePlacement()` is evaluated.
- Overhead is strictly limited to reading a single cached boolean field in the config record, guaranteeing zero performance regression compared to baseline.

### 3. Configuration Format & Compatibility
- Config key: `"onlySwapNaturalGeneration"` (also accepts `"only_swap_natural_generation"` for resilience).
- Default: `false`.
- Existing configuration files lacking the key automatically deserialize with `false`. No migration file or manual update is required.
- Robust parsing: If the key has an invalid type (such as a string or number), a clear error is logged and the setting safely defaults to `false` without crashing the game or server.
- Reload behavior: The setting is read whenever `BlockSwapConfig.getConfig(true)` is called, taking effect on world load without requiring a client or server restart.
- Non-retroactive: Clarified in config template comments that chunks generated while the option is true contain swapped blocks, while previously generated chunks remain untouched.
