/*
 * Copyright (c) 2026 CorgiTaco
 *
 * This file is part of Block Swap, licensed under the GNU Lesser General Public License v3.0 (LGPL-3.0).
 * You should have received a copy of the LGPL-3.0 along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.html>.
 */

package corgitaco.blockswap;

import corgitaco.blockswap.config.BlockSwapConfig;
import corgitaco.blockswap.config.MissingBlockIDsConfig;
import corgitaco.blockswap.swapper.Swapper;
import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class BlockSwapUnitTest {

    @BeforeAll
    public static void setupMinecraft() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        CompoundTag tag = new CompoundTag();
        ListTag sizeList = new ListTag();
        sizeList.add(IntTag.valueOf(3));
        sizeList.add(IntTag.valueOf(3));
        sizeList.add(IntTag.valueOf(3));
        tag.put("size", sizeList);
        tag.put("entities", new ListTag());
        tag.put("blocks", new ListTag());
        ListTag palette = new ListTag();
        CompoundTag air = new CompoundTag();
        air.putString("Name", "minecraft:air");
        palette.add(air);
        tag.put("palette", palette);
        tag.putInt("DataVersion", 3955);

        Path dir = Path.of("src/main/resources/data/blockswap/structure");
        Files.createDirectories(dir);
        NbtIo.writeCompressed(tag, dir.resolve("empty.nbt"));
        NbtIo.writeCompressed(tag, dir.resolve("blockswapgametests.empty.nbt"));
    }

    @Test
    public void testValidJson5ConfigParsing() {
        String json5 = """
                {
                    // Comments in JSON5
                    "swapper": {
                        "minecraft:coarse_dirt": "minecraft:dirt",
                        "minecraft:oak_log": "minecraft:birch_log",
                    },
                    /* Multiline comment */
                    "retro_gen": true,
                    "generate_all_known_states": false
                }
                """;

        BlockSwapConfig config = BlockSwapConfig.parseJsonString(json5);
        assertNotNull(config);
        assertTrue(config.retroGen());
        assertFalse(config.generateAllKnownStates());
        assertFalse(config.onlySwapNaturalGeneration());
        assertEquals(Blocks.DIRT, config.blockBlockMap().get(Blocks.COARSE_DIRT));
        assertEquals(Blocks.BIRCH_LOG, config.blockBlockMap().get(Blocks.OAK_LOG));
    }

    @Test
    public void testOnlySwapNaturalGenerationConfigParsing() {
        // 1. Explicitly true
        String jsonTrue = """
                {
                    "swapper": {
                        "minecraft:granite": "minecraft:diorite"
                    },
                    "onlySwapNaturalGeneration": true
                }
                """;
        BlockSwapConfig configTrue = BlockSwapConfig.parseJsonString(jsonTrue);
        assertNotNull(configTrue);
        assertTrue(configTrue.onlySwapNaturalGeneration());

        // 2. Snake case variant
        String jsonSnake = """
                {
                    "swapper": {
                        "minecraft:granite": "minecraft:diorite"
                    },
                    "only_swap_natural_generation": true
                }
                """;
        BlockSwapConfig configSnake = BlockSwapConfig.parseJsonString(jsonSnake);
        assertNotNull(configSnake);
        assertTrue(configSnake.onlySwapNaturalGeneration());

        // 3. Explicitly false
        String jsonFalse = """
                {
                    "swapper": {
                        "minecraft:granite": "minecraft:diorite"
                    },
                    "onlySwapNaturalGeneration": false
                }
                """;
        BlockSwapConfig configFalse = BlockSwapConfig.parseJsonString(jsonFalse);
        assertNotNull(configFalse);
        assertFalse(configFalse.onlySwapNaturalGeneration());

        // 4. Absent key defaults to false
        String jsonAbsent = """
                {
                    "swapper": {
                        "minecraft:granite": "minecraft:diorite"
                    }
                }
                """;
        BlockSwapConfig configAbsent = BlockSwapConfig.parseJsonString(jsonAbsent);
        assertNotNull(configAbsent);
        assertFalse(configAbsent.onlySwapNaturalGeneration());

        // 5. Invalid string value falls back to false without crashing
        String jsonInvalidString = """
                {
                    "swapper": {
                        "minecraft:granite": "minecraft:diorite"
                    },
                    "onlySwapNaturalGeneration": "notABooleanValue"
                }
                """;
        BlockSwapConfig configInvalid = BlockSwapConfig.parseJsonString(jsonInvalidString);
        assertNotNull(configInvalid);
        assertFalse(configInvalid.onlySwapNaturalGeneration());
        assertEquals(Blocks.DIORITE, configInvalid.blockBlockMap().get(Blocks.GRANITE));

        // 6. Invalid object value falls back to false without crashing
        String jsonInvalidObj = """
                {
                    "swapper": {
                        "minecraft:granite": "minecraft:diorite"
                    },
                    "onlySwapNaturalGeneration": { "invalid": true }
                }
                """;
        BlockSwapConfig configInvalidObj = BlockSwapConfig.parseJsonString(jsonInvalidObj);
        assertNotNull(configInvalidObj);
        assertFalse(configInvalidObj.onlySwapNaturalGeneration());

        // 7. String representation "true"
        String jsonStringTrue = """
                {
                    "swapper": {
                        "minecraft:granite": "minecraft:diorite"
                    },
                    "onlySwapNaturalGeneration": "true"
                }
                """;
        BlockSwapConfig configStringTrue = BlockSwapConfig.parseJsonString(jsonStringTrue);
        assertNotNull(configStringTrue);
        assertTrue(configStringTrue.onlySwapNaturalGeneration());

        // 8. Key alias "naturalOnly"
        String jsonAlias = """
                {
                    "swapper": {
                        "minecraft:granite": "minecraft:diorite"
                    },
                    "naturalOnly": true
                }
                """;
        BlockSwapConfig configAlias = BlockSwapConfig.parseJsonString(jsonAlias);
        assertNotNull(configAlias);
        assertTrue(configAlias.onlySwapNaturalGeneration());
    }

    @Test
    public void testLivePlacementTracker() {
        Swapper.resetLivePlacement();
        assertFalse(Swapper.isLivePlacement());

        Swapper.enterLivePlacement();
        assertTrue(Swapper.isLivePlacement());

        // Nested call
        Swapper.enterLivePlacement();
        assertTrue(Swapper.isLivePlacement());

        Swapper.exitLivePlacement();
        assertTrue(Swapper.isLivePlacement());

        Swapper.exitLivePlacement();
        assertFalse(Swapper.isLivePlacement());

        // Extra exit does not underflow
        Swapper.exitLivePlacement();
        assertFalse(Swapper.isLivePlacement());
    }

    @Test
    public void testSerializationRoundTripWithOnlyNaturalGen() {
        IdentityHashMap<net.minecraft.world.level.block.Block, net.minecraft.world.level.block.Block> blockMap = new IdentityHashMap<>();
        blockMap.put(Blocks.GRANITE, Blocks.DIORITE);
        BlockSwapConfig config = new BlockSwapConfig(blockMap, new IdentityHashMap<>(), false, false, true);

        String json = config.toJsonString();
        assertTrue(json.contains("\"onlySwapNaturalGeneration\":true"));

        BlockSwapConfig parsed = BlockSwapConfig.parseJsonString(json);
        assertTrue(parsed.onlySwapNaturalGeneration());
        assertEquals(Blocks.DIORITE, parsed.blockBlockMap().get(Blocks.GRANITE));
    }

    @Test
    public void testMalformedJsonHandling() {
        String malformedJson = "{ this is not valid json or json5 at all !!! :::";

        // Must not throw exception; should return default config
        BlockSwapConfig config = BlockSwapConfig.parseJsonString(malformedJson);
        assertNotNull(config);
        assertEquals(BlockSwapConfig.DEFAULT, config);
    }

    @Test
    public void testUnknownBlockIdHandling() {
        String jsonWithUnknownBlocks = """
                {
                    "swapper": {
                        "minecraft:this_block_does_not_exist_at_all": "minecraft:dirt",
                        "minecraft:oak_log": "minecraft:this_block_also_does_not_exist",
                        "minecraft:granite": "minecraft:diorite"
                    }
                }
                """;

        BlockSwapConfig config = BlockSwapConfig.parseJsonString(jsonWithUnknownBlocks);
        assertNotNull(config);
        // Valid pair must still be loaded
        assertEquals(Blocks.DIORITE, config.blockBlockMap().get(Blocks.GRANITE));
        // Invalid pairs must be omitted cleanly
        assertEquals(1, config.blockBlockMap().size());
    }

    @Test
    public void testCircularReferenceDetection() {
        String circularJson = """
                {
                    "swapper": {
                        "minecraft:granite": "minecraft:diorite",
                        "minecraft:diorite": "minecraft:andesite"
                    }
                }
                """;

        BlockSwapConfig config = BlockSwapConfig.parseJsonString(circularJson);
        assertNotNull(config);
        // Circular reference must be rejected to prevent infinite swap loops
        assertFalse(config.blockBlockMap().containsKey(Blocks.GRANITE) && config.blockBlockMap().containsKey(Blocks.DIORITE));
    }

    @Test
    public void testPropertyRemappingAxis() {
        IdentityHashMap<net.minecraft.world.level.block.Block, net.minecraft.world.level.block.Block> blockMap = new IdentityHashMap<>();
        blockMap.put(Blocks.OAK_LOG, Blocks.BIRCH_LOG);
        BlockSwapConfig.getConfig(new BlockSwapConfig(blockMap, new IdentityHashMap<>(), false, false));

        BlockState oakLogZ = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        BlockState remapped = Swapper.remapState(oakLogZ);

        assertEquals(Blocks.BIRCH_LOG, remapped.getBlock());
        assertEquals(Direction.Axis.Z, remapped.getValue(RotatedPillarBlock.AXIS));
    }

    @Test
    public void testPropertyRemappingStairs() {
        IdentityHashMap<net.minecraft.world.level.block.Block, net.minecraft.world.level.block.Block> blockMap = new IdentityHashMap<>();
        blockMap.put(Blocks.OAK_STAIRS, Blocks.BIRCH_STAIRS);
        BlockSwapConfig.getConfig(new BlockSwapConfig(blockMap, new IdentityHashMap<>(), false, false));

        BlockState oakStairs = Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, Direction.WEST)
                .setValue(StairBlock.HALF, Half.TOP)
                .setValue(StairBlock.SHAPE, StairsShape.OUTER_LEFT)
                .setValue(StairBlock.WATERLOGGED, true);

        BlockState remapped = Swapper.remapState(oakStairs);

        assertEquals(Blocks.BIRCH_STAIRS, remapped.getBlock());
        assertEquals(Direction.WEST, remapped.getValue(StairBlock.FACING));
        assertEquals(Half.TOP, remapped.getValue(StairBlock.HALF));
        assertEquals(StairsShape.OUTER_LEFT, remapped.getValue(StairBlock.SHAPE));
        assertTrue(remapped.getValue(StairBlock.WATERLOGGED));
    }

    @Test
    public void testPropertyRemappingSlabWaterlogged() {
        IdentityHashMap<net.minecraft.world.level.block.Block, net.minecraft.world.level.block.Block> blockMap = new IdentityHashMap<>();
        blockMap.put(Blocks.OAK_SLAB, Blocks.BIRCH_SLAB);
        BlockSwapConfig.getConfig(new BlockSwapConfig(blockMap, new IdentityHashMap<>(), false, false));

        BlockState oakSlab = Blocks.OAK_SLAB.defaultBlockState()
                .setValue(SlabBlock.TYPE, SlabType.TOP)
                .setValue(SlabBlock.WATERLOGGED, true);

        BlockState remapped = Swapper.remapState(oakSlab);

        assertEquals(Blocks.BIRCH_SLAB, remapped.getBlock());
        assertEquals(SlabType.TOP, remapped.getValue(SlabBlock.TYPE));
        assertTrue(remapped.getValue(SlabBlock.WATERLOGGED));
    }

    @Test
    public void testMissingBlockIDsConfigParsing() {
        String json = """
                {
                    "id_remapper": {
                        "mod:deleted_block": "minecraft:dirt",
                        "mod:invalid_block": "minecraft:non_existent"
                    }
                }
                """;

        MissingBlockIDsConfig config = MissingBlockIDsConfig.parseJsonString(json);
        assertNotNull(config);
        assertEquals(Blocks.DIRT, config.idRemapper().get("mod:deleted_block"));
        assertNull(config.idRemapper().get("mod:invalid_block"));
    }

    @Test
    public void testWorldGenRegionChunkVerification() throws Exception {
        Path regionDir = Path.of("run/world/region");
        if (!Files.exists(regionDir)) {
            regionDir = Path.of("../run/world/region");
        }
        if (!Files.exists(regionDir)) {
            regionDir = Path.of("../../run/world/region");
        }
        if (!Files.exists(regionDir)) {
            System.out.println("No region directory found, skipping region disk check");
            return;
        }

        int chunksChecked = 0;
        int dioriteFoundCount = 0;
        try (java.nio.file.DirectoryStream<Path> stream = Files.newDirectoryStream(regionDir, "*.mca")) {
            for (Path mcaFile : stream) {
                // Skip region files generated for GameTest structures at extreme coordinates (|rx| > 100 or |rz| > 100)
                String fileName = mcaFile.getFileName().toString();
                String[] parts = fileName.split("\\.");
                if (parts.length >= 3) {
                    try {
                        int rx = Integer.parseInt(parts[1]);
                        int rz = Integer.parseInt(parts[2]);
                        if (Math.abs(rx) > 100 || Math.abs(rz) > 100) {
                            continue;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }

                try (net.minecraft.world.level.chunk.storage.RegionFile regionFile = new net.minecraft.world.level.chunk.storage.RegionFile(
                        new net.minecraft.world.level.chunk.storage.RegionStorageInfo("world", Level.OVERWORLD, "region"),
                        mcaFile,
                        regionDir,
                        true
                )) {
                    for (int x = 0; x < 32; x++) {
                        for (int z = 0; z < 32; z++) {
                            net.minecraft.world.level.ChunkPos pos = new net.minecraft.world.level.ChunkPos(x, z);
                            if (regionFile.hasChunk(pos)) {
                                try (java.io.DataInputStream dis = regionFile.getChunkDataInputStream(pos)) {
                                    if (dis != null) {
                                        CompoundTag tag = NbtIo.read(dis);
                                        ListTag sections = tag.getList("sections", net.minecraft.nbt.Tag.TAG_COMPOUND);
                                        for (int s = 0; s < sections.size(); s++) {
                                            CompoundTag sec = sections.getCompound(s);
                                            if (sec.contains("block_states", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                                                CompoundTag bs = sec.getCompound("block_states");
                                                if (bs.contains("palette", net.minecraft.nbt.Tag.TAG_LIST)) {
                                                    ListTag pal = bs.getList("palette", net.minecraft.nbt.Tag.TAG_COMPOUND);
                                                    for (int p = 0; p < pal.size(); p++) {
                                                        String name = pal.getCompound(p).getString("Name");
                                                        assertNotEquals("minecraft:granite", name, "Found replaced block minecraft:granite in generated chunk at " + pos);
                                                        if ("minecraft:diorite".equals(name)) {
                                                            dioriteFoundCount++;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        chunksChecked++;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        System.out.println("Verified " + chunksChecked + " chunks from region files. Zero granite found, diorite occurrences: " + dioriteFoundCount);
        assertTrue(chunksChecked > 0, "Expected at least one generated chunk in region files");
    }

    @Test
    public void testEmptyConfigRegression() {
        BlockSwapConfig.getConfig(BlockSwapConfig.DEFAULT);

        BlockState granite = Blocks.GRANITE.defaultBlockState();
        BlockState oakLog = Blocks.OAK_LOG.defaultBlockState();
        BlockState oakStairs = Blocks.OAK_STAIRS.defaultBlockState();

        assertSame(granite, Swapper.remapState(granite));
        assertSame(oakLog, Swapper.remapState(oakLog));
        assertSame(oakStairs, Swapper.remapState(oakStairs));
        assertFalse(BlockSwapConfig.DEFAULT.contains(granite));
    }

    @Test
    public void testConfigReload() throws Exception {
        Path tempConfigFile = Files.createTempFile("block_swap_test", ".json5");
        try {
            // Initial configuration
            String initialJson = """
                    {
                        "swapper": {
                            "minecraft:granite": "minecraft:diorite"
                        }
                    }
                    """;
            Files.writeString(tempConfigFile, initialJson);
            BlockSwapConfig config1 = BlockSwapConfig.loadFromFile(tempConfigFile);
            BlockSwapConfig.getConfig(config1);

            assertEquals(Blocks.DIORITE, Swapper.remapState(Blocks.GRANITE.defaultBlockState()).getBlock());
            assertEquals(Blocks.COBBLESTONE, Swapper.remapState(Blocks.COBBLESTONE.defaultBlockState()).getBlock());

            // Reloaded configuration
            String reloadedJson = """
                    {
                        "swapper": {
                            "minecraft:cobblestone": "minecraft:mossy_cobblestone"
                        }
                    }
                    """;
            Files.writeString(tempConfigFile, reloadedJson);
            BlockSwapConfig config2 = BlockSwapConfig.loadFromFile(tempConfigFile);
            BlockSwapConfig.getConfig(config2);

            // granite is no longer swapped
            assertEquals(Blocks.GRANITE, Swapper.remapState(Blocks.GRANITE.defaultBlockState()).getBlock());
            // cobblestone is now swapped
            assertEquals(Blocks.MOSSY_COBBLESTONE, Swapper.remapState(Blocks.COBBLESTONE.defaultBlockState()).getBlock());
        } finally {
            Files.deleteIfExists(tempConfigFile);
        }
    }

    @Test
    public void testTagParsingAndResolution() {
        TagKey<net.minecraft.world.level.block.Block> testTag = TagKey.create(Registries.BLOCK, ResourceLocation.parse("minecraft:coal_ores"));
        BuiltInRegistries.BLOCK.bindTags(Map.of(testTag, List.of(
                BuiltInRegistries.BLOCK.wrapAsHolder(Blocks.COAL_ORE),
                BuiltInRegistries.BLOCK.wrapAsHolder(Blocks.DEEPSLATE_COAL_ORE)
        )));

        String json = """
                {
                    "swapper": {
                        "#minecraft:coal_ores": "minecraft:infested_cobblestone"
                    }
                }
                """;

        BlockSwapConfig config = BlockSwapConfig.parseJsonString(json).resolveTags();
        assertNotNull(config);
        assertEquals(Blocks.INFESTED_COBBLESTONE, config.blockBlockMap().get(Blocks.COAL_ORE));
        assertEquals(Blocks.INFESTED_COBBLESTONE, config.blockBlockMap().get(Blocks.DEEPSLATE_COAL_ORE));
    }

    @Test
    public void testTagWithDirectBlockOverride() {
        TagKey<net.minecraft.world.level.block.Block> testTag = TagKey.create(Registries.BLOCK, ResourceLocation.parse("minecraft:coal_ores"));
        BuiltInRegistries.BLOCK.bindTags(Map.of(testTag, List.of(
                BuiltInRegistries.BLOCK.wrapAsHolder(Blocks.COAL_ORE),
                BuiltInRegistries.BLOCK.wrapAsHolder(Blocks.DEEPSLATE_COAL_ORE)
        )));

        String json = """
                {
                    "swapper": {
                        "#minecraft:coal_ores": "minecraft:infested_cobblestone",
                        "minecraft:coal_ore": "minecraft:diorite"
                    }
                }
                """;

        BlockSwapConfig config = BlockSwapConfig.parseJsonString(json).resolveTags();
        assertNotNull(config);
        // Direct block override takes precedence
        assertEquals(Blocks.DIORITE, config.blockBlockMap().get(Blocks.COAL_ORE));
        // Other blocks in the tag receive the tag mapping
        assertEquals(Blocks.INFESTED_COBBLESTONE, config.blockBlockMap().get(Blocks.DEEPSLATE_COAL_ORE));
    }

    @Test
    public void testTagSerializationRoundTrip() {
        TagKey<net.minecraft.world.level.block.Block> testTag = TagKey.create(Registries.BLOCK, ResourceLocation.parse("c:ores"));
        Map<TagKey<net.minecraft.world.level.block.Block>, net.minecraft.world.level.block.Block> tagMap = new LinkedHashMap<>();
        tagMap.put(testTag, Blocks.INFESTED_COBBLESTONE);

        BlockSwapConfig config = new BlockSwapConfig(new IdentityHashMap<>(), new IdentityHashMap<>(), tagMap, false, false, true);
        String json = config.toJsonString();
        assertTrue(json.contains("\"#c:ores\":\"minecraft:infested_cobblestone\""));

        BlockSwapConfig parsed = BlockSwapConfig.parseJsonString(json);
        assertTrue(parsed.tagBlockMap().containsKey(testTag));
        assertEquals(Blocks.INFESTED_COBBLESTONE, parsed.tagBlockMap().get(testTag));
    }

    @Test
    public void testGlobalDimensionCondition() {
        String json = """
                {
                    "dimension": "minecraft:the_nether",
                    "swapper": {
                        "minecraft:dirt": "minecraft:sponge"
                    }
                }
                """;

        BlockSwapConfig config = BlockSwapConfig.parseJsonString(json);
        assertNotNull(config);
        assertTrue(config.globalDimensions().contains(ResourceLocation.parse("minecraft:the_nether")));

        ResourceLocation nether = ResourceLocation.parse("minecraft:the_nether");
        ResourceLocation overworld = ResourceLocation.parse("minecraft:overworld");

        assertTrue(config.contains(Blocks.DIRT.defaultBlockState(), nether));
        assertFalse(config.contains(Blocks.DIRT.defaultBlockState(), overworld));

        assertEquals(Blocks.SPONGE, config.getTargetBlock(Blocks.DIRT, nether));
        assertNull(config.getTargetBlock(Blocks.DIRT, overworld));

        BlockState remappedNether = Swapper.remapState(Blocks.DIRT.defaultBlockState(), nether, config);
        assertEquals(Blocks.SPONGE.defaultBlockState(), remappedNether);

        BlockState remappedOverworld = Swapper.remapState(Blocks.DIRT.defaultBlockState(), overworld, config);
        assertEquals(Blocks.DIRT.defaultBlockState(), remappedOverworld);
    }

    @Test
    public void testPerSwapDimensionCondition() {
        String json = """
                {
                    "swapper": {
                        "minecraft:dirt": {
                            "replaceWith": "minecraft:sponge",
                            "dimension": "minecraft:overworld"
                        },
                        "minecraft:stone": {
                            "replaceWith": "minecraft:granite",
                            "dimension": "the_nether"
                        }
                    }
                }
                """;

        BlockSwapConfig config = BlockSwapConfig.parseJsonString(json);
        assertNotNull(config);

        ResourceLocation overworld = ResourceLocation.parse("minecraft:overworld");
        ResourceLocation nether = ResourceLocation.parse("minecraft:the_nether");

        // Dirt only swaps in overworld
        assertTrue(config.contains(Blocks.DIRT.defaultBlockState(), overworld));
        assertFalse(config.contains(Blocks.DIRT.defaultBlockState(), nether));
        assertEquals(Blocks.SPONGE, config.getTargetBlock(Blocks.DIRT, overworld));
        assertNull(config.getTargetBlock(Blocks.DIRT, nether));

        // Stone only swaps in nether
        assertTrue(config.contains(Blocks.STONE.defaultBlockState(), nether));
        assertFalse(config.contains(Blocks.STONE.defaultBlockState(), overworld));
        assertEquals(Blocks.GRANITE, config.getTargetBlock(Blocks.STONE, nether));
        assertNull(config.getTargetBlock(Blocks.STONE, overworld));
    }

    @Test
    public void testMultiDimensionCondition() {
        String json = """
                {
                    "swapper": {
                        "minecraft:dirt": {
                            "replaceWith": "minecraft:sponge",
                            "dimensions": ["minecraft:overworld", "minecraft:the_end"]
                        }
                    }
                }
                """;

        BlockSwapConfig config = BlockSwapConfig.parseJsonString(json);
        assertNotNull(config);

        ResourceLocation overworld = ResourceLocation.parse("minecraft:overworld");
        ResourceLocation end = ResourceLocation.parse("minecraft:the_end");
        ResourceLocation nether = ResourceLocation.parse("minecraft:the_nether");

        assertTrue(config.contains(Blocks.DIRT.defaultBlockState(), overworld));
        assertTrue(config.contains(Blocks.DIRT.defaultBlockState(), end));
        assertFalse(config.contains(Blocks.DIRT.defaultBlockState(), nether));
    }

    @Test
    public void testPerSwapOverridesGlobalDimension() {
        String json = """
                {
                    "dimension": "minecraft:overworld",
                    "swapper": {
                        "minecraft:dirt": "minecraft:sponge",
                        "minecraft:stone": {
                            "replaceWith": "minecraft:granite",
                            "dimension": "minecraft:the_nether"
                        }
                    }
                }
                """;

        BlockSwapConfig config = BlockSwapConfig.parseJsonString(json);
        assertNotNull(config);

        ResourceLocation overworld = ResourceLocation.parse("minecraft:overworld");
        ResourceLocation nether = ResourceLocation.parse("minecraft:the_nether");

        // Dirt inherits global overworld
        assertTrue(config.contains(Blocks.DIRT.defaultBlockState(), overworld));
        assertFalse(config.contains(Blocks.DIRT.defaultBlockState(), nether));

        // Stone overrides to nether
        assertTrue(config.contains(Blocks.STONE.defaultBlockState(), nether));
        assertFalse(config.contains(Blocks.STONE.defaultBlockState(), overworld));
    }

    @Test
    public void testTagWithDimensionCondition() {
        TagKey<net.minecraft.world.level.block.Block> testTag = TagKey.create(Registries.BLOCK, ResourceLocation.parse("minecraft:coal_ores"));
        BuiltInRegistries.BLOCK.bindTags(Map.of(testTag, List.of(
                BuiltInRegistries.BLOCK.wrapAsHolder(Blocks.COAL_ORE),
                BuiltInRegistries.BLOCK.wrapAsHolder(Blocks.DEEPSLATE_COAL_ORE)
        )));

        String json = """
                {
                    "swapper": {
                        "#minecraft:coal_ores": {
                            "replaceWith": "minecraft:infested_cobblestone",
                            "dimension": "minecraft:the_nether"
                        }
                    }
                }
                """;

        BlockSwapConfig config = BlockSwapConfig.parseJsonString(json).resolveTags();
        assertNotNull(config);

        ResourceLocation nether = ResourceLocation.parse("minecraft:the_nether");
        ResourceLocation overworld = ResourceLocation.parse("minecraft:overworld");

        assertTrue(config.contains(Blocks.COAL_ORE.defaultBlockState(), nether));
        assertTrue(config.contains(Blocks.DEEPSLATE_COAL_ORE.defaultBlockState(), nether));

        assertFalse(config.contains(Blocks.COAL_ORE.defaultBlockState(), overworld));
        assertFalse(config.contains(Blocks.DEEPSLATE_COAL_ORE.defaultBlockState(), overworld));
    }

    @Test
    public void testPlacementAndWorldGenConfigSeparationOnReload() throws Exception {
        Path tempDir = Files.createTempDirectory("blockswap_reload_test");
        Path originalConfigPath = BlockSwap.CONFIG_PATH;
        try {
            BlockSwap.CONFIG_PATH = tempDir;

            // Initial config
            String initialJson = """
                    {
                        "swapper": {
                            "minecraft:dirt": "minecraft:sponge"
                        }
                    }
                    """;
            Files.writeString(tempDir.resolve("block_swap.json5"), initialJson);

            BlockSwapConfig initial = BlockSwapConfig.loadFromFile(tempDir.resolve("block_swap.json5"));
            BlockSwapConfig.setWorldGenConfig(initial);
            BlockSwapConfig.setPlacementConfig(initial);

            assertEquals(Blocks.SPONGE, BlockSwapConfig.getWorldGenConfig().getTargetBlock(Blocks.DIRT, null));
            assertEquals(Blocks.SPONGE, BlockSwapConfig.getPlacementConfig().getTargetBlock(Blocks.DIRT, null));

            // User updates config on disk
            String updatedJson = """
                    {
                        "swapper": {
                            "minecraft:dirt": "minecraft:emerald_block",
                            "minecraft:stone": "minecraft:diamond_block"
                        }
                    }
                    """;
            Files.writeString(tempDir.resolve("block_swap.json5"), updatedJson);

            // Execute placement reload (simulate /blockswap reload)
            BlockSwapConfig.reloadPlacementConfig();

            // WorldGen config must remain UNCHANGED
            assertEquals(Blocks.SPONGE, BlockSwapConfig.getWorldGenConfig().getTargetBlock(Blocks.DIRT, null));
            assertNull(BlockSwapConfig.getWorldGenConfig().getTargetBlock(Blocks.STONE, null));

            // Placement config must be RELOADED
            assertEquals(Blocks.EMERALD_BLOCK, BlockSwapConfig.getPlacementConfig().getTargetBlock(Blocks.DIRT, null));
            assertEquals(Blocks.DIAMOND_BLOCK, BlockSwapConfig.getPlacementConfig().getTargetBlock(Blocks.STONE, null));
        } finally {
            BlockSwap.CONFIG_PATH = originalConfigPath;
        }
    }
}

