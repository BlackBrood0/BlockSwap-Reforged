/*
 * Copyright (c) 2026 CorgiTaco
 *
 * This file is part of Block Swap, licensed under the GNU Lesser General Public License v3.0 (LGPL-3.0).
 * You should have received a copy of the LGPL-3.0 along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.html>.
 */

package corgitaco.blockswap.mixin;

import corgitaco.blockswap.util.DimensionHolder;
import corgitaco.blockswap.util.TickHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class MixinLevelChunk implements TickHelper {
    @Unique
    private boolean blockSwap$isTickDirty;

    @Override
    public boolean markTickDirty() {
        return blockSwap$isTickDirty;
    }

    @Override
    public void setTickDirty() {
        this.blockSwap$isTickDirty = true;
    }

    @Inject(method = "setBlockState", at = @At("HEAD"))
    private void beforeLevelChunkSetBlockState(BlockPos pos, BlockState state, boolean isMoving, CallbackInfoReturnable<BlockState> cir) {
        LevelChunk chunk = (LevelChunk) (Object) this;
        if (chunk.getLevel() != null) {
            ResourceLocation dim = chunk.getLevel().dimension().location();
            if (((DimensionHolder) chunk).blockswap$getDimension() == null) {
                ((DimensionHolder) chunk).blockswap$setDimension(dim);
            }
            int sectionIndex = chunk.getSectionIndex(pos.getY());
            LevelChunkSection[] sections = chunk.getSections();
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
