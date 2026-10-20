/*
 * Copyright (c) 2026 CorgiTaco
 *
 * This file is part of Block Swap, licensed under the GNU Lesser General Public License v3.0 (LGPL-3.0).
 * You should have received a copy of the LGPL-3.0 along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.html>.
 */

package corgitaco.blockswap.mixin;

import corgitaco.blockswap.config.MissingBlockIDsConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Optional;

@Mixin(SerializableChunkData.class)
public class MixinSerializableChunkData {

    @Inject(method = "parse", at = @At("HEAD"))
    private static void repairStates(LevelHeightAccessor levelHeight, PalettedContainerFactory containerFactory, CompoundTag chunkData, CallbackInfoReturnable<SerializableChunkData> cir) {
        if (chunkData == null) {
            return;
        }

        Map<String, Block> idRemapper = MissingBlockIDsConfig.getConfig(false).idRemapper();
        if (idRemapper.isEmpty()) {
            return;
        }

        ListTag sections = chunkData.getListOrEmpty("sections");
        for (int i = 0; i < sections.size(); i++) {
            CompoundTag sectionTag = sections.getCompoundOrEmpty(i);
            Optional<CompoundTag> blockStatesTagOpt = sectionTag.getCompound("block_states");
            if (blockStatesTagOpt.isPresent()) {
                CompoundTag blockStatesTag = blockStatesTagOpt.get();
                ListTag palette = blockStatesTag.getListOrEmpty("palette");
                for (int j = 0; j < palette.size(); j++) {
                    CompoundTag stateTag = palette.getCompoundOrEmpty(j);
                    String name = stateTag.getStringOr("Name", "");
                    Block replacement = idRemapper.get(name);
                    if (replacement != null) {
                        stateTag.putString("Name", BuiltInRegistries.BLOCK.getKey(replacement).toString());
                    }
                }
            }
        }
    }
}
