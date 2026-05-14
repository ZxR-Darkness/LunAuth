package osteam.lunauth;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // секция настроек
    public static final ModConfigSpec.IntValue AUTH_TIMEOUT = BUILDER
            .comment("Время на авторизацию в секундах.")
            .defineInRange("features.auth_timeout", 60, 10, 300);

    public static final ModConfigSpec.IntValue MAX_ATTEMPTS = BUILDER
            .comment("Максимальное количество попыток ввода пароля.")
            .defineInRange("features.max_attempts", 3, 1, 10);

    // Настройки локализации
    public static final ModConfigSpec.ConfigValue<String> LANGUAGE = BUILDER
            .comment("Server message language. Use 'en_us' or 'ru_ru'.")
            .define("general.language", "en_us");

    // Секция сообщений
    public static final ModConfigSpec.ConfigValue<String> WELCOME_MESSAGE = BUILDER
            .comment("Entry message. Leave empty \"\" to use auto-translation.")
            .define("messages.welcome", "§6[LunAuth] §fWelcome to the server!");

    public static final ModConfigSpec.ConfigValue<String> REGISTER_PROMPT = BUILDER
            .comment("Registration prompt.")
            .define("messages.register_prompt", "§fPlease use: §a/register <password>");

    public static final ModConfigSpec.ConfigValue<String> LOGIN_PROMPT = BUILDER
            .comment("Login prompt.")
            .define("messages.login_prompt", "§fPlease use: §a/login <password>");

    public static final ModConfigSpec.ConfigValue<String> SUCCESS_REG = BUILDER
            .comment("Message after successful registration.")
            .define("messages.success_reg", "§aYou have successfully registered!");

    public static final ModConfigSpec.ConfigValue<String> SUCCESS_LOGIN = BUILDER
            .comment("Message after successful login.")
            .define("messages.success_login", "§aYou have successfully logged in!");

    public static final ModConfigSpec.ConfigValue<String> WRONG_PASS = BUILDER
            .comment("Wrong password message.")
            .define("messages.wrong_password", "§cIncorrect password!");

    public static final ModConfigSpec.ConfigValue<String> ALREADY_REGISTERED = BUILDER
            .comment("Message if player is already registered.")
            .define("messages.already_registered", "§cYou are already registered!");

    public static final ModConfigSpec.BooleanValue USE_BLINDNESS = BUILDER
            .comment("Apply blindness to unauthenticated players?")
            .define("features.use_blindness", true);

    public static final ModConfigSpec SPEC = BUILDER.build();
}