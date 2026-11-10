# State Swapper and Known States

While the standard `swapper` table handles general block-to-block and tag-to-block replacements, **`state_swapper`** allows you to define exact, state-level transformations with specific block properties.

---

## 1. What is a Block State?

In Minecraft, a single block can have multiple variations called **Block States**. For example:
- A stair block has `facing` (north, south, east, west), `half` (top, bottom), `shape` (straight, inner_left, etc.), and `waterlogged` (true, false).
- A log block has `axis` (x, y, z).
- A redstone lamp has `lit` (true, false).
- A fence block has connection booleans (`north`, `south`, `east`, `west`, `waterlogged`).

`state_swapper` lets you match on specific property combinations and replace them with precisely defined target states.

---

## 2. Configuration Syntax

In `config/blockswap/block_swap.json5`, `state_swapper` is an array of transformation objects:

```json5
{
  "state_swapper": [
    {
      "old": {
        "Name": "minecraft:oak_log",
        "Properties": {
          "axis": "z"
        }
      },
      "new": {
        "Name": "minecraft:birch_log",
        "Properties": {
          "axis": "x"
        }
      },
      "dimension": "minecraft:overworld"
    }
  ]
}
```

### Explanation
- **`old`**: Specifies the criteria for the incoming state.
  - `"Name"`: The source block identifier.
  - `"Properties"` (optional): A dictionary of property keys and values. If properties are omitted, matches any state of that block.
- **`new`**: Specifies the replacement state to produce.
  - `"Name"`: The target block identifier.
  - `"Properties"` (optional): The exact property values to apply on the target block.
- **`dimension`** (optional): Restricts the state transformation to a specific dimension.

---

## 3. Practical Use Cases

### Converting Horizontal Logs Only
Suppose you want vertical logs to remain oak, but any horizontal oak log oriented along the X axis to become an acacia log oriented along the Y axis:

```json5
{
  "state_swapper": [
    {
      "old": {
        "Name": "minecraft:oak_log",
        "Properties": {
          "axis": "x"
        }
      },
      "new": {
        "Name": "minecraft:acacia_log",
        "Properties": {
          "axis": "y"
        }
      }
    }
  ]
}
```

### Swapping Only Lit Redstone Lamps
Swap redstone lamps to glowstone only when they are turned on (`lit=true`):

```json5
{
  "state_swapper": [
    {
      "old": {
        "Name": "minecraft:redstone_lamp",
        "Properties": {
          "lit": "true"
        }
      },
      "new": {
        "Name": "minecraft:glowstone"
      }
    }
  ]
}
```

---

## 4. The `known_states/` Reference Directory

Knowing the exact names and allowed values of every property for every block in vanilla and modded Minecraft can be tricky. Block Swap Reforged automates this for you.

### Generating Known States

In `config/blockswap/block_swap.json5`, set:
```json5
{
  "generate_all_known_states": true
}
```

When Minecraft starts or loads a world, Block Swap Reforged scans the entire game registry and exports the complete property definitions for every block into:
```
config/
  └── blockswap/
        └── known_states/
              ├── minecraft/
              │     ├── oak_log.json5
              │     ├── stone_stairs.json5
              │     └── ...
              └── <other_mod_namespace>/
                    └── ...
```

### Inspecting a Known State File
For example, opening `config/blockswap/known_states/minecraft/oak_stairs.json5` reveals all valid property combinations:

```json5
[
  {
    "Name": "minecraft:oak_stairs",
    "Properties": {
      "facing": "north",
      "half": "bottom",
      "shape": "straight",
      "waterlogged": "false"
    }
  },
  ...
]
```
You can copy and paste directly from these reference files into your `state_swapper` configuration.
