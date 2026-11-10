/*
 * Copyright (c) 2026 CorgiTaco
 *
 * This file is part of Block Swap, licensed under the GNU Lesser General Public License v3.0 (LGPL-3.0).
 * You should have received a copy of the LGPL-3.0 along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.html>.
 */

package corgitaco.blockswap.mixin;

import corgitaco.blockswap.swapper.Swapper;
import corgitaco.blockswap.util.DimensionHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(ChunkStep.class)
public class MixinChunkStep {

    @Inject(method = "apply", at = @At("HEAD"))
    private void beforeApply(WorldGenContext context, StaticCache2D<GenerationChunkHolder> cache, ChunkAccess chunk, CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir) {
        if (context != null && context.level() != null) {
            ResourceLocation dim = context.level().dimension().location();
            if (chunk instanceof DimensionHolder holder) {
                holder.blockswap$setDimension(dim);
            }
            Swapper.setCurrentDimension(dim);
        }
    }
}
