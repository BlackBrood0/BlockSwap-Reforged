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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.IdentityHashMap;

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
        assertEquals(Blocks.DIRT, config.blockBlockMap().get(Blocks.COARSE_DIRT));
        assertEquals(Blocks.BIRCH_LOG, config.blockBlockMap().get(Blocks.OAK_LOG));
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
}

