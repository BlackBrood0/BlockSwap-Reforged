/*
 * Copyright (c) 2026 CorgiTaco
 *
 * This file is part of Block Swap, licensed under the GNU Lesser General Public License v3.0 (LGPL-3.0).
 * You should have received a copy of the LGPL-3.0 along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.html>.
 */

package corgitaco.blockswap;

import com.mojang.brigadier.Command;
import corgitaco.blockswap.config.BlockSwapConfig;
import corgitaco.blockswap.config.MissingBlockIDsConfig;
import corgitaco.blockswap.gametest.BlockSwapGameTests;
import corgitaco.blockswap.network.ClientConfigSyncPayload;
import corgitaco.blockswap.swapper.Swapper;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

@Mod(BlockSwap.MOD_ID)
public class BlockSwap {
    public static final String MOD_ID = "blockswap";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static Path CONFIG_PATH;

    public BlockSwap(IEventBus modEventBus, ModContainer modContainer) {
        CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve(MOD_ID);

        // Register mod lifecycle listeners
        modEventBus.addListener(this::registerPayloadHandlers);
        modEventBus.addListener(this::registerGameTests);

        // Register game lifecycle listeners
        NeoForge.EVENT_BUS.addListener(this::onServerAboutToStart);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(this::onDatapackSync);
        NeoForge.EVENT_BUS.addListener(this::onServerTickPre);
        NeoForge.EVENT_BUS.addListener(this::onTagsUpdated);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);

        // Initial config read
        BlockSwapConfig config = BlockSwapConfig.getConfig(true);
        MissingBlockIDsConfig.getConfig(true);
        LOGGER.info("BlockSwap initial configuration loaded: onlySwapNaturalGeneration = {}", config.onlySwapNaturalGeneration());
    }

    private void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("blockswap")
                        .then(Commands.literal("reload")
                                .requires(source -> source.hasPermission(2) || !source.getServer().isDedicatedServer())
                                .executes(context -> {
                                    BlockSwapConfig.reloadPlacementConfig();
                                    BlockSwapConfig updated = BlockSwapConfig.getPlacementConfig();
                                    String json = updated.toJsonString();
                                    PacketDistributor.sendToAllPlayers(new ClientConfigSyncPayload(json));
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("[BlockSwap] Placement swap conditions reloaded. World generation conditions remain unchanged.")
                                                    .withStyle(ChatFormatting.GREEN),
                                            true
                                    );
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
        );
    }

    private void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.shouldUpdateStaticData()) {
            LOGGER.info("Tags updated, resolving BlockSwap tag mappings");
            BlockSwapConfig currentPlacement = BlockSwapConfig.getPlacementConfig();
            if (currentPlacement != null && !currentPlacement.tagRules().isEmpty()) {
                BlockSwapConfig resolved = currentPlacement.resolveTags();
                BlockSwapConfig.setPlacementConfig(resolved);
                LOGGER.info("BlockSwap placement tag mappings resolved: {} total block swaps active", resolved.blockBlockMap().size());
            }
            BlockSwapConfig currentWorldGen = BlockSwapConfig.getWorldGenConfig();
            if (currentWorldGen != null && !currentWorldGen.tagRules().isEmpty()) {
                BlockSwapConfig resolved = currentWorldGen.resolveTags();
                BlockSwapConfig.setWorldGenConfig(resolved);
                LOGGER.info("BlockSwap worldgen tag mappings resolved: {} total block swaps active", resolved.blockBlockMap().size());
            }
        }
    }

    private void onServerTickPre(net.neoforged.neoforge.event.tick.ServerTickEvent.Pre event) {
        Swapper.resetLivePlacement();
    }

    private void registerPayloadHandlers(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(MOD_ID);
        registrar.playToClient(
                ClientConfigSyncPayload.TYPE,
                ClientConfigSyncPayload.STREAM_CODEC,
                ClientConfigSyncPayload::handle
        );
    }

    private void registerGameTests(final RegisterGameTestsEvent event) {
        event.register(BlockSwapGameTests.class);
        event.register(corgitaco.blockswap.gametest.BlockSwapBenchmark.class);
    }

    private void onServerAboutToStart(ServerAboutToStartEvent event) {
        LOGGER.info("Reloading BlockSwap configuration on world load");
        BlockSwapConfig config = BlockSwapConfig.getConfig(true).resolveTags();
        BlockSwapConfig.setWorldGenConfig(config);
        BlockSwapConfig.setPlacementConfig(config);
        MissingBlockIDsConfig.getConfig(true);
        LOGGER.info("BlockSwap configuration loaded: {} block swaps active, onlySwapNaturalGeneration = {}",
                config.blockBlockMap().size(), config.onlySwapNaturalGeneration());
        Swapper.generateAllKnownStates(CONFIG_PATH);
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncConfigToPlayer(serverPlayer);
        }
    }

    private void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            syncConfigToPlayer(event.getPlayer());
        } else {
            BlockSwapConfig config = BlockSwapConfig.getConfig(true).resolveTags();
            BlockSwapConfig.setPlacementConfig(config);
            MissingBlockIDsConfig.getConfig(true);
            LOGGER.info("BlockSwap placement configuration reloaded via datapack sync: {} block swaps active, onlySwapNaturalGeneration = {}",
                    config.blockBlockMap().size(), config.onlySwapNaturalGeneration());
            String json = config.toJsonString();
            PacketDistributor.sendToAllPlayers(new ClientConfigSyncPayload(json));
        }
    }

    private void syncConfigToPlayer(ServerPlayer player) {
        String json = BlockSwapConfig.getPlacementConfig().toJsonString();
        PacketDistributor.sendToPlayer(player, new ClientConfigSyncPayload(json));
    }
}
