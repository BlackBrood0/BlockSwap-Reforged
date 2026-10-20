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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

public record BlockSwapConfig(
        Map<Block, Block> blockBlockMap,
        Map<BlockState, BlockState> blockStateBlockStateMap,
        boolean retroGen,
        boolean generateAllKnownStates
) {
    public static final BlockSwapConfig DEFAULT = new BlockSwapConfig(new IdentityHashMap<>(), new IdentityHashMap<>(), false, true);

    private static final Jankson JANKSON = Jankson.builder().build();
    private static BlockSwapConfig CONFIG = null;

    private static final String DEFAULT_CONFIG_TEMPLATE = """
            {
            	// A map of blocks that specifies what the "old" block is and what its "new" block is.
            	// Example:
            	// "swapper": {
            	// 	"minecraft:coarse_dirt": "minecraft:dirt",
            	// 	"minecraft:diamond_block": "minecraft:emerald_block"
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
            	// 		}
            	// 	}
            	// ]
            	"state_swapper": [],

            	// Whether blocks are replaced in existing chunks.
            	"retro_gen": false,

            	// Generates all block states for all blocks in the registry.
            	"generate_all_known_states": true
            }
            """;

    public boolean contains(BlockState state) {
        return blockBlockMap.containsKey(state.getBlock()) || blockStateBlockStateMap.containsKey(state);
    }

    public static BlockSwapConfig getConfig(BlockSwapConfig serverConfig) {
        CONFIG = serverConfig;
        return CONFIG;
    }

    public static BlockSwapConfig getConfig(boolean reload) {
        if (CONFIG == null || reload) {
            Path path = BlockSwap.CONFIG_PATH.resolve("block_swap.json5");
            File configFile = path.toFile();

            if (!configFile.exists()) {
                try {
                    Files.createDirectories(path.getParent());
                    Files.writeString(path, DEFAULT_CONFIG_TEMPLATE);
                } catch (IOException e) {
                    BlockSwap.LOGGER.error("Failed to create default config at {}", path, e);
                }
                CONFIG = DEFAULT;
                return CONFIG;
            }

            CONFIG = loadFromFile(path);
        }
        return CONFIG;
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
            com.google.gson.JsonObject root = rootElement.getAsJsonObject();

            Map<Block, Block> rawBlockMap = new IdentityHashMap<>();
            if (root.has("swapper") && root.get("swapper").isJsonObject()) {
                com.google.gson.JsonObject swapperObj = root.getAsJsonObject("swapper");
                for (Map.Entry<String, JsonElement> entry : swapperObj.entrySet()) {
                    String oldId = entry.getKey();
                    if (!entry.getValue().isJsonPrimitive()) {
                        BlockSwap.LOGGER.error("Invalid value in 'swapper' for key '{}': expected string block ID", oldId);
                        continue;
                    }
                    String newId = entry.getValue().getAsString();

                    Optional<Block> oldBlock = parseBlock(oldId);
                    Optional<Block> newBlock = parseBlock(newId);

                    if (oldBlock.isEmpty()) {
                        BlockSwap.LOGGER.error("Unknown block '{}' in 'swapper' old block entry, skipping", oldId);
                        continue;
                    }
                    if (newBlock.isEmpty()) {
                        BlockSwap.LOGGER.error("Unknown block '{}' in 'swapper' new block entry, skipping", newId);
                        continue;
                    }

                    rawBlockMap.put(oldBlock.get(), newBlock.get());
                }
            }

            Map<BlockState, BlockState> rawStateMap = new IdentityHashMap<>();
            if (root.has("state_swapper") && root.get("state_swapper").isJsonArray()) {
                JsonArray stateArray = root.getAsJsonArray("state_swapper");
                for (JsonElement element : stateArray) {
                    if (!element.isJsonObject()) {
                        BlockSwap.LOGGER.error("Invalid entry in 'state_swapper': expected object");
                        continue;
                    }
                    com.google.gson.JsonObject pairObj = element.getAsJsonObject();
                    if (!pairObj.has("old") || !pairObj.has("new")) {
                        BlockSwap.LOGGER.error("Entry in 'state_swapper' missing 'old' or 'new' field: {}", pairObj);
                        continue;
                    }

                    Optional<BlockState> oldState = BlockState.CODEC.parse(JsonOps.INSTANCE, pairObj.get("old"))
                            .resultOrPartial(err -> BlockSwap.LOGGER.error("Error parsing 'old' state in state_swapper: {}", err));
                    Optional<BlockState> newState = BlockState.CODEC.parse(JsonOps.INSTANCE, pairObj.get("new"))
                            .resultOrPartial(err -> BlockSwap.LOGGER.error("Error parsing 'new' state in state_swapper: {}", err));

                    if (oldState.isPresent() && newState.isPresent()) {
                        rawStateMap.put(oldState.get(), newState.get());
                    }
                }
            }

            boolean retroGen = root.has("retro_gen") && root.get("retro_gen").getAsBoolean();
            boolean generateAllKnownStates = !root.has("generate_all_known_states") || root.get("generate_all_known_states").getAsBoolean();

            // Verify circular references
            Map<Block, Block> verifiedBlockMap = new IdentityHashMap<>();
            for (Map.Entry<Block, Block> entry : rawBlockMap.entrySet()) {
                if (rawBlockMap.containsKey(entry.getValue())) {
                    BlockSwap.LOGGER.error("Detected circular BLOCK reference in 'swapper'! Block '{}' maps to '{}' which is also swapped.",
                            BuiltInRegistries.BLOCK.getKey(entry.getKey()),
                            BuiltInRegistries.BLOCK.getKey(entry.getValue()));
                } else {
                    verifiedBlockMap.put(entry.getKey(), entry.getValue());
                }
            }

            Map<BlockState, BlockState> verifiedStateMap = new IdentityHashMap<>();
            for (Map.Entry<BlockState, BlockState> entry : rawStateMap.entrySet()) {
                if (rawStateMap.containsKey(entry.getValue())) {
                    BlockSwap.LOGGER.error("Detected circular BLOCKSTATE reference in 'state_swapper'! State '{}' is also swapped into.",
                            entry.getKey());
                } else {
                    verifiedStateMap.put(entry.getKey(), entry.getValue());
                }
            }

            return new BlockSwapConfig(verifiedBlockMap, verifiedStateMap, retroGen, generateAllKnownStates);
        } catch (Exception e) {
            BlockSwap.LOGGER.error("Failed to parse config json string, using default", e);
            return DEFAULT;
        }
    }

    private static Optional<Block> parseBlock(String id) {
        try {
            Identifier rl = Identifier.parse(id);
            if (BuiltInRegistries.BLOCK.containsKey(rl)) {
                return BuiltInRegistries.BLOCK.getOptional(rl);
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }

    public String toJsonString() {
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();

        com.google.gson.JsonObject swapper = new com.google.gson.JsonObject();
        for (Map.Entry<Block, Block> entry : blockBlockMap.entrySet()) {
            swapper.addProperty(BuiltInRegistries.BLOCK.getKey(entry.getKey()).toString(),
                    BuiltInRegistries.BLOCK.getKey(entry.getValue()).toString());
        }
        root.add("swapper", swapper);

        JsonArray stateSwapper = new JsonArray();
        for (Map.Entry<BlockState, BlockState> entry : blockStateBlockStateMap.entrySet()) {
            com.google.gson.JsonObject pair = new com.google.gson.JsonObject();
            BlockState.CODEC.encodeStart(JsonOps.INSTANCE, entry.getKey())
                    .resultOrPartial(BlockSwap.LOGGER::error)
                    .ifPresent(json -> pair.add("old", json));
            BlockState.CODEC.encodeStart(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(BlockSwap.LOGGER::error)
                    .ifPresent(json -> pair.add("new", json));
            stateSwapper.add(pair);
        }
        root.add("state_swapper", stateSwapper);

        root.addProperty("retro_gen", retroGen);
        root.addProperty("generate_all_known_states", generateAllKnownStates);

        return root.toString();
    }
}
