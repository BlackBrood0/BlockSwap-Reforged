/*
 * Copyright (c) 2026 CorgiTaco
 *
 * This file is part of Block Swap, licensed under the GNU Lesser General Public License v3.0 (LGPL-3.0).
 * You should have received a copy of the LGPL-3.0 along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.html>.
 */

package corgitaco.blockswap.swapper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import corgitaco.blockswap.BlockSwap;
import corgitaco.blockswap.config.BlockSwapConfig;
import corgitaco.blockswap.util.TickHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Swapper {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ThreadLocal<int[]> LIVE_DEPTH = ThreadLocal.withInitial(() -> new int[1]);
    private static final ThreadLocal<ArrayDeque<ResourceLocation>> DIMENSION_STACK = ThreadLocal.withInitial(ArrayDeque::new);

    public static void enterLivePlacement() {
        LIVE_DEPTH.get()[0]++;
    }

    public static void exitLivePlacement() {
        int[] depth = LIVE_DEPTH.get();
        if (depth[0] > 0) {
            depth[0]--;
        }
    }

    public static boolean isLivePlacement() {
        return LIVE_DEPTH.get()[0] > 0;
    }

    public static void resetLivePlacement() {
        LIVE_DEPTH.get()[0] = 0;
        clearCurrentDimension();
    }

    public static void pushDimension(ResourceLocation dimension) {
        if (dimension != null) {
            DIMENSION_STACK.get().push(dimension);
        }
    }

    public static void popDimension() {
        ArrayDeque<ResourceLocation> stack = DIMENSION_STACK.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    public static ResourceLocation getCurrentDimension() {
        ArrayDeque<ResourceLocation> stack = DIMENSION_STACK.get();
        return stack.isEmpty() ? null : stack.peek();
    }

    public static void setCurrentDimension(ResourceLocation dimension) {
        ArrayDeque<ResourceLocation> stack = DIMENSION_STACK.get();
        stack.clear();
        if (dimension != null) {
            stack.push(dimension);
        }
    }

    public static void clearCurrentDimension() {
        DIMENSION_STACK.get().clear();
    }

    public static BlockState remapState(BlockState incomingState, ResourceLocation currentDimension, BlockSwapConfig config) {
        // 1. Explicit block state mapping takes precedence
        BlockState stateTarget = config.getTargetState(incomingState, currentDimension);
        if (stateTarget != null) {
            return stateTarget;
        }

        // 2. Block-to-block mapping with property transfer
        Block targetBlock = config.getTargetBlock(incomingState.getBlock(), currentDimension);
        if (targetBlock == null) {
            return incomingState;
        }

        BlockState newState = targetBlock.defaultBlockState();

        for (Property<?> incomingProperty : incomingState.getProperties()) {
            Property<?> targetProperty = targetBlock.getStateDefinition().getProperty(incomingProperty.getName());
            if (targetProperty != null && targetProperty.getValueClass().equals(incomingProperty.getValueClass())) {
                newState = copyPropertyValue(incomingState, incomingProperty, targetProperty, newState);
            }
        }

        return newState;
    }

    public static BlockState remapState(BlockState incomingState, ResourceLocation currentDimension) {
        return remapState(incomingState, currentDimension, BlockSwapConfig.getConfig(false));
    }

    public static BlockState remapState(BlockState incomingState) {
        return remapState(incomingState, getCurrentDimension(), BlockSwapConfig.getConfig(false));
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState copyPropertyValue(
            BlockState incomingState,
            Property<T> incomingProperty,
            Property<?> targetProperty,
            BlockState targetState
    ) {
        try {
            Property<T> typedTargetProperty = (Property<T>) targetProperty;
            T value = incomingState.getValue(incomingProperty);
            if (typedTargetProperty.getPossibleValues().contains(value)) {
                return targetState.setValue(typedTargetProperty, value);
            }
        } catch (Exception ignored) {
        }
        return targetState;
    }

    public static void runRetroGenerator(LevelChunk chunk) {
        BlockSwapConfig config = BlockSwapConfig.getWorldGenConfig();
        if (config.retroGen() && chunk instanceof TickHelper tickHelper) {
            if (!tickHelper.markTickDirty()) {
                Level world = chunk.getLevel();
                ResourceLocation dim = world != null ? world.dimension().location() : null;
                LevelChunkSection[] sections = chunk.getSections();
                for (int i = 0; i < sections.length; i++) {
                    LevelChunkSection section = sections[i];
                    if (section != null && !section.hasOnlyAir()) {
                        int bottomY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(i));
                        for (int x = 0; x < 16; x++) {
                            for (int y = 0; y < 16; y++) {
                                for (int z = 0; z < 16; z++) {
                                    BlockPos blockPos = new BlockPos(
                                            SectionPos.sectionToBlockCoord(chunk.getPos().x) + x,
                                            bottomY + y,
                                            SectionPos.sectionToBlockCoord(chunk.getPos().z) + z
                                    );
                                    BlockState state = world.getBlockState(blockPos);
                                    if (config.contains(state, dim)) {
                                        world.setBlock(blockPos, remapState(state, dim, config), 2);
                                    }
                                }
                            }
                        }
                    }
                }
                tickHelper.setTickDirty();
            }
        }
    }

    public static void generateAllKnownStates(Path configDir) {
        BlockSwapConfig config = BlockSwapConfig.getConfig(false);
        if (!config.generateAllKnownStates()) {
            return;
        }

        Map<Block, List<BlockState>> allKnownStates = new TreeMap<>(Comparator.comparing(b -> BuiltInRegistries.BLOCK.getKey(b).toString()));
        for (Block block : BuiltInRegistries.BLOCK) {
            allKnownStates.put(block, block.getStateDefinition().getPossibleStates());
        }

        Path knownStatesDir = configDir.resolve("known_states");
        allKnownStates.forEach((block, states) -> {
            ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
            Path filePath = knownStatesDir.resolve(key.getNamespace()).resolve(key.getPath() + ".json5");
            try {
                Files.createDirectories(filePath.getParent());
                com.google.gson.JsonArray stateArray = new com.google.gson.JsonArray();
                for (BlockState state : states) {
                    BlockState.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, state)
                            .result()
                            .ifPresent(stateArray::add);
                }
                Files.writeString(filePath, GSON.toJson(stateArray));
            } catch (IOException e) {
                BlockSwap.LOGGER.error("Failed to generate known states for {}", key, e);
            }
        });
    }
}
