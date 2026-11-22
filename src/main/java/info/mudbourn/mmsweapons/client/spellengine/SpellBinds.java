package info.mudbourn.mmsweapons.client.spellengine;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

// The player's chosen spell hotbar slot per spell id, saved in config/mms_weapons/spell_binds.json.
public final class SpellBinds {

    private static final Logger LOG = LoggerFactory.getLogger("mms_weapons");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, Integer> CHOSEN = new HashMap<>();

    private SpellBinds() {
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("mms_weapons").resolve("spell_binds.json");
    }

    public static void load() {
        CHOSEN.clear();
        try {
            if (Files.exists(file())) {
                Map<String, Integer> read = GSON.fromJson(Files.readString(file()), new TypeToken<Map<String, Integer>>() { }.getType());
                if (read != null) {
                    CHOSEN.putAll(read);
                }
            }
        } catch (IOException | RuntimeException e) {
            LOG.warn("Could not read the spell binds.", e);
        }
    }

    public static OptionalInt slotOf(String spellId) {
        Integer slot = CHOSEN.get(spellId);
        return slot == null ? OptionalInt.empty() : OptionalInt.of(slot);
    }

    public static void choose(String spellId, int slot) {
        CHOSEN.put(spellId, slot);
        save();
    }

    private static void save() {
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), GSON.toJson(CHOSEN));
        } catch (IOException e) {
            LOG.warn("Could not save the spell binds.", e);
        }
    }
}
