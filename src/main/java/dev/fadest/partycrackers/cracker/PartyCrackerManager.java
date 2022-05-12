package dev.fadest.partycrackers.cracker;

import dev.fadest.partycrackers.PartyCrackersPlugin;
import dev.fadest.partycrackers.cracker.loader.PartyCrackerFactory;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Getter
public class PartyCrackerManager {

    private final PartyCrackersPlugin plugin;
    /**
     * A map containing the {@link Player}s and their {@link PartyCracker}s being used.
     */
    private final Map<UUID, String> explodingPartyCrackers;
    /**
     * The {@link Item}s entities that are alive on the world
     */
    private final Set<Item> partyCrackerItemEntities;
    private final List<PartyCracker> partyCrackers;

    /**
     * The {@link NamespacedKey} that will be used for the {@link PartyCracker} item {@link PersistentDataContainer}
     */
    private final NamespacedKey namespacedKey;

    public PartyCrackerManager(@NotNull PartyCrackersPlugin plugin) {
        this.plugin = plugin;
        this.explodingPartyCrackers = new HashMap<>();
        this.partyCrackerItemEntities = new HashSet<>();
        this.partyCrackers = new ArrayList<>();
        this.namespacedKey = new NamespacedKey(plugin, "party_cracker");

        this.partyCrackers.addAll(PartyCrackerFactory.loadPartyCrackersAndFailed().first());
        startTickingSoundRunnable();
    }

    /**
     * Starts a {@link Runnable} that will tick the {@link PartyCracker}s sound
     * <p>
     * Each sound will be played to the linked {@link Player}
     */
    public void startTickingSoundRunnable() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (var entry : explodingPartyCrackers.entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player == null || !player.isOnline()) continue;

                getPartyCrackedById(entry.getValue()).ifPresent(partyCracker -> player.playSound(player.getLocation(), partyCracker.getTickingSound(), 1f, 1f));
            }
        }, 0L, 5L);
    }

    @NotNull
    public Optional<PartyCracker> getPartyCrackedById(@NotNull String id) {
        return this.partyCrackers.stream().filter(partyCracker -> partyCracker.getId().equalsIgnoreCase(id)).findFirst();
    }

    /**
     * Checks if the {@param itemStack} is a {@link PartyCracker} item
     *
     * @param itemStack The {@link ItemStack} to check
     * @return Whether the {@param itemStack} is a {@link PartyCracker} item
     */

    public boolean isPartyCracker(@NotNull ItemStack itemStack) {
        if (!itemStack.hasItemMeta()) return false;
        final ItemMeta itemMeta = itemStack.getItemMeta();

        return itemMeta.getPersistentDataContainer().has(namespacedKey);
    }

    /**
     * Gets the {@link PartyCracker} id that is linked on the {@link ItemStack} {@link PersistentDataContainer}
     *
     * @param itemStack The {@link ItemStack} to check
     * @return An {@link Optional} containing the {@link PartyCracker} id
     */
    @NotNull
    public Optional<String> getPartyCrackerId(@NotNull ItemStack itemStack) {
        final ItemMeta itemMeta = itemStack.getItemMeta();

        return Optional.ofNullable(itemMeta.getPersistentDataContainer().get(namespacedKey, PersistentDataType.STRING));
    }

    /**
     * Removes all the @{@link PartyCracker} items on the world, as well as any task of the plugin
     * and finally give rewards for the pending {@link PartyCracker} explosions
     */
    public void cancelExplosionsAndGiveAllRewards() {
        partyCrackerItemEntities.forEach(Item::remove);
        Bukkit.getScheduler().cancelTasks(plugin); //All tasks of this plugin are the explosion tasks (except for the ticking sound task)

        var iterator = this.explodingPartyCrackers.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            iterator.remove(); // Remove the player from the map

            Optional<PartyCracker> partyCrackerOptional = getPartyCrackedById(entry.getValue());
            if (partyCrackerOptional.isEmpty()) continue;

            PartyCracker partyCracker = partyCrackerOptional.get();

            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) continue;

            // Forcefully give rewards to the player
            partyCracker.giveReward(player, player.getLocation());
        }
    }


}
