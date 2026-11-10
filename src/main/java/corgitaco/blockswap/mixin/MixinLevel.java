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
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class MixinLevel {

    @Inject(
            method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD")
    )
    private void beforeSetBlock(BlockPos pos, BlockState state, int flags, int recursionLeft, CallbackInfoReturnable<Boolean> cir) {
        Swapper.enterLivePlacement();
        Swapper.pushDimension(((Level) (Object) this).dimension().location());
    }

    @Inject(
            method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("RETURN")
    )
    private void afterSetBlock(BlockPos pos, BlockState state, int flags, int recursionLeft, CallbackInfoReturnable<Boolean> cir) {
        Swapper.exitLivePlacement();
        Swapper.popDimension();
    }

    @ModifyVariable(
            method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private BlockState replaceSetBlockState(BlockState state) {
        BlockSwapConfig config = BlockSwapConfig.getPlacementConfig();
        if (!config.onlySwapNaturalGeneration()) {
            ResourceLocation dim = ((Level) (Object) this).dimension().location();
            if (config.contains(state, dim)) {
                return Swapper.remapState(state, dim, config);
            }
        }
        return state;
    }
}
