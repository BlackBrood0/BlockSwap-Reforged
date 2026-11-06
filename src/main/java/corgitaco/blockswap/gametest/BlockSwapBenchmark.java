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
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.IdentityHashMap;

public class BlockSwapBenchmark {

    private static BlockSwapConfig createRealisticConfig(boolean naturalOnly) {
        IdentityHashMap<Block, Block> blockMap = new IdentityHashMap<>();
        blockMap.put(Blocks.GRANITE, Blocks.DIORITE);
        blockMap.put(Blocks.OAK_LOG, Blocks.BIRCH_LOG);
        blockMap.put(Blocks.OAK_STAIRS, Blocks.BIRCH_STAIRS);
        return new BlockSwapConfig(blockMap, new IdentityHashMap<>(), false, false, naturalOnly);
    }

    private static BlockSwapConfig createEmptyConfig(boolean naturalOnly) {
        return new BlockSwapConfig(new IdentityHashMap<>(), new IdentityHashMap<>(), false, false, naturalOnly);
    }

    @GameTest(template = "empty", templateNamespace = BlockSwap.MOD_ID, batch = "benchmarkBatch", timeoutTicks = 6000)
    public static void runAllBenchmarks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos testPos = helper.absolutePos(new BlockPos(1, 1, 1));
        StringBuilder report = new StringBuilder();
        report.append("================ BLOCKSWAP BENCHMARK RUN ================\n");

        // We benchmark Option B (naturalOnly = false) and Option C (naturalOnly = true)
        runSuiteForMode(level, testPos, false, "Option B (onlySwapNaturalGeneration = false)", report, 2000);
        runSuiteForMode(level, testPos, true, "Option C (onlySwapNaturalGeneration = true)", report, 5000);

        report.append("=========================================================\n");
        String finalOutput = report.toString();
        System.out.println(finalOutput);
        BlockSwap.LOGGER.info(finalOutput);

        try {
            Path outPath = Path.of("benchmark_run_results.txt");
            Files.writeString(outPath, finalOutput, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            BlockSwap.LOGGER.error("Failed to write benchmark results file", e);
        }

        helper.succeed();
    }

    private static void runSuiteForMode(ServerLevel level, BlockPos testPos, boolean naturalOnly, String modeName, StringBuilder sb, int chunkCoordBase) {
        sb.append("\n--- ").append(modeName).append(" ---\n");

        // 1. WorldGen Benchmark with Realistic Config
        BlockSwapConfig.getConfig(createRealisticConfig(naturalOnly));
        // Warmup: 25 chunks
        generateChunks(level, chunkCoordBase, 0, 5, 5);
        double[] worldgenRealisticTimes = new double[5];
        for (int rep = 0; rep < 5; rep++) {
            long start = System.nanoTime();
            generateChunks(level, chunkCoordBase + 100 + (rep * 20), 0, 10, 5); // 50 chunks
            long elapsed = System.nanoTime() - start;
            worldgenRealisticTimes[rep] = elapsed / 1_000_000.0;
        }
        appendStats(sb, "WorldGen (Realistic Swaps, 50 chunks)", worldgenRealisticTimes);

        // 2. WorldGen Benchmark with Empty Config
        BlockSwapConfig.getConfig(createEmptyConfig(naturalOnly));
        generateChunks(level, chunkCoordBase, 1000, 5, 5);
        double[] worldgenEmptyTimes = new double[5];
        for (int rep = 0; rep < 5; rep++) {
            long start = System.nanoTime();
            generateChunks(level, chunkCoordBase + 100 + (rep * 20), 1000, 10, 5); // 50 chunks
            long elapsed = System.nanoTime() - start;
            worldgenEmptyTimes[rep] = elapsed / 1_000_000.0;
        }
        appendStats(sb, "WorldGen (Empty Swaps, 50 chunks)", worldgenEmptyTimes);

        // 3. Live Placement Benchmark (Matching block: Granite, 100,000 placements)
        BlockSwapConfig.getConfig(createRealisticConfig(naturalOnly));
        BlockState granite = Blocks.GRANITE.defaultBlockState();
        BlockState oakLog = Blocks.OAK_LOG.defaultBlockState();
        // Warmup: 20,000 placements
        for (int i = 0; i < 20_000; i++) {
            level.setBlock(testPos, (i & 1) == 0 ? granite : oakLog, 2);
        }
        double[] liveMatchingTimes = new double[5];
        for (int rep = 0; rep < 5; rep++) {
            long start = System.nanoTime();
            for (int i = 0; i < 100_000; i++) {
                level.setBlock(testPos, (i & 1) == 0 ? granite : oakLog, 2);
            }
            long elapsed = System.nanoTime() - start;
            liveMatchingTimes[rep] = elapsed / 1_000_000.0;
        }
        appendStats(sb, "Live Placement (Matching Rules, 100,000 blocks)", liveMatchingTimes);

        // 4. Live Placement Benchmark (Non-matching block: Stone, 100,000 placements)
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        // Warmup: 20,000 placements
        for (int i = 0; i < 20_000; i++) {
            level.setBlock(testPos, (i & 1) == 0 ? stone : dirt, 2);
        }
        double[] liveNonMatchingTimes = new double[5];
        for (int rep = 0; rep < 5; rep++) {
            long start = System.nanoTime();
            for (int i = 0; i < 100_000; i++) {
                level.setBlock(testPos, (i & 1) == 0 ? stone : dirt, 2);
            }
            long elapsed = System.nanoTime() - start;
            liveNonMatchingTimes[rep] = elapsed / 1_000_000.0;
        }
        appendStats(sb, "Live Placement (Non-matching Rules, 100,000 blocks)", liveNonMatchingTimes);
    }

    private static void generateChunks(ServerLevel level, int startChunkX, int startChunkZ, int sizeX, int sizeZ) {
        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                level.getChunkSource().getChunk(startChunkX + x, startChunkZ + z, ChunkStatus.FULL, true);
            }
        }
    }

    private static void appendStats(StringBuilder sb, String benchmarkName, double[] timesMs) {
        double[] sorted = Arrays.copyOf(timesMs, timesMs.length);
        Arrays.sort(sorted);
        double median = sorted[sorted.length / 2];
        double min = sorted[0];
        double max = sorted[sorted.length - 1];
        double spread = max - min;
        sb.append(String.format("  %-48s : Median = %7.2f ms | Spread = [%6.2f ms - %6.2f ms] (Delta: %5.2f ms) | All: %s\n",
                benchmarkName, median, min, max, spread, Arrays.toString(timesMs)));
    }
}
