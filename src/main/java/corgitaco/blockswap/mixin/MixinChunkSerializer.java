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
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(ChunkSerializer.class)
public class MixinChunkSerializer {

    @Inject(method = "read", at = @At("HEAD"))
    private static void repairStates(ServerLevel level, PoiManager poiManager, RegionStorageInfo regionStorageInfo, ChunkPos pos, CompoundTag tag, CallbackInfoReturnable<ProtoChunk> cir) {
        if (tag == null || !tag.contains("sections", Tag.TAG_LIST)) {
            return;
        }

        Map<String, Block> idRemapper = MissingBlockIDsConfig.getConfig(false).idRemapper();
        if (idRemapper.isEmpty()) {
            return;
        }

        ListTag sections = tag.getList("sections", Tag.TAG_COMPOUND);
        for (int i = 0; i < sections.size(); i++) {
            CompoundTag sectionTag = sections.getCompound(i);
            if (sectionTag.contains("block_states", Tag.TAG_COMPOUND)) {
                CompoundTag blockStatesTag = sectionTag.getCompound("block_states");
                if (blockStatesTag.contains("palette", Tag.TAG_LIST)) {
                    ListTag palette = blockStatesTag.getList("palette", Tag.TAG_COMPOUND);
                    for (int j = 0; j < palette.size(); j++) {
                        CompoundTag stateTag = palette.getCompound(j);
                        String name = stateTag.getString("Name");
                        Block replacement = idRemapper.get(name);
                        if (replacement != null) {
                            stateTag.putString("Name", BuiltInRegistries.BLOCK.getKey(replacement).toString());
                        }
                    }
                }
            }
        }
    }
}
