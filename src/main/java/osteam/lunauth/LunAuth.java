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
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
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

    private Component getAuthMessage(String key, String configValue) {
        if (configValue != null && !configValue.isEmpty()) return Component.literal(configValue);
        return Component.translatable(key);
    }

    @SubscribeEvent
    public void onPlayerJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.getLevel().isClientSide) {
            String name = player.getName().getString();
            joinTimes.put(player.getUUID(), System.currentTimeMillis());
            loginAttempts.put(player.getUUID(), 0);

            player.sendSystemMessage(getAuthMessage("lunauth.welcome", Config.WELCOME_MESSAGE.get()));
            if (AuthStorage.isRegistered(name)) {
                player.sendSystemMessage(getAuthMessage("lunauth.login_prompt", Config.LOGIN_PROMPT.get()));
            } else {
                player.sendSystemMessage(getAuthMessage("lunauth.register_prompt", Config.REGISTER_PROMPT.get()));
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

    // --- БЛОКИРОВКА ЧАТА ---
    @SubscribeEvent
    public void onPlayerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (!authenticatedPlayers.contains(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§cВы не можете писать в чат до авторизации!"));
            event.setCanceled(true); // Сообщение не уйдет в чат
        }
    }

    // --- ЗАЩИТА ОТ УРОНА (И БОЙЦОВСКИЙ БАН) ---
    @SubscribeEvent
    public void onPlayerDamage(LivingIncomingDamageEvent event) {
        // Если урон получает неавторизованный игрок — отменяем
        if (event.getEntity() instanceof ServerPlayer victim) {
            if (!authenticatedPlayers.contains(victim.getUUID())) {
                event.setCanceled(true);
                return;
            }
        }

        // Если урон ПЫТАЕТСЯ нанести неавторизованный игрок — тоже отменяем
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            if (!authenticatedPlayers.contains(attacker.getUUID())) {
                attacker.sendSystemMessage(Component.literal("§cВы не можете наносить урон до авторизации!"));
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
                        serverPlayer.connection.disconnect(Component.literal("§cВремя на авторизацию истекло!"));
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
                            if (AuthStorage.isRegistered(player.getName().getString())) return 0;

                            AuthStorage.register(player.getName().getString(), StringArgumentType.getString(context, "password"));
                            authenticatedPlayers.add(uuid);
                            joinTimes.remove(uuid);
                            loginAttempts.remove(uuid);

                            context.getSource().sendSuccess(() -> getAuthMessage("lunauth.success_reg", Config.SUCCESS_REG.get()), false);
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

                                context.getSource().sendSuccess(() -> getAuthMessage("lunauth.success_login", Config.SUCCESS_LOGIN.get()), false);
                                return 1;
                            } else {
                                int attempts = loginAttempts.getOrDefault(uuid, 0) + 1;
                                if (attempts >= Config.MAX_ATTEMPTS.get()) {
                                    player.connection.disconnect(Component.literal("§cСлишком много неудачных попыток!"));
                                } else {
                                    loginAttempts.put(uuid, attempts);
                                    context.getSource().sendFailure(Component.literal("§cНеверный пароль! Попыток: " + attempts + "/" + Config.MAX_ATTEMPTS.get()));
                                }
                                return 0;
                            }
                        })));
    }
}