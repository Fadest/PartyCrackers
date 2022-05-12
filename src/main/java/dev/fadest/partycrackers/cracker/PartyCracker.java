package dev.fadest.partycrackers.cracker;

import dev.fadest.partycrackers.PartyCrackersPlugin;
import dev.fadest.partycrackers.cracker.reward.Reward;
import dev.fadest.partycrackers.cracker.reward.RewardType;
import dev.fadest.partycrackers.utils.Messages;
import dev.fadest.partycrackers.utils.RandomPick;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Builder
@Getter
public class PartyCracker {

    private final String id;
    @Getter(AccessLevel.NONE) // No need to include a Getter for this since we'll use #getItem()
    private final ItemStack itemStack;
    @Builder.Default
    private final int secondsToExplode = 5;
    @Builder.Default
    private final Sound tickingSound = Sound.UI_BUTTON_CLICK;
    @Singular
    private final List<Sound> explosionSounds;
    @Singular
    private final List<Particle> explosionParticles;
    @Singular
    private final Set<Reward> rewards;

    /**
     * Clones the PartyCracker item, add the id to the {@link PersistentDataContainer} and then return it
     *
     * @return The built PartyCracker item
     */
    public ItemStack getItem() {
        ItemStack item = itemStack.clone();
        ItemMeta itemMeta = item.getItemMeta();

        PersistentDataContainer container = itemMeta.getPersistentDataContainer();
        container.set(PartyCrackersPlugin.getInstance().getManager().getNamespacedKey(),
                PersistentDataType.STRING, this.id);

        item.setItemMeta(itemMeta);
        return item;
    }

    /**
     * Grab the name of the PartyCracker item and then return it as a Component
     *
     * @return The name of the Party Cracker
     */
    public Component getName() {
        return itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName() ?
                itemStack.getItemMeta().displayName() :
                Component.translatable(itemStack.translationKey());
    }

    /**
     * Starts a runnable that will explode the PartyCracker after the specified amount of time
     * This will also set a custom name to the {@param item} depending on the amount of time left
     *
     * @param player The player that started the Party Cracker
     * @param item   The item entity that was spawned
     */
    public void startExplosionTicking(@NotNull Player player, @NotNull Item item) {
        final long timeUntilExplosion = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(secondsToExplode);
        BukkitRunnable bukkitRunnable = new BukkitRunnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() >= timeUntilExplosion) {
                    item.setCustomNameVisible(false);
                    explode(player, item);

                    this.cancel();
                    return;
                }

                long currentTime = timeUntilExplosion - System.currentTimeMillis();

                long seconds = (currentTime / 1000) % 60;
                long millis = (currentTime % 1000) / 100;

                item.customName(Messages.toComponent("<red><bold>" + String.format("%02d.%s", seconds, millis) + "</bold> <gray>seconds remaining"));
                item.setCustomNameVisible(true);
            }
        };
        bukkitRunnable.runTaskTimerAsynchronously(PartyCrackersPlugin.getInstance(), 0, 2L);
    }

    /**
     * Explodes the PartyCracker, removing the {@param item} and allowing players to start another one.
     * <p>
     * This method also plays the explosion sound, spawns the explosion particles and then select a random reward
     *
     * @param player The player that started the Party Cracker
     * @param item   The item entity that will be removed
     */
    public void explode(@NotNull Player player, @NotNull Item item) {
        final Location location = item.getLocation();
        final PartyCrackersPlugin plugin = PartyCrackersPlugin.getInstance();
        final PartyCrackerManager manager = plugin.getManager();

        manager.getPartyCrackerItemEntities().remove(item);
        manager.getExplodingPartyCrackers().remove(player.getUniqueId());

        Bukkit.getScheduler().runTask(plugin, item::remove);

        final Random random = ThreadLocalRandom.current();

        Sound explosionSound = null;
        if (!this.explosionSounds.isEmpty()) {
            explosionSound = this.explosionSounds.get(random.nextInt(this.explosionSounds.size()));
        }

        Particle explosionParticle = null;
        if (!this.explosionParticles.isEmpty()) {
            explosionParticle = this.explosionParticles.get(random.nextInt(this.explosionParticles.size()));
        }

        if (explosionSound != null) player.playSound(location, explosionSound, 1f, 1f);
        if (explosionParticle != null) player.spawnParticle(explosionParticle, location.clone().add(0, 1, 0), 10);

        giveReward(player, location);
    }

    /**
     * Selects a random reward and then give it to the {@param player}
     * This method will loop through itself until a reward is found is none was present
     *
     * @param player   The player that started the Party Cracker
     * @param location The location of the Party Cracker to drop the rewards with type {@link RewardType#ITEM}
     */
    public void giveReward(@NotNull Player player, @NotNull Location location) {
        Optional<Reward> rewardOptional = getRandomReward();
        if (rewardOptional.isEmpty()) {
            //Loop until we find a reward
            giveReward(player, location);
            return;
        }

        Reward reward = rewardOptional.get();
        final Random random = ThreadLocalRandom.current();

        int amount = random.nextInt(reward.getMinAmount(), reward.getMaxAmount() + 1);

        Runnable runnable = () -> {
            if (reward.getRewardType() == RewardType.COMMAND) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), reward.getCommand()
                        .replace("{player}", player.getName())
                        .replace("{amount}", Integer.toString(amount)));
            } else {
                ItemStack itemStack = reward.getItemStack();
                if (itemStack.getMaxStackSize() > 1) {
                    itemStack.setAmount(amount);
                }

                for (int i = 0; i < (itemStack.getMaxStackSize() > 1 ? 1 : amount); i++) {
                    final Item item = location.getWorld().dropItem(location, itemStack); // Drops at the exact location
                    item.setVelocity(new Vector()); // No velocity so it doesn't fly away
                    item.setPickupDelay(0);
                }
            }

            final String rewardMessage = reward.getMessage();
            if (rewardMessage != null) {
                player.sendMessage(Messages.toComponent(rewardMessage,
                        Map.of(
                                "amount", Integer.toString(amount),
                                "player", player.getName()
                        )));
            }
        };

        // If it's running async, then we need to run it on the main thread
        // This also helps if the plugin is stopping so the runnable can be executed
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTask(PartyCrackersPlugin.getInstance(), runnable);
        }
    }

    /**
     * Gets a random reward from the list of rewards
     * <p>
     * This method creates a {@link RandomPick} instance, add all the rewards and then picks a random reward
     *
     * @return An {@link Optional} of {@link Reward}
     */
    private Optional<Reward> getRandomReward() {
        RandomPick<Reward> rewardRandomPick = new RandomPick<>();
        rewards.forEach(reward -> rewardRandomPick.add(reward.getChance(), reward));

        return Optional.ofNullable(rewardRandomPick.next());
    }
}
