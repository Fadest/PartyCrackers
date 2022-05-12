package dev.fadest.partycrackers.cracker.loader;

import com.google.gson.stream.JsonReader;
import dev.fadest.partycrackers.PartyCrackersPlugin;
import dev.fadest.partycrackers.cracker.PartyCracker;
import dev.fadest.partycrackers.cracker.reward.Reward;
import dev.fadest.partycrackers.cracker.reward.RewardType;
import dev.fadest.partycrackers.utils.Pair;
import dev.fadest.partycrackers.utils.item.ItemLoader;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class JSONPartyCrackerLoader {

    /**
     * Loads a party cracker from a file.
     * <p>
     * If the file is a directory, all files in the directory will be loaded.
     *
     * @param toLoad The file to load from.
     * @return A pair of the loaded party cracker and the number of files loaded.
     */
    @NotNull
    public Pair<List<PartyCracker>, Integer> loadFrom(File toLoad) {
        final Logger logger = PartyCrackersPlugin.getInstance().getLogger();
        final List<PartyCracker> partyCrackers = new ArrayList<>();
        final AtomicInteger failedPartyCrackers = new AtomicInteger(0);

        if (toLoad.isDirectory()) {
            File[] files = toLoad.listFiles((file, s) -> s.endsWith(".json"));

            if (files != null) {
                for (File file : files) {
                    var pair = loadFrom(file);

                    partyCrackers.addAll(pair.first());
                    failedPartyCrackers.addAndGet(pair.second());
                }
            }

            return Pair.of(partyCrackers, failedPartyCrackers.get());
        }

        logger.warning("Loading file: " + toLoad.getName());
        try (FileReader fileReader = new FileReader(toLoad); JsonReader reader = new JsonReader(fileReader)) {
            reader.beginArray();

            while (reader.hasNext()) {
                final PartyCracker partyCracker = readPartyCracked(reader);
                if (partyCracker == null) {
                    failedPartyCrackers.incrementAndGet();
                    continue;
                }

                partyCrackers.add(partyCracker);
            }

            reader.endArray();
        } catch (IOException e) {
            logger.warning("An error has occurred while loading the party crackers, stack trace: "
                    + e.getMessage());
        }

        return Pair.of(partyCrackers, failedPartyCrackers.get());
    }


    /**
     * Reads a PartyCracker from the reader.
     * <p>
     * The PartyCracker must be in the following format:
     * <pre>
     *     {
     *         "id": "default",
     *         "seconds": 5,
     *         "ticking_sound": "UI_BUTTON_CLICK",
     *         "explosion_sound": "UI_TOAST_CHALLENGE_COMPLETE",
     *         "explosion_particle": "VILLAGER_HAPPY",
     *         "item": {
     *            "type": "TRIPWIRE_HOOK",
     *            "name": "<#fcdf03>Default Party Cracker",
     *            "lore": [
     *               "<gray>This is the default party cracker",
     *               "<gray>Drop it to get rewards!"
     *            ],
     *            "glowing": true
     *         },
     *         "rewards": [
     *            {
     *             "type": "COMMAND",
     *             "command": "give {player} minecraft:diamond 64",
     *             "message": "<yellow>You have received 64 diamonds!",
     *             "chance": 50
     *            },
     *            {
     *             "type": "ITEM",
     *             "item": {
     *             "type": "STONE",
     *             "name": "<gradient:#03dbfc:#035afc>Enchanted Stone</gradient>",
     *             "lore": [
     *                "<gray>This is an enchanted stone!"
     *             ],
     *             "glowing": true
     *            },
     *            "message": "<yellow>You have received x{amount} enchanted stone blocks!",
     *            "chance": 10,
     *            "min": 1,
     *            "max": 10
     *            }
     *         ]
     *     }
     * </pre>
     *
     * @param reader The reader to read from.
     * @return The built PartyCracker or null if an error occurred.
     */
    @Nullable
    private PartyCracker readPartyCracked(@NotNull JsonReader reader) {
        final Logger logger = PartyCrackersPlugin.getInstance().getLogger();

        try {
            final PartyCracker.PartyCrackerBuilder builder = PartyCracker.builder();

            reader.beginObject();

            while (reader.hasNext()) {
                String readerName = reader.nextName();
                switch (readerName.toLowerCase()) {
                    case "id" -> builder.id(reader.nextString());
                    case "seconds", "explode_time", "seconds_to_explode" -> builder.secondsToExplode(reader.nextInt());
                    case "sound", "ticking", "ticking_sound" ->
                            builder.tickingSound(Sound.valueOf(reader.nextString().toUpperCase(Locale.ROOT)));
                    case "explosion_sounds", "explode_sounds" -> {
                        reader.beginArray();
                        while (reader.hasNext()) {
                            builder.explosionSound(Sound.valueOf(reader.nextString().toUpperCase(Locale.ROOT)));
                        }
                        reader.endArray();
                    }
                    case "explosion_sound", "explode_sound" ->
                            builder.explosionSound(Sound.valueOf(reader.nextString().toUpperCase(Locale.ROOT)));
                    case "explosion_particles", "explode_particles" -> {
                        reader.beginArray();
                        while (reader.hasNext()) {
                            builder.explosionParticle(Particle.valueOf(reader.nextString().toUpperCase(Locale.ROOT)));
                        }
                        reader.endArray();
                    }
                    case "explosion_particle", "explode_particle" ->
                            builder.explosionParticle(Particle.valueOf(reader.nextString().toUpperCase(Locale.ROOT)));
                    case "item", "item_stack" -> builder.itemStack(ItemLoader.readItem(reader));
                    case "rewards" -> {
                        reader.beginArray();
                        while (reader.hasNext()) {
                            final Reward reward = readReward(reader);
                            if (reward != null) builder.reward(reward);
                        }
                        reader.endArray();
                    }
                    case "reward" -> {
                        final Reward reward = readReward(reader);
                        if (reward != null) builder.reward(reward);
                    }
                    default -> reader.skipValue();
                }
            }

            reader.endObject();

            PartyCracker partyCracker = builder.build();
            if (partyCracker.getId() == null) {
                logger.warning("[LOADING] A party cracker is missing the id");
                partyCracker = null;
            }

            return partyCracker;
        } catch (IOException | IllegalArgumentException e) {
            logger.warning("[LOADING] An error has occurred while loading the party tracker, stack trace: " + e.getMessage());
        }

        return null;
    }

    /**
     * Reads a reward from the reader.
     * <p>
     * The reward must be in the following format:
     * <pre>
     *     {
     *         "type": "COMMAND",
     *         "chance": "50",
     *         "command": "give {player} minecraft:diamond 64!",
     *         "message:" "Hello {player} you got 64 diamonds!"
     *     }
     * </pre>
     *
     * @param reader The reader to read from.
     * @return The built Reward or null if an error occurred.
     */
    @Nullable
    private Reward readReward(@NotNull JsonReader reader) {
        final Logger logger = PartyCrackersPlugin.getInstance().getLogger();

        try {
            final Reward.RewardBuilder builder = Reward.builder();

            reader.beginObject();

            while (reader.hasNext()) {
                String readerName = reader.nextName();
                switch (readerName.toLowerCase()) {
                    case "type", "reward_type" ->
                            builder.rewardType(RewardType.valueOf(reader.nextString().toUpperCase(Locale.ROOT)));
                    case "command" -> builder.command(reader.nextString());
                    case "message" -> builder.message(reader.nextString());
                    case "item", "item_stack" -> builder.itemStack(ItemLoader.readItem(reader));
                    case "chance", "probabilities", "probability" -> builder.chance(reader.nextDouble());
                    case "min", "minimum" -> builder.minAmount(reader.nextInt());
                    case "max", "maximum" -> builder.maxAmount(reader.nextInt());
                    default -> reader.skipValue();
                }
            }

            reader.endObject();

            Reward reward = builder.build();

            if (reward.getRewardType() == RewardType.COMMAND && reward.getCommand() == null) {
                logger.warning("A Reward with type COMMAND doesn't contains the command");
                reward = null;
            } else if (reward.getRewardType() == RewardType.ITEM && reward.getItemStack() == null) {
                logger.warning("A Reward with type ITEM doesn't contains the item");
                reward = null;
            }

            return reward;
        } catch (IOException e) {
            logger.warning("An error has occurred while loading a reward, stack trace: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.warning("An error has occurred while loading a reward type, valid values are COMMAND and ITEM, stack trace: "
                    + e.getMessage());
        }
        return null;
    }

}
