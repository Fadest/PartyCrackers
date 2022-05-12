package dev.fadest.partycrackers.cracker.loader;

import dev.fadest.partycrackers.PartyCrackersPlugin;
import dev.fadest.partycrackers.cracker.PartyCracker;
import dev.fadest.partycrackers.utils.Pair;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

@UtilityClass
public class PartyCrackerFactory {

    /**
     * Loads all {@link PartyCracker}s from the config directory.
     * As well as the amount of failed party crackers.
     *
     * @return A {@link Pair} of the list of loaded party crackers and the amount of party crackers that failed to load.
     */
    @NotNull
    public Pair<List<PartyCracker>, Integer> loadPartyCrackersAndFailed() {
        File folderFile = new File(PartyCrackersPlugin.getInstance().getDataFolder() + File.separator + "party_crackers");
        if (!folderFile.exists()) {
            folderFile.mkdirs();
        }

        final var loader = new JSONPartyCrackerLoader();
        final Logger logger = PartyCrackersPlugin.getInstance().getLogger();

        final var pair = loader.loadFrom(folderFile);
        if (pair.first().isEmpty()) {
            pair.first().add(loadDefaultPartyCracker(loader, folderFile));
        }
        logger.info(pair.first().size() + " party crackers have been successfully loaded, and " + pair.second() + " have failed");

        return pair;
    }

    /**
     * Loads the default party cracker file and then saves it.
     *
     * @param loader     The loader to use.
     * @param folderFile The folder file.
     * @return The default PartyCracker instance
     */
    @NotNull
    private PartyCracker loadDefaultPartyCracker(@NotNull JSONPartyCrackerLoader loader, @NotNull File folderFile) {
        PartyCrackersPlugin.getInstance().getLogger().info("Loading the default configuration file");
        File file = new File(folderFile, "default.json");
        if (!file.exists()) {
            try (InputStream inputStream = PartyCrackersPlugin.getInstance().getResource("party_crackers/default.json");
                 OutputStream out = new FileOutputStream(file)) {
                IOUtils.copy(Objects.requireNonNull(inputStream), out);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // The default file only contains 1 party cracker so just get first
        return loader.loadFrom(file).first().get(0);
    }
}
