package dev.fadest.partycrackers.listeners;

import dev.fadest.partycrackers.cracker.PartyCracker;
import dev.fadest.partycrackers.cracker.PartyCrackerManager;
import dev.fadest.partycrackers.utils.Messages;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public record PlayerListener(PartyCrackerManager manager) implements Listener {

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        final Player player = event.getPlayer();
        final Item item = event.getItemDrop();
        final ItemStack itemStack = item.getItemStack();

        if (!manager.isPartyCracker(itemStack)) return;
        final Optional<String> partyCrackerIdOptional = manager.getPartyCrackerId(itemStack);

        // This should never happen as manager.isPartyCracker() should have already checked this
        if (partyCrackerIdOptional.isEmpty()) return;

        final String partyCrackerId = partyCrackerIdOptional.get();
        final Optional<PartyCracker> partyCrackerOptional = manager.getPartyCrackedById(partyCrackerId);
        if (partyCrackerOptional.isEmpty()) {
            player.sendMessage(Messages.ERROR_DROP_PARTY_CRACKER_NOT_FOUND.toComponent());
            event.setCancelled(true);
            return;
        }

        if (manager.getExplodingPartyCrackers().containsKey(player.getUniqueId())) {
            player.sendMessage(Messages.ERROR_DROP_PARTY_CRACKER_COOL_DOWN.toComponent());
            event.setCancelled(true);
            return;
        }

        // Add properties to the item, so it doesn't get destroyed
        item.setUnlimitedLifetime(true);
        item.setCanMobPickup(false);
        item.setPickupDelay(Integer.MAX_VALUE);
        item.setCanPlayerPickup(false);
        item.setInvulnerable(true);
        item.setWillAge(false);

        final PartyCracker partyCracker = partyCrackerOptional.get();
        manager.getPartyCrackerItemEntities().add(item); // We add the item to the list of items to be removed later
        manager.getExplodingPartyCrackers().put(player.getUniqueId(), partyCrackerId); // Make sure we add players, so they can't drop multiple party crackers

        partyCracker.startExplosionTicking(player, item);
    }


}
