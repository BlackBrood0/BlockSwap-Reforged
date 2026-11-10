/*
 * Copyright (c) 2026 CorgiTaco
 *
 * This file is part of Block Swap, licensed under the GNU Lesser General Public License v3.0 (LGPL-3.0).
 * You should have received a copy of the LGPL-3.0 along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.html>.
 */

package corgitaco.blockswap.mixin;

import corgitaco.blockswap.config.BlockSwapConfig;
import corgitaco.blockswap.swapper.Swapper;
import corgitaco.blockswap.util.DimensionHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LevelChunkSection.class)
public abstract class MixinChunkSection implements DimensionHolder {
    @Unique
    private ResourceLocation blockswap$dimension;

    @Override
    public ResourceLocation blockswap$getDimension() {
        return this.blockswap$dimension;
    }

    @Override
    public void blockswap$setDimension(ResourceLocation dimension) {
        this.blockswap$dimension = dimension;
    }

    @ModifyVariable(
            method = "setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private BlockState replaceState(BlockState state) {
        boolean live = Swapper.isLivePlacement();
        BlockSwapConfig config = live ? BlockSwapConfig.getPlacementConfig() : BlockSwapConfig.getWorldGenConfig();

        if (live && config.onlySwapNaturalGeneration()) {
            return state;
        }

        ResourceLocation dim = this.blockswap$dimension;
        if (dim == null) {
            dim = Swapper.getCurrentDimension();
        }

        if (!config.retroGen() && config.contains(state, dim)) {
            return Swapper.remapState(state, dim, config);
        }
        return state;
    }
}
