# Dimension Conditions

The **Dimension Condition** feature allows you to restrict block replacements to specific dimensions. When a swap has a dimension condition assigned, it **only** executes in the designated dimension(s) and cannot happen anywhere else.

---

## 1. Global Dimension Condition (Whole Mod)

If you want all block swaps in the configuration file to apply only to specific dimensions, add the `"dimension"` or `"dimensions"` field at the root level of `config/blockswap/block_swap.json5`.

### Single Dimension Example

```json5
{
  // Restricts all swaps in this file to the Nether only
  "dimension": "minecraft:the_nether",

  "swapper": {
    "minecraft:netherrack": "minecraft:gold_block",
    "minecraft:gravel": "minecraft:glowstone"
  }
}
```
**Outcome**:
- In the Nether (`minecraft:the_nether`): Netherrack turns to gold blocks, and gravel turns to glowstone.
- In the Overworld or The End: No blocks in this configuration will be swapped.

### Multiple Dimensions Array Example

```json5
{
  // Active in both the Overworld and The End
  "dimensions": ["minecraft:overworld", "minecraft:the_end"],

  "swapper": {
    "minecraft:dirt": "minecraft:sponge"
  }
}
```

---

## 2. Per-Swap Dimension Condition

You can assign dimension conditions to individual swap entries. Per-swap conditions are specified by writing the value as an object instead of a simple string.

### Syntax

```json5
{
  "swapper": {
    // 1. Overworld only swap
    "minecraft:dirt": {
      "replaceWith": "minecraft:diamond_block",
      "dimension": "minecraft:overworld"
    },

    // 2. Nether only swap
    "minecraft:netherrack": {
      "replaceWith": "minecraft:gold_block",
      "dimension": "minecraft:the_nether"
    },

    // 3. Unrestricted swap (applies to ALL dimensions)
    "minecraft:gravel": "minecraft:sand"
  }
}
```

### Supported Target Key Aliases

When defining an object entry in `"swapper"`, you can use any of the following keys for the replacement block:
- `replaceWith` / `replace_with`
- `target`
- `to`
- `replacement`
- `new`
- `block`

### Supported Dimension Key Aliases

You can use any of the following keys for the dimension condition:
- `dimension`
- `dimensions` (supports an array of strings)
- `dimension_condition`
- `dim` / `dims`

---

## 3. Per-Swap Multi-Dimension Condition

An individual swap can be configured to execute in multiple selected dimensions:

```json5
{
  "swapper": {
    "minecraft:dirt": {
      "replaceWith": "minecraft:sponge",
      "dimensions": ["minecraft:overworld", "minecraft:the_end"]
    }
  }
}
```
In this example, Dirt swaps to Sponge in the Overworld and The End, but remains ordinary Dirt in the Nether.

---

## 4. Tag Replacement with Dimension Conditions

Dimension conditions work seamlessly with block tags:

```json5
{
  "swapper": {
    // All ores generate and place as infested cobblestone ONLY in the Overworld
    "#c:ores": {
      "replaceWith": "minecraft:infested_cobblestone",
      "dimension": "minecraft:overworld"
    }
  }
}
```
In this scenario:
- Overworld ores are swapped to infested cobblestone.
- Nether quartz ore and nether gold ore remain standard nether ores.
- End ores (from other mods) remain untouched.

---

## 5. Per-Swap Overriding Global Dimensions

Per-swap dimension conditions always **override** the global root dimension:

```json5
{
  // Global default: Overworld
  "dimension": "minecraft:overworld",

  "swapper": {
    // Inherits global condition (Overworld only)
    "minecraft:dirt": "minecraft:sponge",

    // Overrides global condition (Nether only)
    "minecraft:netherrack": {
      "replaceWith": "minecraft:gold_block",
      "dimension": "minecraft:the_nether"
    }
  }
}
```

---

## 6. Dimension in `state_swapper`

You can also apply dimension conditions to explicit block state transformations:

```json5
{
  "state_swapper": [
    {
      "old": {
        "Name": "minecraft:oak_log",
        "Properties": { "axis": "y" }
      },
      "new": {
        "Name": "minecraft:birch_log",
        "Properties": { "axis": "y" }
      },
      "dimension": "minecraft:overworld"
    }
  ]
}
```

---

## 7. Dimension ID Format and Custom Dimensions

- **Namespace Auto-Resolution**: If you omit `minecraft:`, the mod automatically resolves vanilla dimensions:
  - `"overworld"` -> `minecraft:overworld`
  - `"the_nether"` -> `minecraft:the_nether`
  - `"the_end"` -> `minecraft:the_end`
- **Custom Mod Dimensions**: Fully supported. Simply enter the full resource location of the dimension (e.g. `"twilightforest:twilight_forest"`, `"ad_astra:moon"`, or `"aether:the_aether"`).
