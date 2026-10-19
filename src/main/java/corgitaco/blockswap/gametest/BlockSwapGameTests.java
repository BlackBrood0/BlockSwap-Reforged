/*
 * Copyright (c) 2026 CorgiTaco
 *
 * This file is part of Block Swap, licensed under the GNU Lesser General Public License v3.0 (LGPL-3.0).
 * You should have received a copy of the LGPL-3.0 along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.html>.
 */

package corgitaco.blockswap.gametest;

import corgitaco.blockswap.BlockSwap;
import corgitaco.blockswap.config.BlockSwapConfig;
import corgitaco.blockswap.swapper.Swapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;

import java.util.IdentityHashMap;

public class BlockSwapGameTests {

    private static void setupTestConfig() {
        IdentityHashMap<net.minecraft.world.level.block.Block, net.minecraft.world.level.block.Block> blockMap = new IdentityHashMap<>();
        blockMap.put(Blocks.GRANITE, Blocks.DIORITE);
        blockMap.put(Blocks.OAK_LOG, Blocks.BIRCH_LOG);
        blockMap.put(Blocks.OAK_STAIRS, Blocks.BIRCH_STAIRS);
        blockMap.put(Blocks.OAK_SLAB, Blocks.BIRCH_SLAB);

        IdentityHashMap<BlockState, BlockState> stateMap = new IdentityHashMap<>();
        // Explicit swap: gold block -> emerald block
        stateMap.put(Blocks.GOLD_BLOCK.defaultBlockState(), Blocks.EMERALD_BLOCK.defaultBlockState());

        BlockSwapConfig.getConfig(new BlockSwapConfig(blockMap, stateMap, false, false));
    }

    @GameTest(template = "empty", templateNamespace = BlockSwap.MOD_ID)
    public static void testLivePlacementGraniteToDiorite(GameTestHelper helper) {
        setupTestConfig();
        BlockPos pos = new BlockPos(1, 1, 1);

        helper.setBlock(pos, Blocks.GRANITE);

        helper.assertBlockPresent(Blocks.DIORITE, pos);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = BlockSwap.MOD_ID)
    public static void testAxisPropertyRemapping(GameTestHelper helper) {
        setupTestConfig();
        BlockPos pos = new BlockPos(1, 1, 1);

        BlockState oakLogZ = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        helper.setBlock(pos, oakLogZ);

        BlockState result = helper.getBlockState(pos);
        if (!result.is(Blocks.BIRCH_LOG)) {
            helper.fail("Expected birch_log but got " + result.getBlock(), pos);
        }
        if (result.getValue(RotatedPillarBlock.AXIS) != Direction.Axis.Z) {
            helper.fail("Expected axis Z on birch_log but got " + result.getValue(RotatedPillarBlock.AXIS), pos);
        }

        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = BlockSwap.MOD_ID)
    public static void testStairsPropertyRemapping(GameTestHelper helper) {
        setupTestConfig();
        BlockPos pos = new BlockPos(1, 1, 1);

        BlockState oakStairs = Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, Direction.SOUTH)
                .setValue(StairBlock.HALF, Half.TOP)
                .setValue(StairBlock.SHAPE, StairsShape.INNER_LEFT);

        helper.setBlock(pos, oakStairs);

        BlockState result = helper.getBlockState(pos);
        if (!result.is(Blocks.BIRCH_STAIRS)) {
            helper.fail("Expected birch_stairs but got " + result.getBlock(), pos);
        }
        if (result.getValue(StairBlock.FACING) != Direction.SOUTH) {
            helper.fail("Expected facing SOUTH but got " + result.getValue(StairBlock.FACING), pos);
        }
        if (result.getValue(StairBlock.HALF) != Half.TOP) {
            helper.fail("Expected half TOP but got " + result.getValue(StairBlock.HALF), pos);
        }
        if (result.getValue(StairBlock.SHAPE) != StairsShape.INNER_LEFT) {
            helper.fail("Expected shape INNER_LEFT but got " + result.getValue(StairBlock.SHAPE), pos);
        }

        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = BlockSwap.MOD_ID)
    public static void testWaterloggedPropertyRemapping(GameTestHelper helper) {
        setupTestConfig();
        BlockPos pos = new BlockPos(1, 1, 1);

        BlockState oakSlab = Blocks.OAK_SLAB.defaultBlockState()
                .setValue(SlabBlock.WATERLOGGED, true)
                .setValue(SlabBlock.TYPE, SlabType.BOTTOM);

        helper.setBlock(pos, oakSlab);

        BlockState result = helper.getBlockState(pos);
        if (!result.is(Blocks.BIRCH_SLAB)) {
            helper.fail("Expected birch_slab but got " + result.getBlock(), pos);
        }
        if (!result.getValue(SlabBlock.WATERLOGGED)) {
            helper.fail("Expected waterlogged=true on birch_slab", pos);
        }
        if (result.getValue(SlabBlock.TYPE) != SlabType.BOTTOM) {
            helper.fail("Expected type BOTTOM on birch_slab but got " + result.getValue(SlabBlock.TYPE), pos);
        }

        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = BlockSwap.MOD_ID)
    public static void testExplicitStateSwapper(GameTestHelper helper) {
        setupTestConfig();
        BlockPos pos = new BlockPos(1, 1, 1);

        helper.setBlock(pos, Blocks.GOLD_BLOCK);

        helper.assertBlockPresent(Blocks.EMERALD_BLOCK, pos);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = BlockSwap.MOD_ID)
    public static void testUnmappedBlockRemainsUnchanged(GameTestHelper helper) {
        setupTestConfig();
        BlockPos pos = new BlockPos(1, 1, 1);

        helper.setBlock(pos, Blocks.STONE);

        helper.assertBlockPresent(Blocks.STONE, pos);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = BlockSwap.MOD_ID)
    public static void testEmptyConfigChangesNothing(GameTestHelper helper) {
        // Regression test: empty config should not change any blocks
        BlockSwapConfig.getConfig(BlockSwapConfig.DEFAULT);
        BlockPos pos1 = new BlockPos(1, 1, 1);
        BlockPos pos2 = new BlockPos(1, 1, 2);

        helper.setBlock(pos1, Blocks.GRANITE);
        helper.assertBlockPresent(Blocks.GRANITE, pos1);

        helper.setBlock(pos2, Blocks.OAK_LOG);
        helper.assertBlockPresent(Blocks.OAK_LOG, pos2);

        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = BlockSwap.MOD_ID)
    public static void testConfigReload(GameTestHelper helper) {
        // Step 1: Initial config (granite -> diorite)
        IdentityHashMap<net.minecraft.world.level.block.Block, net.minecraft.world.level.block.Block> initialMap = new IdentityHashMap<>();
        initialMap.put(Blocks.GRANITE, Blocks.DIORITE);
        BlockSwapConfig.getConfig(new BlockSwapConfig(initialMap, new IdentityHashMap<>(), false, false));

        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Blocks.GRANITE);
        helper.assertBlockPresent(Blocks.DIORITE, pos);

        // Step 2: Reload config with new rule (cobblestone -> mossy_cobblestone)
        IdentityHashMap<net.minecraft.world.level.block.Block, net.minecraft.world.level.block.Block> reloadedMap = new IdentityHashMap<>();
        reloadedMap.put(Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE);
        BlockSwapConfig.getConfig(new BlockSwapConfig(reloadedMap, new IdentityHashMap<>(), false, false));

        // Former rule (granite -> diorite) should no longer apply
        helper.setBlock(pos, Blocks.GRANITE);
        helper.assertBlockPresent(Blocks.GRANITE, pos);

        // New rule (cobblestone -> mossy_cobblestone) should apply
        helper.setBlock(pos, Blocks.COBBLESTONE);
        helper.assertBlockPresent(Blocks.MOSSY_COBBLESTONE, pos);

        helper.succeed();
    }
}

