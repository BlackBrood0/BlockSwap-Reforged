/*
 * Copyright (c) 2026 CorgiTaco
 *
 * This file is part of Block Swap, licensed under the GNU Lesser General Public License v3.0 (LGPL-3.0).
 * You should have received a copy of the LGPL-3.0 along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.html>.
 */

package corgitaco.blockswap.mixin;

import corgitaco.blockswap.util.DimensionHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ProtoChunk.class)
public abstract class MixinProtoChunk {

    @Inject(method = "setBlockState", at = @At("HEAD"))
    private void beforeProtoSetBlockState(BlockPos pos, BlockState state, boolean isMoving, CallbackInfoReturnable<BlockState> cir) {
        ProtoChunk proto = (ProtoChunk) (Object) this;
        ResourceLocation dim = ((DimensionHolder) proto).blockswap$getDimension();
        if (dim != null) {
            int sectionIndex = proto.getSectionIndex(pos.getY());
            LevelChunkSection[] sections = proto.getSections();
            if (sections != null && sectionIndex >= 0 && sectionIndex < sections.length) {
                LevelChunkSection section = sections[sectionIndex];
                if (section instanceof DimensionHolder sectionHolder) {
                    if (sectionHolder.blockswap$getDimension() == null) {
                        sectionHolder.blockswap$setDimension(dim);
                    }
                }
            }
        }
    }
}
