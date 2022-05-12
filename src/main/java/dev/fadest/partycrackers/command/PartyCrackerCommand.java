package dev.fadest.partycrackers.command;

import dev.fadest.partycrackers.PartyCrackersPlugin;
import dev.fadest.partycrackers.cracker.PartyCracker;
import dev.fadest.partycrackers.cracker.PartyCrackerManager;
import dev.fadest.partycrackers.cracker.loader.PartyCrackerFactory;
import dev.fadest.partycrackers.cracker.reward.Reward;
import dev.fadest.partycrackers.cracker.reward.RewardType;
import dev.fadest.partycrackers.utils.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public record PartyCrackerCommand(PartyCrackersPlugin plugin,
                                  PartyCrackerManager manager) implements CommandExecutor, TabCompleter {

    private static final DecimalFormat FORMAT = new DecimalFormat("0.00");

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("partycracker.admin")) {
            sender.sendMessage(Messages.COMMAND_ERROR_NOT_PERMISSIONS.toComponent());
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Messages.COMMAND_HELP.toComponent());
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give", "add", "g" -> {
                if (args.length < 3) {
                    sender.sendMessage(Messages.COMMAND_GIVE_USAGE.toComponent());
                    break;
                }

                final Player targetPlayer = Bukkit.getPlayer(args[1]);
                if (targetPlayer == null || !targetPlayer.isOnline()) {
                    sender.sendMessage(Messages.COMMAND_ERROR_PLAYER_NOT_FOUND.toComponent(Map.of("player", args[1])));
                    break;
                }

                Optional<PartyCracker> partyCrackerOptional = manager.getPartyCrackedById(args[2]);
                if (partyCrackerOptional.isEmpty()) {
                    sender.sendMessage(Messages.COMMAND_ERROR_PARTY_CRACKER_NOT_FOUND.toComponent(Map.of("id", args[2])));
                    break;
                }
                final PartyCracker partyCracker = partyCrackerOptional.get();
                final ItemStack itemStack = partyCracker.getItem();

                int amount = 1;
                if (args.length >= 4) {
                    try {
                        amount = Math.max(1, Integer.parseInt(args[3]));
                    } catch (NumberFormatException ignored) {
                    }
                }
                itemStack.setAmount(amount);

                final Map<String, Object> placeholders = Map.of(
                        "party_cracker", partyCracker.getName(),
                        "amount", amount,
                        "player", targetPlayer.getName()
                );

                sender.sendMessage(Messages.COMMAND_GIVE_GIVEN.toComponent(placeholders));
                targetPlayer.sendMessage(Messages.COMMAND_GIVE_RECEIVED.toComponent(placeholders));

                // Drop any residual item
                targetPlayer.getInventory().addItem(itemStack).values()
                        .forEach(i -> targetPlayer.getWorld().dropItemNaturally(targetPlayer.getLocation(), i));
            }
            case "list", "l" -> {
                for (String s : Messages.COMMAND_PARTY_CRACKER_LIST.toMessage().split("\\n")) {
                    if (s.contains("party_crackers")) {
                        sender.sendMessage(Messages.toComponent(s, Map.of("party_crackers", manager.getPartyCrackers().size())));
                        continue;
                    }

                    for (PartyCracker partyCracker : manager().getPartyCrackers()) {
                        String tickingSound = partyCracker.getTickingSound() != null ? partyCracker.getTickingSound().name() : "None";

                        sender.sendMessage(Messages.toComponent(s,
                                Map.of(
                                        "id", partyCracker.getId(),
                                        "name", partyCracker.getName(),
                                        "seconds_to_explode", partyCracker.getSecondsToExplode(),
                                        "ticking_sound", tickingSound,
                                        "explosion_sounds", partyCracker.getExplosionSounds().size(),
                                        "explosion_particles", partyCracker.getExplosionParticles().size(),
                                        "rewards", partyCracker.getRewards().size()
                                )
                        ));
                    }
                }
            }
            case "reload", "r" -> {
                Messages.reload();
                manager.cancelExplosionsAndGiveAllRewards();

                List<PartyCracker> partyCrackers = manager.getPartyCrackers();
                partyCrackers.clear();

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    final var pair = PartyCrackerFactory.loadPartyCrackersAndFailed();
                    partyCrackers.addAll(pair.first());

                    sender.sendMessage(Messages.COMMAND_RELOAD_RELOADED.toComponent(
                            Map.of(
                                    "loaded", partyCrackers.size(),
                                    "failed", pair.second()
                            )
                    ));
                    manager.startTickingSoundRunnable();
                });
            }
            case "information", "info", "i" -> {
                if (args.length < 2) {
                    sender.sendMessage(Messages.COMMAND_INFORMATION_USAGE.toComponent());
                    break;
                }

                String partyCrackerId = args[1];
                Optional<PartyCracker> partyCrackerOptional = manager.getPartyCrackedById(partyCrackerId);
                if (partyCrackerOptional.isEmpty()) {
                    sender.sendMessage(Messages.COMMAND_ERROR_PARTY_CRACKER_NOT_FOUND.toComponent(Map.of(
                            "id", partyCrackerId
                    )));
                    break;
                }

                PartyCracker partyCracker = partyCrackerOptional.get();
                for (String message : Messages.COMMAND_INFORMATION_INFORMATION.toMessage().split("\\n")) {
                    if (message.contains("explosion_sounds")) {
                        sender.sendMessage(getFinalComponent(partyCracker.getExplosionSounds().stream().map(Sound::name).iterator(), message, "explosion_sounds"));
                        continue;
                    } else if (message.contains("explosion_particles")) {
                        sender.sendMessage(getFinalComponent(partyCracker.getExplosionParticles().stream().map(Particle::name).iterator(), message, "explosion_particles"));
                        continue;
                    } else if (message.contains("reward_list")) {
                        String[] split = message.split(" ");

                        Component component = Messages.toComponent(split[0]);

                        var iterator = partyCracker.getRewards().iterator();

                        if (!iterator.hasNext()) {
                            component = component.append(Messages.toComponent(split[1],
                                    Map.of("reward_list", "None")));
                        } else {
                            String finalMessage = " " + split[1].replace("{reward_list}", "");
                            AtomicInteger counter = new AtomicInteger(0);
                            while (iterator.hasNext()) {
                                Reward reward = iterator.next();

                                Component hoverComponent;
                                if (reward.getRewardType() == RewardType.COMMAND) {
                                    hoverComponent = Messages.toComponent("<gray>Command:" + finalMessage + reward.getCommand() + "<newline>");
                                } else {
                                    final ItemStack itemStack = reward.getItemStack();
                                    hoverComponent = Messages.toComponent("<gray>Item: <white>{item}<newline>",
                                            Map.of(
                                                    "item", (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()
                                                            ? Objects.requireNonNull(itemStack.getItemMeta().displayName()) : Component.translatable(itemStack.translationKey()))
                                            )
                                    );
                                }

                                hoverComponent = hoverComponent.append(Messages.toComponent(
                                        """
                                                <gray>Chance:{chance}
                                                <gray>Amount:{amount}
                                                <gray>Message:{message}""",
                                        Map.of(
                                                "chance", finalMessage + FORMAT.format(reward.getChance()),
                                                "amount", finalMessage + reward.getMinAmount() + " - " + reward.getMaxAmount(),
                                                "message", finalMessage + (reward.getMessage() != null ? reward.getMessage() : "None")
                                        )
                                ));

                                component = component.append(Messages.toComponent(finalMessage + counter.incrementAndGet()).hoverEvent(HoverEvent.showText(hoverComponent)));
                                if (iterator.hasNext()) {
                                    component = component.append(Component.text(",").color(NamedTextColor.GRAY));
                                }
                            }
                        }

                        sender.sendMessage(component);
                        continue;
                    }

                    final String tickingSound = partyCracker.getTickingSound() != null ? partyCracker.getTickingSound().name() : "None";

                    sender.sendMessage(Messages.toComponent(message,
                            Map.of(
                                    "name", partyCracker.getName(),
                                    "id", partyCracker.getId(),
                                    "seconds_to_explode", partyCracker.getSecondsToExplode(),
                                    "ticking_sound", tickingSound
                            )
                    ));
                }

            }
            default -> sender.sendMessage(Messages.COMMAND_HELP.toComponent());
        }
        return true;
    }

    private Component getFinalComponent(Iterator<String> iterator, String message, String placeholder) {
        String[] split = message.split(":");

        Component component = Messages.toComponent(split[0]);

        if (!iterator.hasNext()) {
            component = component.append(Messages.toComponent(split[1],
                    Map.of(placeholder, "None")));
        } else {
            String finalMessage = split[1];
            while (iterator.hasNext()) {
                //We add a space character as we split by a space above
                component = component.append(Messages.toComponent(finalMessage,
                        Map.of(placeholder, iterator.next())
                ));

                if (iterator.hasNext()) {
                    component = component.append(Component.text(",").color(NamedTextColor.GRAY));
                }
            }
        }

        return component;
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!command.getName().equalsIgnoreCase("partycracker") || !sender.hasPermission("partycracker.admin"))
            return null;

        final List<String> completions = List.of("give", "list", "reload", "info");

        return switch (args.length) {
            case 0 -> completions;
            // Autocomplete using subcommands that start with the given string
            case 1 -> completions.stream().filter(c -> c.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
            default -> switch (args[0].toLowerCase()) {
                case "give" -> switch (args.length) {
                    // Name of players that can be targeted
                    case 2 -> Bukkit.getOnlinePlayers().stream().map(Player::getName)
                            .filter(name -> name.startsWith(args[1].toLowerCase())).toList();
                    // List all the party crackers
                    case 3 -> manager.getPartyCrackers().stream().map(PartyCracker::getId)
                            .filter(id -> id.startsWith(args[2].toLowerCase())).toList();
                    // Default amount of party crackers items to give
                    case 4 -> List.of("1");
                    default -> null;
                };
                // List all the party crackers
                case "info" -> manager.getPartyCrackers().stream().map(PartyCracker::getId)
                        .filter(id -> id.startsWith(args[1].toLowerCase())).toList();
                default -> null;
            };
        };
    }
}
