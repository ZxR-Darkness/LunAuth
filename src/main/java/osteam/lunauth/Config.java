package osteam.lunauth;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Секция настроек функций
    public static final ModConfigSpec.IntValue AUTH_TIMEOUT = BUILDER
            .comment("Время на авторизацию в секундах.")
            .defineInRange("features.auth_timeout", 60, 10, 300);

    public static final ModConfigSpec.IntValue MAX_ATTEMPTS = BUILDER
            .comment("Максимальное количество попыток ввода пароля.")
            .defineInRange("features.max_attempts", 3, 1, 10);

    public static final ModConfigSpec.BooleanValue USE_BLINDNESS = BUILDER
            .comment("Применять эффект слепоты к неавторизованным игрокам?")
            .define("features.use_blindness", true);

    // Секция основных сообщений
    public static final ModConfigSpec.ConfigValue<String> WELCOME_MESSAGE = BUILDER
            .define("messages.welcome", "§6[LunAuth] §fДобро пожаловать на сервер!");

    public static final ModConfigSpec.ConfigValue<String> REGISTER_PROMPT = BUILDER
            .define("messages.register_prompt", "§fПожалуйста, используйте: §a/register <пароль>");

    public static final ModConfigSpec.ConfigValue<String> LOGIN_PROMPT = BUILDER
            .define("messages.login_prompt", "§fПожалуйста, используйте: §a/login <пароль>");

    public static final ModConfigSpec.ConfigValue<String> SUCCESS_REG = BUILDER
            .define("messages.success_reg", "§aВы успешно зарегистрировались!");

    public static final ModConfigSpec.ConfigValue<String> SUCCESS_LOGIN = BUILDER
            .define("messages.success_login", "§aВы успешно вошли в систему!");

    public static final ModConfigSpec.ConfigValue<String> WRONG_PASS = BUILDER
            .comment("Сообщение о неверном пароле. {attempts} и {max} заменятся на числа.")
            .define("messages.wrong_password", "§cНеверный пароль! Попыток: {attempts}/{max}");

    public static final ModConfigSpec.ConfigValue<String> ALREADY_REGISTERED = BUILDER
            .define("messages.already_registered", "§cВы уже зарегистрированы!");

    // Секция блокировок
    public static final ModConfigSpec.ConfigValue<String> NO_CHAT = BUILDER
            .define("messages.no_chat", "§cВы не можете писать в чат до авторизации!");

    public static final ModConfigSpec.ConfigValue<String> NO_DROP = BUILDER
            .define("messages.no_drop", "§cВы не можете выбрасывать предметы до авторизации!");

    public static final ModConfigSpec.ConfigValue<String> NO_DAMAGE = BUILDER
            .define("messages.no_damage", "§cВы не можете наносить урон до авторизации!");

    // Секция киков
    public static final ModConfigSpec.ConfigValue<String> TIMEOUT_KICK = BUILDER
            .define("messages.timeout_kick", "§cВремя на авторизацию истекло!");

    public static final ModConfigSpec.ConfigValue<String> TOO_MANY_ATTEMPTS = BUILDER
            .define("messages.too_many_attempts", "§cСлишком много неудачных попыток!");

    public static final ModConfigSpec SPEC = BUILDER.build();
}