package example;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;

@net.neoforged.fml.common.EventBusSubscriber(modid = ExampleAddon.MOD_ID)
public final class AssetCatalogChecks {
    @net.neoforged.bus.api.SubscribeEvent public static void started(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        if (!Boolean.getBoolean("ic2.assets.verify")) return;
        try { run(); } finally { event.getServer().halt(false); }
    }
    static void run() {
        var errors = new TreeSet<String>();
        var models = new HashSet<String>();
        var en = json("assets/ic2/lang/en_us.json", errors);
        var ru = json("assets/ic2/lang/ru_ru.json", errors);
        int count = 0;
        for (var item : BuiltInRegistries.ITEM) {
            var id = BuiltInRegistries.ITEM.getKey(item);
            if (!id.getNamespace().equals("ic2")) continue;
            count++;
            var key = item.getDescriptionId();
            if (!en.has(key)) errors.add("en_us: " + key);
            if (!ru.has(key)) errors.add("ru_ru: " + key);
            model("ic2:item/" + id.getPath(), models, errors);
        }
        for (var block : BuiltInRegistries.BLOCK) {
            var id = BuiltInRegistries.BLOCK.getKey(block);
            if (!id.getNamespace().equals("ic2") || block.asItem() == Items.AIR) continue;
            blockModels(json("assets/ic2/blockstates/" + id.getPath() + ".json", errors), models, errors);
        }
        if (!errors.isEmpty()) throw new AssertionError("IC2 asset catalog failures (" + errors.size() + "):\n" + String.join("\n", errors));
        com.mojang.logging.LogUtils.getLogger().info("IC2 asset catalog passed: {} items, EN/RU names, blockstates and {} model dependencies", count, models.size());
    }
    private static JsonObject json(String path, Set<String> errors) {
        try (var stream = AssetCatalogChecks.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) { errors.add("Missing " + path); return new JsonObject(); }
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException | RuntimeException failure) { errors.add(path + ": " + failure.getMessage()); return new JsonObject(); }
    }
    private static void model(String id, Set<String> seen, Set<String> errors) {
        if (!id.startsWith("ic2:") || !seen.add(id)) return;
        var data = json("assets/ic2/models/" + id.substring(4) + ".json", errors);
        if (data.has("parent")) model(data.get("parent").getAsString(), seen, errors);
        if (data.has("textures")) for (var entry : data.getAsJsonObject("textures").entrySet()) {
            var texture = entry.getValue().getAsString();
            if (!texture.startsWith("ic2:")) continue;
            var path = "assets/ic2/textures/" + texture.substring(4) + ".png";
            if (AssetCatalogChecks.class.getClassLoader().getResource(path) == null) errors.add("Missing " + path);
        }
    }
    private static void blockModels(JsonElement element, Set<String> seen, Set<String> errors) {
        if (element.isJsonArray()) for (var child : element.getAsJsonArray()) blockModels(child, seen, errors);
        else if (element.isJsonObject()) for (var entry : element.getAsJsonObject().entrySet()) {
            if (entry.getKey().equals("model") && entry.getValue().isJsonPrimitive()) model(entry.getValue().getAsString(), seen, errors);
            else blockModels(entry.getValue(), seen, errors);
        }
    }
}
