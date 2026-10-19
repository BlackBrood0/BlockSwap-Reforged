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
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Swapper {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static BlockState remapState(BlockState incomingState) {
        BlockSwapConfig config = BlockSwapConfig.getConfig(false);

        // 1. Explicit block state mapping takes precedence
        if (config.blockStateBlockStateMap().containsKey(incomingState)) {
            return config.blockStateBlockStateMap().get(incomingState);
        }

        // 2. Block-to-block mapping with property transfer
        Block targetBlock = config.blockBlockMap().get(incomingState.getBlock());
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
        BlockSwapConfig config = BlockSwapConfig.getConfig(false);
        if (config.retroGen() && chunk instanceof TickHelper tickHelper) {
            if (!tickHelper.markTickDirty()) {
                Level world = chunk.getLevel();
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
                                    if (config.contains(state)) {
                                        world.setBlock(blockPos, remapState(state), 2);
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
            if (!Files.exists(filePath)) {
                try {
                    Files.createDirectories(filePath.getParent());
                    JsonElement json = BlockState.CODEC.listOf().encodeStart(JsonOps.INSTANCE, states)
                            .getOrThrow(IllegalStateException::new);
                    Files.writeString(filePath, GSON.toJson(json));
                } catch (IOException | IllegalStateException e) {
                    BlockSwap.LOGGER.error("Failed to generate known states for block {}", key, e);
                }
            }
        });
    }
}
