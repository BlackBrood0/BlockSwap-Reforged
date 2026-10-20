/*
 * Copyright (c) 2026 CorgiTaco
 *
 * This file is part of Block Swap, licensed under the GNU Lesser General Public License v3.0 (LGPL-3.0).
 * You should have received a copy of the LGPL-3.0 along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.html>.
 */

package corgitaco.blockswap.network;

import corgitaco.blockswap.BlockSwap;
import corgitaco.blockswap.config.BlockSwapConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClientConfigSyncPayload(String configJson) implements CustomPacketPayload {
    public static final Type<ClientConfigSyncPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(BlockSwap.MOD_ID, "config_sync"));

    public static final StreamCodec<FriendlyByteBuf, ClientConfigSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            ClientConfigSyncPayload::configJson,
            ClientConfigSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClientConfigSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            BlockSwap.LOGGER.info("Received server BlockSwap configuration");
            BlockSwapConfig.getConfig(BlockSwapConfig.parseJsonString(payload.configJson()));
        });
    }
}
