package dev.fadest.partycrackers.utils.item;

import com.google.gson.stream.JsonReader;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

@UtilityClass
public class ItemLoader {

    /**
     * Loads an {@link ItemStack} from a JSON file.
     * <p>
     * The item must be in the following format:
     * <pre>
     *     {
     *         "material": "STONE",
     *         "name": "Stone",
     *         "lore": [
     *             "This is a stone"
     *          ],
     *          "enchants": [
     *              {
     *                  "name": "unbreaking",
     *                  "level": 1
     *              }
     *          ],
     *          "flags": [
     *              "HIDE_ENCHANTS",
     *              "HIDE_ATTRIBUTES"
     *          ]
     *    }
     * </pre>
     *
     * @param reader The reader to read from
     * @return The loaded item.
     */
    @SneakyThrows
    @NotNull
    public ItemStack readItem(@NotNull JsonReader reader) {
        final ItemBuilder.ItemBuilderBuilder builder = ItemBuilder.builder().material(Material.AIR);

        reader.beginObject();

        while (reader.hasNext()) {
            switch (reader.nextName().toLowerCase()) {
                case "type", "material" -> builder.material(Material.valueOf(reader.nextString().toUpperCase()));
                case "name" -> builder.name(reader.nextString());
                case "lore", "description" -> {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        builder.lore(reader.nextString());
                    }
                    reader.endArray();
                }
                case "enchantments", "enchants" -> {
                    reader.beginArray();

                    while (reader.hasNext()) {
                        reader.beginObject();

                        while (reader.hasNext()) {
                            Enchantment enchantment = Enchantment.DURABILITY;
                            int level = 1;
                            switch (reader.nextName().toLowerCase()) {
                                case "name" ->
                                        enchantment = Enchantment.getByKey(NamespacedKey.minecraft(reader.nextString().toLowerCase(Locale.ROOT)));
                                case "level" -> level = reader.nextInt();
                                default -> reader.skipValue();
                            }
                            builder.enchant(enchantment, level);
                        }
                        reader.endObject();
                    }
                    reader.endArray();
                }
                case "unbreakable" -> {
                    reader.skipValue();
                    builder.unbreakable(true);
                }
                case "allflags", "hideflags" -> {
                    reader.skipValue();
                    builder.allFlags(true);
                }
                case "flag", "item_flag" ->
                        builder.flag(ItemFlag.valueOf(reader.nextString().toUpperCase(Locale.ROOT)));
                case "flags" -> {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        builder.flag(ItemFlag.valueOf(reader.nextString().toUpperCase(Locale.ROOT)));
                    }
                    reader.endArray();
                }
                case "durability", "damage" -> builder.durability(Integer.valueOf(reader.nextInt()).byteValue());
                case "model", "modeldata", "model_data", "custom_model" -> builder.modelData(reader.nextInt());
                case "glowing", "addglow", "glow" -> builder.glow(reader.nextBoolean());
                default -> reader.skipValue();
            }
        }

        reader.endObject();
        return builder.build().make();
    }


}
