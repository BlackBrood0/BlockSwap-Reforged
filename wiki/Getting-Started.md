# Getting Started with Block Swap Reforged

This page guides you through installing and setting up Block Swap Reforged on both singleplayer clients and dedicated multiplayer servers.

---

## Requirements

Before installing, ensure your environment meets the following specifications:

| Component | Required Version |
| :--- | :--- |
| **Minecraft** | `1.21.1` |
| **Mod Loader** | **NeoForge** `21.1.0` or newer (tested on `21.1.263+`) |
| **Java Runtime** | **Java 21** (OpenJDK / Eclipse Adoptium or equivalent) |
| **Dependencies** | None (Self-contained standalone mod) |

---

## Client Installation (Singleplayer)

1. **Install NeoForge**:
   - Download and run the official installer for NeoForge 1.21.1 from [neoforged.net](https://neoforged.net/).
   - Complete the installation in your Minecraft launcher.

2. **Download Block Swap Reforged**:
   - Grab the latest JAR file (e.g. `blockswap-neoforge-1.21.1-1.2-beta.jar`) from the [Releases](https://github.com/BlackBrood0/BlockSwap-Reforged/releases) page.

3. **Install the JAR**:
   - Navigate to your Minecraft game directory:
     - Windows: `%appdata%\.minecraft\mods`
     - macOS: `~/Library/Application Support/minecraft/mods`
     - Linux: `~/.minecraft/mods`
   - Place the downloaded JAR file into the `mods` folder.

4. **Launch the Game**:
   - Start Minecraft selecting the NeoForge 1.21.1 profile.
   - On the title screen, check the **Mods** menu: Block Swap should be listed with author `BlackBrood0`.

---

## Dedicated Server Installation

1. **Set Up Server**:
   - Install a standard NeoForge 1.21.1 dedicated server.

2. **Add Mod File**:
   - Copy `blockswap-neoforge-1.21.1-1.2-beta.jar` into the server's `mods/` directory.

3. **First Run**:
   - Start the server. Block Swap will automatically create the configuration folder and files if they do not exist:
     ```
     config/
       └── blockswap/
             ├── block_swap.json5
             └── missing_block_ids.json5
     ```

4. **Client Synchronization**:
   - When players join the dedicated server, the server automatically synchronizes active placement rules using custom network packets (`blockswap:config_sync`).
   - Players do not need to manually configure client config files on their side for server rules to take effect.

---

## File and Folder Overview

All mod configuration is stored inside `config/blockswap/`:

- **`config/blockswap/block_swap.json5`**:
  The primary configuration file. Controls block-to-block mappings, tag replacements, dimension conditions, state mappings, and natural generation mode.

- **`config/blockswap/missing_block_ids.json5`**:
  Secondary palette repair file. Used to remap missing block IDs in existing chunk NBT data.

- **`config/blockswap/known_states/`**:
  Generated reference folder created when `generate_all_known_states` is set to `true`. Contains JSON5 representations of all valid block states registered in the game.

---

## Next Steps

- Head to the **[Configuration Guide](Configuration-Guide.md)** to learn how to configure your first block swap.
- Check out **[Block and Tag Swapping](Block-and-Tag-Swapping.md)** to configure mass tag replacements.
- Read **[Dimension Conditions](Dimension-Conditions.md)** to restrict swaps to specific dimensions.
