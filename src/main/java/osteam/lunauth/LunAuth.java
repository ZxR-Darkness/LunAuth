package osteam.lunauth;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;

import java.util.*;

@Mod(LunAuth.MODID)
public class LunAuth {
    public static final String MODID = "lunauth";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final Set<UUID> authenticatedPlayers = new HashSet<>();

    private final Map<UUID, Long> joinTimes = new HashMap<>();
    private final Map<UUID, Integer> loginAttempts = new HashMap<>();

    public LunAuth(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON,
                Config.SPEC, "LunAuth/settings.toml");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        AuthStorage.load();
        LOGGER.info("LunAuth: Система защиты O.S. TEAM запущена!");
    }

    // Универсальный метод для получения сообщений из конфига
    private Component getAuthMessage(String configValue) {
        return Component.literal(configValue);
    }

    @SubscribeEvent
    public void onPlayerJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.getLevel().isClientSide) {
            if (authenticatedPlayers.contains(player.getUUID())) {
                return;
            }

            String name = player.getName().getString();
            joinTimes.put(player.getUUID(), System.currentTimeMillis());
            loginAttempts.put(player.getUUID(), 0);

            player.sendSystemMessage(getAuthMessage(Config.WELCOME_MESSAGE.get()));
            if (AuthStorage.isRegistered(name)) {
                player.sendSystemMessage(getAuthMessage(Config.LOGIN_PROMPT.get()));
            } else {
                player.sendSystemMessage(getAuthMessage(Config.REGISTER_PROMPT.get()));
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        authenticatedPlayers.remove(uuid);
        joinTimes.remove(uuid);
        loginAttempts.remove(uuid);
    }

    @SubscribeEvent
    public void onPlayerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (!authenticatedPlayers.contains(player.getUUID())) {
            player.sendSystemMessage(getAuthMessage(Config.NO_CHAT.get()));
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onItemToss(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            if (!authenticatedPlayers.contains(player.getUUID())) {
                player.getInventory().add(event.getEntity().getItem());
                player.sendSystemMessage(getAuthMessage(Config.NO_DROP.get()));
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onContainerOpen(PlayerContainerEvent.Open event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!authenticatedPlayers.contains(player.getUUID())) {
                player.closeContainer();
            }
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!authenticatedPlayers.contains(event.getEntity().getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onPlayerDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer victim) {
            if (!authenticatedPlayers.contains(victim.getUUID())) {
                event.setCanceled(true);
                return;
            }
        }

        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            if (!authenticatedPlayers.contains(attacker.getUUID())) {
                attacker.sendSystemMessage(getAuthMessage(Config.NO_DAMAGE.get()));
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            UUID uuid = serverPlayer.getUUID();

            if (!authenticatedPlayers.contains(uuid)) {
                Long joinTime = joinTimes.get(uuid);
                if (joinTime != null) {
                    long secondsPassed = (System.currentTimeMillis() - joinTime) / 1000;
                    if (secondsPassed > Config.AUTH_TIMEOUT.get()) {
                        serverPlayer.connection.disconnect(getAuthMessage(Config.TIMEOUT_KICK.get()));
                        return;
                    }
                }

                if (Config.USE_BLINDNESS.get()) {
                    serverPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.BLINDNESS, 60, 255, false, false));
                }
                serverPlayer.setDeltaMovement(0, 0, 0);
                serverPlayer.connection.teleport(serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(), serverPlayer.getYRot(), serverPlayer.getXRot());
            }
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("register")
                .then(Commands.argument("password", StringArgumentType.string())
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            UUID uuid = player.getUUID();
                            if (AuthStorage.isRegistered(player.getName().getString())) {
                                context.getSource().sendFailure(getAuthMessage(Config.ALREADY_REGISTERED.get()));
                                return 0;
                            }

                            AuthStorage.register(player.getName().getString(), StringArgumentType.getString(context, "password"));
                            authenticatedPlayers.add(uuid);
                            joinTimes.remove(uuid);
                            loginAttempts.remove(uuid);

                            context.getSource().sendSuccess(() -> getAuthMessage(Config.SUCCESS_REG.get()), false);
                            return 1;
                        })));

        dispatcher.register(Commands.literal("login")
                .then(Commands.argument("password", StringArgumentType.string())
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            String name = player.getName().getString();
                            UUID uuid = player.getUUID();

                            if (AuthStorage.checkPassword(name, StringArgumentType.getString(context, "password"))) {
                                authenticatedPlayers.add(uuid);
                                joinTimes.remove(uuid);
                                loginAttempts.remove(uuid);

                                context.getSource().sendSuccess(() -> getAuthMessage(Config.SUCCESS_LOGIN.get()), false);
                                return 1;
                            } else {
                                int attempts = loginAttempts.getOrDefault(uuid, 0) + 1;
                                if (attempts >= Config.MAX_ATTEMPTS.get()) {
                                    player.connection.disconnect(getAuthMessage(Config.TOO_MANY_ATTEMPTS.get()));
                                } else {
                                    loginAttempts.put(uuid, attempts);
                                    String msg = Config.WRONG_PASS.get().replace("{attempts}", String.valueOf(attempts)).replace("{max}", String.valueOf(Config.MAX_ATTEMPTS.get()));
                                    context.getSource().sendFailure(getAuthMessage(msg));
                                }
                                return 0;
                            }
                        })));
    }
}