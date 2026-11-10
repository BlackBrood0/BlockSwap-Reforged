# Commands and Hot Reload

Block Swap Reforged includes an in-game hot reload command that allows server administrators and players to update placement swap rules without restarting the server or disconnecting players.

---

## The `/blockswap reload` Command

### Syntax
```
/blockswap reload
```

### Permission Requirements
- **Singleplayer / LAN**: Always accessible (does not require cheats to be enabled).
- **Dedicated Server**: Requires **Permission Level 2** (OP / Operator).

---

## How It Works: Placement vs. World Generation Separation

When you execute `/blockswap reload`:

1. **Reloads Live Placement Conditions**:
   - The mod re-reads `config/blockswap/block_swap.json5` from disk.
   - It parses all new block mappings, tag mappings, and dimension conditions.
   - The updated rules immediately take effect for all subsequent in-game block placements.

2. **Preserves World Generation Conditions**:
   - The active world generation rules **remain unchanged** as they were loaded at world/server start.
   - **Why this design is important**: Changing world generation rules on a running world can create jarring chunk borders and mismatched terrain seams between adjacent chunks. Leaving world generation stable while allowing live placement adjustments provides the ideal balance between flexibility and terrain stability.

3. **Multiplayer Client Synchronization**:
   - The server packages the updated configuration into a `blockswap:config_sync` network payload.
   - The payload is broadcast to every connected client on the server.
   - Clients update their local prediction cache so that block placement animations and interactions feel seamless with zero desync.

4. **In-Game Chat Feedback**:
   - The player who issued the command receives a green confirmation message:
     ```
     [BlockSwap] Placement swap conditions reloaded. World generation conditions remain unchanged.
     ```

---

## Step-by-Step Example

1. Open `config/blockswap/block_swap.json5` while Minecraft is running.
2. Add a new swap entry:
   ```json5
   "swapper": {
     "minecraft:dirt": "minecraft:emerald_block"
   }
   ```
3. Save the file.
4. In Minecraft, run:
   ```
   /blockswap reload
   ```
5. Place a Dirt block in your world: it immediately turns into an Emerald Block.
6. Any new chunks that generate will continue using the world generation configuration established at world load.

---

## Relationship with Vanilla `/reload`

- Vanilla `/reload` reloads server datapacks, advancements, functions, and loot tables.
- When `/reload` runs, NeoForge fires `TagsUpdatedEvent` and `OnDatapackSyncEvent`.
- Block Swap Reforged listens to these events and automatically updates registered block tag mappings (`#c:ores`, `#minecraft:logs`, etc.) to match any changes made in the datapacks.
