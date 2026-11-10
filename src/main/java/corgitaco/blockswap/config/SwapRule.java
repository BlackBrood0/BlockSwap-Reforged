/*
 * Copyright (c) 2026 CorgiTaco
 *
 * This file is part of Block Swap, licensed under the GNU Lesser General Public License v3.0 (LGPL-3.0).
 * You should have received a copy of the LGPL-3.0 along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.html>.
 */

package corgitaco.blockswap.config;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.Set;

public record SwapRule<T>(T target, Set<ResourceLocation> dimensions) {

    public SwapRule(T target) {
        this(target, Collections.emptySet());
    }

    public boolean appliesTo(ResourceLocation dimension) {
        if (dimensions == null || dimensions.isEmpty()) {
            return true;
        }
        if (dimension == null) {
            return false;
        }
        return dimensions.contains(dimension);
    }
}
