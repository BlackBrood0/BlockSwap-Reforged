/*
 * Copyright (c) 2026 CorgiTaco
 *
 * This file is part of Block Swap, licensed under the GNU Lesser General Public License v3.0 (LGPL-3.0).
 * You should have received a copy of the LGPL-3.0 along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.html>.
 */

package corgitaco.blockswap.config;

import blue.endless.jankson.Jankson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import corgitaco.blockswap.BlockSwap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record MissingBlockIDsConfig(Map<String, Block> idRemapper) {
    public static final MissingBlockIDsConfig DEFAULT = new MissingBlockIDsConfig(new HashMap<>());

    private static final Jankson JANKSON = Jankson.builder().build();
    private static MissingBlockIDsConfig CONFIG = null;

    private static final String DEFAULT_CONFIG_TEMPLATE = """
            {
            	// A map of blocks that specifies what the "old" broken block is and what its "new" functional block is.
            	// Example:
            	// "id_remapper": {
            	// 	"minecraft:coarse_dirt": "minecraft:dirt",
            	// 	"minecraft:diamond_block": "minecraft:emerald_block"
            	// }
            	"id_remapper": {}
            }
            """;

    public static MissingBlockIDsConfig getConfig(boolean reload) {
        if (CONFIG == null || reload) {
            Path path = BlockSwap.CONFIG_PATH.resolve("missing_block_ids.json5");
            File configFile = path.toFile();

            if (!configFile.exists()) {
                try {
                    Files.createDirectories(path.getParent());
                    Files.writeString(path, DEFAULT_CONFIG_TEMPLATE);
                } catch (IOException e) {
                    BlockSwap.LOGGER.error("Failed to create default missing_block_ids config at {}", path, e);
                }
                CONFIG = DEFAULT;
                return CONFIG;
            }

            CONFIG = loadFromFile(path);
        }
        return CONFIG;
    }

    public static MissingBlockIDsConfig loadFromFile(Path path) {
        try {
            blue.endless.jankson.JsonObject janksonObj = JANKSON.load(path.toFile());
            String standardJson = janksonObj.toJson(false, false);
            return parseJsonString(standardJson);
        } catch (Exception e) {
            BlockSwap.LOGGER.error("Failed to parse missing_block_ids config at {}, using default", path, e);
            return DEFAULT;
        }
    }

    public static MissingBlockIDsConfig parseJsonString(String jsonString) {
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
                return DEFAULT;
            }
            JsonObject root = rootElement.getAsJsonObject();
            Map<String, Block> map = new HashMap<>();

            if (root.has("id_remapper") && root.get("id_remapper").isJsonObject()) {
                JsonObject remapperObj = root.getAsJsonObject("id_remapper");
                for (Map.Entry<String, JsonElement> entry : remapperObj.entrySet()) {
                    String brokenId = entry.getKey();
                    if (!entry.getValue().isJsonPrimitive()) {
                        continue;
                    }
                    String validId = entry.getValue().getAsString();
                    Optional<Block> replacement = parseBlock(validId);
                    if (replacement.isPresent()) {
                        map.put(brokenId, replacement.get());
                    } else {
                        BlockSwap.LOGGER.error("Unknown replacement block '{}' in 'id_remapper' for '{}', skipping", validId, brokenId);
                    }
                }
            }
            return new MissingBlockIDsConfig(map);
        } catch (Exception e) {
            BlockSwap.LOGGER.error("Failed to parse missing_block_ids JSON", e);
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
}
