package osteam.lunauth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class AuthStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    // Измененный путь: теперь в папке config/LunAuth/
    private static final Path STORAGE_PATH = Path.of("config", "LunAuth", "users.json");
    private static Map<String, String> userDatabase = new HashMap<>();

    public static void load() {
        if (!Files.exists(STORAGE_PATH)) return;
        try (Reader reader = Files.newBufferedReader(STORAGE_PATH)) {
            userDatabase = GSON.fromJson(reader, new TypeToken<Map<String, String>>(){}.getType());
        } catch (IOException e) {
            LunAuth.LOGGER.error("LunAuth: Ошибка при загрузке базы паролей!", e);
        }
    }

    public static void save() {
        try {
            // Создает всю иерархию папок (config/LunAuth), если её еще нет
            Files.createDirectories(STORAGE_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(STORAGE_PATH)) {
                GSON.toJson(userDatabase, writer);
            }
        } catch (IOException e) {
            LunAuth.LOGGER.error("LunAuth: Ошибка при сохранении базы паролей!", e);
        }
    }

    public static void register(String username, String password) {
        userDatabase.put(username.toLowerCase(), password);
        save();
    }

    public static boolean isRegistered(String username) {
        return userDatabase.containsKey(username.toLowerCase());
    }

    public static boolean checkPassword(String username, String password) {
        String stored = userDatabase.get(username.toLowerCase());
        return stored != null && stored.equals(password);
    }
}