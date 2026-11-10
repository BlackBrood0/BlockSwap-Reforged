/*
 * Copyright (c) 2026 CorgiTaco
 *
 * This file is part of Block Swap, licensed under the GNU Lesser General Public License v3.0 (LGPL-3.0).
 * You should have received a copy of the LGPL-3.0 along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.html>.
 */

package corgitaco.blockswap.mixin;

import corgitaco.blockswap.util.DimensionHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkAccess.class)
public abstract class MixinChunkAccess implements DimensionHolder {
    @Shadow public abstract LevelChunkSection[] getSections();

    @Unique
    private ResourceLocation blockswap$dimension;

    @Override
    public ResourceLocation blockswap$getDimension() {
        return this.blockswap$dimension;
    }

    @Override
    public void blockswap$setDimension(ResourceLocation dimension) {
        this.blockswap$dimension = dimension;
        LevelChunkSection[] sections = getSections();
        if (sections != null) {
            for (LevelChunkSection section : sections) {
                if (section instanceof DimensionHolder sectionHolder) {
                    sectionHolder.blockswap$setDimension(dimension);
                }
            }
        }
    }

    @Inject(method = "getSection", at = @At("RETURN"))
    private void onGetSection(int index, CallbackInfoReturnable<LevelChunkSection> cir) {
        LevelChunkSection section = cir.getReturnValue();
        if (section instanceof DimensionHolder sectionHolder && sectionHolder.blockswap$getDimension() == null) {
            ResourceLocation dim = this.blockswap$dimension;
            if (dim == null && (Object) this instanceof LevelChunk levelChunk && levelChunk.getLevel() != null) {
                dim = levelChunk.getLevel().dimension().location();
                this.blockswap$dimension = dim;
            }
            if (dim != null) {
                sectionHolder.blockswap$setDimension(dim);
            }
        }
    }
}
