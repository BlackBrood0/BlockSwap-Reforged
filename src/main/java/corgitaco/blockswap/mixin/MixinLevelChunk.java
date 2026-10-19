/*
 * Copyright (c) 2026 CorgiTaco
 *
 * This file is part of Block Swap, licensed under the GNU Lesser General Public License v3.0 (LGPL-3.0).
 * You should have received a copy of the LGPL-3.0 along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.html>.
 */

package corgitaco.blockswap.mixin;

import corgitaco.blockswap.util.TickHelper;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

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
}
