/*
 * Copyright (c) 2026 CorgiTaco
 *
 * This file is part of Block Swap, licensed under the GNU Lesser General Public License v3.0 (LGPL-3.0).
 * You should have received a copy of the LGPL-3.0 along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.html>.
 */

package corgitaco.blockswap.config;

import blue.endless.jankson.Jankson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import corgitaco.blockswap.BlockSwap;
import corgitaco.blockswap.swapper.Swapper;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public record BlockSwapConfig(
        Map<Block, Block> blockBlockMap,
        Map<BlockState, BlockState> blockStateBlockStateMap,
        Map<TagKey<Block>, Block> tagBlockMap,
        Map<Block, List<SwapRule<Block>>> blockRules,
        Map<BlockState, List<SwapRule<BlockState>>> stateRules,
        Map<TagKey<Block>, List<SwapRule<Block>>> tagRules,
        Set<ResourceLocation> globalDimensions,
        boolean retroGen,
        boolean generateAllKnownStates,
        boolean onlySwapNaturalGeneration
) {
    public static final BlockSwapConfig DEFAULT = new BlockSwapConfig(
            new IdentityHashMap<>(),
            new IdentityHashMap<>(),
            Collections.emptyMap(),
            new IdentityHashMap<>(),
            new IdentityHashMap<>(),
            Collections.emptyMap(),
            Collections.emptySet(),
            false,
            true,
            false
    );

    // Convenience constructor for backward compatibility
    public BlockSwapConfig(
            Map<Block, Block> blockBlockMap,
            Map<BlockState, BlockState> blockStateBlockStateMap,
            Map<TagKey<Block>, Block> tagBlockMap,
            boolean retroGen,
            boolean generateAllKnownStates,
            boolean onlySwapNaturalGeneration
    ) {
        this(
                blockBlockMap,
                blockStateBlockStateMap,
                tagBlockMap,
                wrapBlockRules(blockBlockMap),
                wrapStateRules(blockStateBlockStateMap),
                wrapTagRules(tagBlockMap),
                Collections.emptySet(),
                retroGen,
                generateAllKnownStates,
                onlySwapNaturalGeneration
        );
    }

    public BlockSwapConfig(
            Map<Block, Block> blockBlockMap,
            Map<BlockState, BlockState> blockStateBlockStateMap,
            boolean retroGen,
            boolean generateAllKnownStates,
            boolean onlySwapNaturalGeneration
    ) {
        this(blockBlockMap, blockStateBlockStateMap, Collections.emptyMap(), retroGen, generateAllKnownStates, onlySwapNaturalGeneration);
    }

    public BlockSwapConfig(
            Map<Block, Block> blockBlockMap,
            Map<BlockState, BlockState> blockStateBlockStateMap,
            boolean retroGen,
            boolean generateAllKnownStates
    ) {
        this(blockBlockMap, blockStateBlockStateMap, Collections.emptyMap(), retroGen, generateAllKnownStates, false);
    }

    private static Map<Block, List<SwapRule<Block>>> wrapBlockRules(Map<Block, Block> map) {
        Map<Block, List<SwapRule<Block>>> rules = new IdentityHashMap<>();
        for (Map.Entry<Block, Block> entry : map.entrySet()) {
            rules.put(entry.getKey(), List.of(new SwapRule<>(entry.getValue())));
        }
        return rules;
    }

    private static Map<BlockState, List<SwapRule<BlockState>>> wrapStateRules(Map<BlockState, BlockState> map) {
        Map<BlockState, List<SwapRule<BlockState>>> rules = new IdentityHashMap<>();
        for (Map.Entry<BlockState, BlockState> entry : map.entrySet()) {
            rules.put(entry.getKey(), List.of(new SwapRule<>(entry.getValue())));
        }
        return rules;
    }

    private static Map<TagKey<Block>, List<SwapRule<Block>>> wrapTagRules(Map<TagKey<Block>, Block> map) {
        Map<TagKey<Block>, List<SwapRule<Block>>> rules = new LinkedHashMap<>();
        for (Map.Entry<TagKey<Block>, Block> entry : map.entrySet()) {
            rules.put(entry.getKey(), List.of(new SwapRule<>(entry.getValue())));
        }
        return rules;
    }

    private static final Jankson JANKSON = Jankson.builder().build();
    private static BlockSwapConfig WORLD_GEN_CONFIG = null;
    private static BlockSwapConfig PLACEMENT_CONFIG = null;

    private static final String DEFAULT_CONFIG_TEMPLATE = """
            {
            	// Optional global dimension condition. When specified, block swaps only apply in this dimension.
            	// Can be a single dimension string or an array of dimensions.
            	// Examples:
            	// "dimension": "minecraft:overworld",
            	// "dimensions": ["minecraft:overworld", "minecraft:the_nether"],

            	// A map of blocks and tags that specifies what the "old" block/tag is and what its "new" block is.
            	// Entries can be simple strings or objects specifying per-swap dimension conditions.
            	// Examples:
            	// "swapper": {
            	// 	"#c:ores": "minecraft:infested_cobblestone",
            	// 	"minecraft:coarse_dirt": "minecraft:dirt",
            	// 	"minecraft:stone": {
            	// 		"replaceWith": "minecraft:granite",
            	// 		"dimension": "minecraft:the_nether"
            	// 	}
            	// }
            	"swapper": {},

            	// A map of states that specifies what the "old" block state is and what its "new" block state is.
            	// See "known_states" folder ("generate_all_known_states" must be set to true in this config) to see all known block states.
            	// Example:
            	// "state_swapper": [
            	// 	{
            	// 		"new": {
            	// 			"Name": "minecraft:birch_log",
            	// 			"Properties": {
            	// 				"axis": "x"
            	// 			}
            	// 		},
            	// 		"old": {
            	// 			"Name": "minecraft:oak_log",
            	// 			"Properties": {
            	// 				"axis": "z"
            	// 			}
            	// 		},
            	// 		"dimension": "minecraft:overworld"
            	// 	}
            	// ]
            	"state_swapper": [],

            	// Whether blocks are replaced in existing chunks.
            	"retro_gen": false,

            	// Generates all block states for all blocks in the registry.
            	"generate_all_known_states": true,

            	// When true, swaps apply ONLY during world generation (terrain, features, structures).
            	// Live block placement (players, pistons, dispensers, falling blocks, fluids, commands) will NOT be swapped.
            	// Chunks generated while this option is true contain swapped blocks; chunks generated before stay as they are.
            	// Does not retroactively change existing chunks.
            	"onlySwapNaturalGeneration": false
            }
            """;

    public Block getTargetBlock(Block block, ResourceLocation dimension) {
        List<SwapRule<Block>> rules = blockRules.get(block);
        if (rules != null) {
            for (SwapRule<Block> rule : rules) {
                if (rule.appliesTo(dimension)) {
                    return rule.target();
                }
            }
        }
        return null;
    }

    public BlockState getTargetState(BlockState state, ResourceLocation dimension) {
        List<SwapRule<BlockState>> rules = stateRules.get(state);
        if (rules != null) {
            for (SwapRule<BlockState> rule : rules) {
                if (rule.appliesTo(dimension)) {
                    return rule.target();
                }
            }
        }
        return null;
    }

    public boolean contains(BlockState state, ResourceLocation dimension) {
        if (getTargetState(state, dimension) != null) {
            return true;
        }
        return getTargetBlock(state.getBlock(), dimension) != null;
    }

    public boolean contains(BlockState state) {
        return contains(state, Swapper.getCurrentDimension());
    }

    public BlockSwapConfig resolveTags() {
        if (tagRules.isEmpty()) {
            return this;
        }

        Map<Block, List<SwapRule<Block>>> updatedBlockRules = new IdentityHashMap<>();
        Map<Block, Block> updatedBlockMap = new IdentityHashMap<>();

        // 1. Expand tag rules
        for (Map.Entry<TagKey<Block>, List<SwapRule<Block>>> entry : tagRules.entrySet()) {
            var tagSet = BuiltInRegistries.BLOCK.getTag(entry.getKey());
            if (tagSet.isPresent()) {
                for (Holder<Block> holder : tagSet.get()) {
                    Block block = holder.value();
                    List<SwapRule<Block>> list = updatedBlockRules.computeIfAbsent(block, k -> new ArrayList<>());
                    list.addAll(entry.getValue());
                    if (!entry.getValue().isEmpty()) {
                        updatedBlockMap.put(block, entry.getValue().get(0).target());
                    }
                }
            } else {
                BlockSwap.LOGGER.debug("Tag '{}' in 'swapper' is not yet bound or contains no blocks.", entry.getKey().location());
            }
        }

        // 2. Direct block mappings overwrite tag mappings
        for (Map.Entry<Block, List<SwapRule<Block>>> entry : blockRules.entrySet()) {
            updatedBlockRules.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            if (!entry.getValue().isEmpty()) {
                updatedBlockMap.put(entry.getKey(), entry.getValue().get(0).target());
            }
        }

        // 3. Verify circular references per block
        Map<Block, List<SwapRule<Block>>> verifiedBlockRules = new IdentityHashMap<>();
        Map<Block, Block> verifiedBlockMap = new IdentityHashMap<>();

        for (Map.Entry<Block, List<SwapRule<Block>>> entry : updatedBlockRules.entrySet()) {
            Block sourceBlock = entry.getKey();
            List<SwapRule<Block>> validRules = new ArrayList<>();

            for (SwapRule<Block> rule : entry.getValue()) {
                Block targetBlock = rule.target();
                if (updatedBlockRules.containsKey(targetBlock)) {
                    BlockSwap.LOGGER.error("Detected circular BLOCK reference in 'swapper'! Block '{}' maps to '{}' which is also swapped.",
                            BuiltInRegistries.BLOCK.getKey(sourceBlock),
                            BuiltInRegistries.BLOCK.getKey(targetBlock));
                } else {
                    validRules.add(rule);
                }
            }

            if (!validRules.isEmpty()) {
                verifiedBlockRules.put(sourceBlock, validRules);
                verifiedBlockMap.put(sourceBlock, validRules.get(0).target());
            }
        }

        return new BlockSwapConfig(
                verifiedBlockMap,
                blockStateBlockStateMap,
                tagBlockMap,
                verifiedBlockRules,
                stateRules,
                tagRules,
                globalDimensions,
                retroGen,
                generateAllKnownStates,
                onlySwapNaturalGeneration
        );
    }

    public static BlockSwapConfig getWorldGenConfig() {
        if (WORLD_GEN_CONFIG == null) {
            getConfig(true);
        }
        return WORLD_GEN_CONFIG;
    }

    public static BlockSwapConfig getPlacementConfig() {
        if (PLACEMENT_CONFIG == null) {
            getConfig(true);
        }
        return PLACEMENT_CONFIG;
    }

    public static void setWorldGenConfig(BlockSwapConfig config) {
        WORLD_GEN_CONFIG = config;
    }

    public static void setPlacementConfig(BlockSwapConfig config) {
        PLACEMENT_CONFIG = config;
    }

    public static void reloadPlacementConfig() {
        Path path = BlockSwap.CONFIG_PATH.resolve("block_swap.json5");
        BlockSwapConfig reloaded = loadFromFile(path).resolveTags();
        PLACEMENT_CONFIG = reloaded;
        BlockSwap.LOGGER.info("Reloaded BlockSwap placement configuration: {} block swaps active, onlySwapNaturalGeneration = {}",
                reloaded.blockRules().size(), reloaded.onlySwapNaturalGeneration());
    }

    public static BlockSwapConfig getConfig(BlockSwapConfig serverConfig) {
        PLACEMENT_CONFIG = serverConfig;
        WORLD_GEN_CONFIG = serverConfig;
        return PLACEMENT_CONFIG;
    }

    public static BlockSwapConfig getConfig(boolean reload) {
        if (PLACEMENT_CONFIG == null || reload) {
            Path path = BlockSwap.CONFIG_PATH.resolve("block_swap.json5");
            File configFile = path.toFile();

            if (!configFile.exists()) {
                try {
                    Files.createDirectories(path.getParent());
                    Files.writeString(path, DEFAULT_CONFIG_TEMPLATE);
                } catch (IOException e) {
                    BlockSwap.LOGGER.error("Failed to create default config at {}", path, e);
                }
                PLACEMENT_CONFIG = DEFAULT;
                if (WORLD_GEN_CONFIG == null || reload) {
                    WORLD_GEN_CONFIG = DEFAULT;
                }
                return PLACEMENT_CONFIG;
            }

            BlockSwapConfig loaded = loadFromFile(path);
            PLACEMENT_CONFIG = loaded;
            if (WORLD_GEN_CONFIG == null || reload) {
                WORLD_GEN_CONFIG = loaded;
            }
        }
        return PLACEMENT_CONFIG;
    }

    public static BlockSwapConfig loadFromFile(Path path) {
        try {
            blue.endless.jankson.JsonObject janksonObj = JANKSON.load(path.toFile());
            String standardJson = janksonObj.toJson(false, false);
            return parseJsonString(standardJson);
        } catch (Exception e) {
            BlockSwap.LOGGER.error("Failed to parse config file at {}, using default config", path, e);
            return DEFAULT;
        }
    }

    public static BlockSwapConfig parseJsonString(String jsonString) {
        try {
            String standardJson;
            try {
                blue.endless.jankson.JsonObject janksonObj = JANKSON.load(jsonString);
                standardJson = janksonObj.toJson(false, false);
            } catch (Exception ignored) {
                standardJson = jsonString;
            }

            JsonElement rootElement = JsonParser.parseString(standardJson);
            if (!rootElement.isJsonObject()) {
                BlockSwap.LOGGER.error("Config root is not a JSON object, falling back to default");
                return DEFAULT;
            }

            JsonObject root = rootElement.getAsJsonObject();

            // 1. Parse global dimensions
            Set<ResourceLocation> globalDimensions = parseDimensions(root);

            // 2. Parse swapper entries
            Map<Block, List<SwapRule<Block>>> rawBlockRules = new IdentityHashMap<>();
            Map<Block, Block> rawBlockMap = new IdentityHashMap<>();
            Map<TagKey<Block>, List<SwapRule<Block>>> rawTagRules = new LinkedHashMap<>();
            Map<TagKey<Block>, Block> rawTagMap = new LinkedHashMap<>();

            if (root.has("swapper") && root.get("swapper").isJsonObject()) {
                JsonObject swapperObj = root.getAsJsonObject("swapper");

                for (Map.Entry<String, JsonElement> entry : swapperObj.entrySet()) {
                    String oldKey = entry.getKey().trim();
                    List<SwapRule<Block>> parsedRules = parseBlockRules(entry.getValue(), globalDimensions);

                    if (parsedRules.isEmpty()) {
                        continue;
                    }

                    if (oldKey.startsWith("#")) {
                        String tagString = oldKey.substring(1).trim();
                        try {
                            ResourceLocation tagRl = ResourceLocation.parse(tagString);
                            TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, tagRl);
                            rawTagRules.put(tagKey, parsedRules);
                            rawTagMap.put(tagKey, parsedRules.get(0).target());
                        } catch (Exception e) {
                            BlockSwap.LOGGER.error("Invalid tag identifier '{}' in 'swapper': {}", oldKey, e.getMessage());
                        }
                    } else {
                        Optional<Block> oldBlock = parseBlock(oldKey);
                        if (oldBlock.isEmpty()) {
                            BlockSwap.LOGGER.error("Unknown block '{}' in 'swapper' old block entry, skipping", oldKey);
                            continue;
                        }
                        rawBlockRules.put(oldBlock.get(), parsedRules);
                        rawBlockMap.put(oldBlock.get(), parsedRules.get(0).target());
                    }
                }

                // Expand tags if already bound in registry
                for (Map.Entry<TagKey<Block>, List<SwapRule<Block>>> tagEntry : rawTagRules.entrySet()) {
                    var tagSet = BuiltInRegistries.BLOCK.getTag(tagEntry.getKey());
                    if (tagSet.isPresent()) {
                        for (Holder<Block> holder : tagSet.get()) {
                            Block block = holder.value();
                            rawBlockRules.computeIfAbsent(block, k -> new ArrayList<>()).addAll(tagEntry.getValue());
                            if (!tagEntry.getValue().isEmpty()) {
                                rawBlockMap.put(block, tagEntry.getValue().get(0).target());
                            }
                        }
                    }
                }
                // Direct blocks overwrite tag blocks
                for (Map.Entry<Block, List<SwapRule<Block>>> directEntry : rawBlockRules.entrySet()) {
                    if (!directEntry.getValue().isEmpty()) {
                        rawBlockMap.put(directEntry.getKey(), directEntry.getValue().get(0).target());
                    }
                }
            }

            // 3. Parse state_swapper entries
            Map<BlockState, List<SwapRule<BlockState>>> rawStateRules = new IdentityHashMap<>();
            Map<BlockState, BlockState> rawStateMap = new IdentityHashMap<>();

            if (root.has("state_swapper") && root.get("state_swapper").isJsonArray()) {
                JsonArray stateArray = root.getAsJsonArray("state_swapper");
                for (JsonElement element : stateArray) {
                    if (!element.isJsonObject()) {
                        BlockSwap.LOGGER.error("Invalid entry in 'state_swapper': expected object");
                        continue;
                    }
                    JsonObject pairObj = element.getAsJsonObject();
                    if (!pairObj.has("old") || !pairObj.has("new")) {
                        BlockSwap.LOGGER.error("Entry in 'state_swapper' missing 'old' or 'new' field: {}", pairObj);
                        continue;
                    }

                    Optional<BlockState> oldState = BlockState.CODEC.parse(JsonOps.INSTANCE, pairObj.get("old"))
                            .resultOrPartial(err -> BlockSwap.LOGGER.error("Error parsing 'old' state in state_swapper: {}", err));
                    Optional<BlockState> newState = BlockState.CODEC.parse(JsonOps.INSTANCE, pairObj.get("new"))
                            .resultOrPartial(err -> BlockSwap.LOGGER.error("Error parsing 'new' state in state_swapper: {}", err));

                    if (oldState.isPresent() && newState.isPresent()) {
                        Set<ResourceLocation> dims = parseDimensions(pairObj);
                        if (dims.isEmpty()) {
                            dims = globalDimensions;
                        }
                        SwapRule<BlockState> rule = new SwapRule<>(newState.get(), dims);
                        rawStateRules.computeIfAbsent(oldState.get(), k -> new ArrayList<>()).add(rule);
                        rawStateMap.put(oldState.get(), newState.get());
                    }
                }
            }

            boolean retroGen = parseBoolean(root, "retro_gen", false);
            boolean generateAllKnownStates = parseBoolean(root, "generate_all_known_states", true);
            boolean onlySwapNaturalGeneration = false;
            for (String key : root.keySet()) {
                String normalized = key.replace("_", "").replace("-", "").toLowerCase();
                if (normalized.equals("onlyswapnaturalgeneration")
                        || normalized.equals("onlynaturalgeneration")
                        || normalized.equals("onlynaturalgen")
                        || normalized.equals("naturalonly")
                        || normalized.equals("naturalgenerationonly")) {
                    onlySwapNaturalGeneration = parseBoolean(root, key, false);
                    break;
                }
            }

            // Verify circular references
            Map<Block, List<SwapRule<Block>>> verifiedBlockRules = new IdentityHashMap<>();
            Map<Block, Block> verifiedBlockMap = new IdentityHashMap<>();

            for (Map.Entry<Block, List<SwapRule<Block>>> entry : rawBlockRules.entrySet()) {
                Block src = entry.getKey();
                List<SwapRule<Block>> validRules = new ArrayList<>();
                for (SwapRule<Block> rule : entry.getValue()) {
                    if (rawBlockRules.containsKey(rule.target())) {
                        BlockSwap.LOGGER.error("Detected circular BLOCK reference in 'swapper'! Block '{}' maps to '{}' which is also swapped.",
                                BuiltInRegistries.BLOCK.getKey(src),
                                BuiltInRegistries.BLOCK.getKey(rule.target()));
                    } else {
                        validRules.add(rule);
                    }
                }
                if (!validRules.isEmpty()) {
                    verifiedBlockRules.put(src, validRules);
                    verifiedBlockMap.put(src, validRules.get(0).target());
                }
            }

            Map<BlockState, List<SwapRule<BlockState>>> verifiedStateRules = new IdentityHashMap<>();
            Map<BlockState, BlockState> verifiedStateMap = new IdentityHashMap<>();

            for (Map.Entry<BlockState, List<SwapRule<BlockState>>> entry : rawStateRules.entrySet()) {
                BlockState src = entry.getKey();
                List<SwapRule<BlockState>> validRules = new ArrayList<>();
                for (SwapRule<BlockState> rule : entry.getValue()) {
                    if (rawStateRules.containsKey(rule.target())) {
                        BlockSwap.LOGGER.error("Detected circular BLOCKSTATE reference in 'state_swapper'! State '{}' is also swapped into.",
                                src);
                    } else {
                        validRules.add(rule);
                    }
                }
                if (!validRules.isEmpty()) {
                    verifiedStateRules.put(src, validRules);
                    verifiedStateMap.put(src, validRules.get(0).target());
                }
            }

            return new BlockSwapConfig(
                    verifiedBlockMap,
                    verifiedStateMap,
                    rawTagMap,
                    verifiedBlockRules,
                    verifiedStateRules,
                    rawTagRules,
                    globalDimensions,
                    retroGen,
                    generateAllKnownStates,
                    onlySwapNaturalGeneration
            );
        } catch (Exception e) {
            BlockSwap.LOGGER.error("Failed to parse config json string, using default", e);
            return DEFAULT;
        }
    }

    private static List<SwapRule<Block>> parseBlockRules(JsonElement element, Set<ResourceLocation> globalDims) {
        List<SwapRule<Block>> rules = new ArrayList<>();

        if (element.isJsonPrimitive()) {
            String targetId = element.getAsString().trim();
            Optional<Block> targetBlock = parseBlock(targetId);
            if (targetBlock.isPresent()) {
                rules.add(new SwapRule<>(targetBlock.get(), globalDims));
            } else {
                BlockSwap.LOGGER.error("Unknown target block '{}' in 'swapper' entry, skipping", targetId);
            }
        } else if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            Optional<Block> targetBlock = findTargetBlock(obj);
            if (targetBlock.isPresent()) {
                Set<ResourceLocation> dims = parseDimensions(obj);
                if (dims.isEmpty()) {
                    dims = globalDims;
                }
                rules.add(new SwapRule<>(targetBlock.get(), dims));
            }
        } else if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            for (JsonElement item : arr) {
                rules.addAll(parseBlockRules(item, globalDims));
            }
        }

        return rules;
    }

    private static Optional<Block> findTargetBlock(JsonObject obj) {
        String[] targetKeys = {"replaceWith", "replace_with", "target", "to", "replacement", "new", "block"};
        for (String key : targetKeys) {
            if (obj.has(key) && obj.get(key).isJsonPrimitive()) {
                String id = obj.get(key).getAsString().trim();
                Optional<Block> b = parseBlock(id);
                if (b.isPresent()) {
                    return b;
                } else {
                    BlockSwap.LOGGER.error("Unknown target block '{}' in 'swapper' object, skipping", id);
                    return Optional.empty();
                }
            }
        }
        BlockSwap.LOGGER.error("Missing target block in 'swapper' object entry: {}", obj);
        return Optional.empty();
    }

    public static Set<ResourceLocation> parseDimensions(JsonObject obj) {
        Set<ResourceLocation> dims = new HashSet<>();
        String[] dimKeys = {"dimension", "dimensions", "dimension_condition", "dimensionCondition", "dim", "dims"};

        for (String key : dimKeys) {
            if (obj.has(key)) {
                JsonElement elem = obj.get(key);
                if (elem.isJsonPrimitive()) {
                    ResourceLocation dim = parseDimension(elem.getAsString());
                    if (dim != null) {
                        dims.add(dim);
                    }
                } else if (elem.isJsonArray()) {
                    for (JsonElement e : elem.getAsJsonArray()) {
                        if (e.isJsonPrimitive()) {
                            ResourceLocation dim = parseDimension(e.getAsString());
                            if (dim != null) {
                                dims.add(dim);
                            }
                        }
                    }
                }
                break;
            }
        }
        return dims.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(dims);
    }

    public static ResourceLocation parseDimension(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim().toLowerCase();
        if (trimmed.isEmpty() || trimmed.equals("*") || trimmed.equals("all")) {
            return null;
        }
        if (!trimmed.contains(":")) {
            trimmed = "minecraft:" + trimmed;
        }
        try {
            return ResourceLocation.parse(trimmed);
        } catch (Exception e) {
            BlockSwap.LOGGER.error("Invalid dimension identifier '{}'", raw, e);
            return null;
        }
    }

    private static boolean parseBoolean(JsonObject root, String key, boolean defaultValue) {
        if (!root.has(key)) {
            return defaultValue;
        }
        JsonElement elem = root.get(key);
        if (elem.isJsonPrimitive()) {
            var prim = elem.getAsJsonPrimitive();
            if (prim.isBoolean()) {
                return prim.getAsBoolean();
            }
            if (prim.isString()) {
                String str = prim.getAsString().trim().toLowerCase();
                if ("true".equals(str) || "1".equals(str) || "yes".equals(str)) {
                    return true;
                }
                if ("false".equals(str) || "0".equals(str) || "no".equals(str)) {
                    return false;
                }
            }
            if (prim.isNumber()) {
                return prim.getAsInt() != 0;
            }
        }
        BlockSwap.LOGGER.error("Invalid value for '{}': expected boolean (true/false), found '{}'. Falling back to default ({}).",
                key, elem, defaultValue);
        return defaultValue;
    }

    private static Optional<Block> parseBlock(String id) {
        try {
            ResourceLocation rl = ResourceLocation.parse(id);
            if (BuiltInRegistries.BLOCK.containsKey(rl)) {
                return Optional.of(BuiltInRegistries.BLOCK.get(rl));
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }

    public String toJsonString() {
        JsonObject root = new JsonObject();

        if (!globalDimensions.isEmpty()) {
            if (globalDimensions.size() == 1) {
                root.addProperty("dimension", globalDimensions.iterator().next().toString());
            } else {
                JsonArray dimsArr = new JsonArray();
                for (ResourceLocation dim : globalDimensions) {
                    dimsArr.add(dim.toString());
                }
                root.add("dimensions", dimsArr);
            }
        }

        JsonObject swapper = new JsonObject();
        for (Map.Entry<TagKey<Block>, List<SwapRule<Block>>> entry : tagRules.entrySet()) {
            String key = "#" + entry.getKey().location().toString();
            swapper.add(key, serializeRules(entry.getValue(), globalDimensions));
        }
        for (Map.Entry<Block, List<SwapRule<Block>>> entry : blockRules.entrySet()) {
            String key = BuiltInRegistries.BLOCK.getKey(entry.getKey()).toString();
            swapper.add(key, serializeRules(entry.getValue(), globalDimensions));
        }
        root.add("swapper", swapper);

        JsonArray stateSwapper = new JsonArray();
        for (Map.Entry<BlockState, List<SwapRule<BlockState>>> entry : stateRules.entrySet()) {
            for (SwapRule<BlockState> rule : entry.getValue()) {
                JsonObject pair = new JsonObject();
                BlockState.CODEC.encodeStart(JsonOps.INSTANCE, entry.getKey())
                        .resultOrPartial(BlockSwap.LOGGER::error)
                        .ifPresent(json -> pair.add("old", json));
                BlockState.CODEC.encodeStart(JsonOps.INSTANCE, rule.target())
                        .resultOrPartial(BlockSwap.LOGGER::error)
                        .ifPresent(json -> pair.add("new", json));
                if (!rule.dimensions().isEmpty() && !rule.dimensions().equals(globalDimensions)) {
                    if (rule.dimensions().size() == 1) {
                        pair.addProperty("dimension", rule.dimensions().iterator().next().toString());
                    } else {
                        JsonArray arr = new JsonArray();
                        for (ResourceLocation d : rule.dimensions()) {
                            arr.add(d.toString());
                        }
                        pair.add("dimensions", arr);
                    }
                }
                stateSwapper.add(pair);
            }
        }
        root.add("state_swapper", stateSwapper);

        root.addProperty("retro_gen", retroGen);
        root.addProperty("generate_all_known_states", generateAllKnownStates);
        root.addProperty("onlySwapNaturalGeneration", onlySwapNaturalGeneration);

        return root.toString();
    }

    private static JsonElement serializeRules(List<SwapRule<Block>> rules, Set<ResourceLocation> globalDims) {
        if (rules.size() == 1) {
            SwapRule<Block> rule = rules.get(0);
            String targetId = BuiltInRegistries.BLOCK.getKey(rule.target()).toString();
            if (rule.dimensions().isEmpty() || rule.dimensions().equals(globalDims)) {
                return new com.google.gson.JsonPrimitive(targetId);
            } else {
                JsonObject obj = new JsonObject();
                obj.addProperty("replaceWith", targetId);
                if (rule.dimensions().size() == 1) {
                    obj.addProperty("dimension", rule.dimensions().iterator().next().toString());
                } else {
                    JsonArray dimsArr = new JsonArray();
                    for (ResourceLocation dim : rule.dimensions()) {
                        dimsArr.add(dim.toString());
                    }
                    obj.add("dimensions", dimsArr);
                }
                return obj;
            }
        }

        JsonArray arr = new JsonArray();
        for (SwapRule<Block> rule : rules) {
            String targetId = BuiltInRegistries.BLOCK.getKey(rule.target()).toString();
            JsonObject obj = new JsonObject();
            obj.addProperty("replaceWith", targetId);
            if (!rule.dimensions().isEmpty() && !rule.dimensions().equals(globalDims)) {
                if (rule.dimensions().size() == 1) {
                    obj.addProperty("dimension", rule.dimensions().iterator().next().toString());
                } else {
                    JsonArray dimsArr = new JsonArray();
                    for (ResourceLocation dim : rule.dimensions()) {
                        dimsArr.add(dim.toString());
                    }
                    obj.add("dimensions", dimsArr);
                }
            }
            arr.add(obj);
        }
        return arr;
    }
}
