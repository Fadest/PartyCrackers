package dev.fadest.partycrackers.utils.item;

import dev.fadest.partycrackers.utils.Messages;
import lombok.Builder;
import lombok.Singular;
import org.apache.commons.lang.Validate;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

@Builder
public class ItemBuilder {

    @Builder.Default
    protected final int amount = 1;
    protected final String name;
    @Builder.Default
    protected final short durability = -1;
    @Builder.Default
    protected final int modelData = -1;
    @Singular("lore")
    protected final List<String> description;
    @Singular
    protected final Map<Enchantment, Integer> enchants;
    @Singular
    protected final Set<ItemFlag> flags;
    protected final boolean glow;
    protected final Material material;
    protected final boolean unbreakable;
    @Builder.Default
    protected final boolean allFlags = false;

    /**
     * Build the {@link ItemMeta} for the {@link ItemStack} from all parameters provided
     *
     * @param stack The {@link ItemStack} to build the {@link ItemMeta} for
     * @return The constructed {@link ItemMeta}
     */
    protected ItemMeta getItemMeta(@NotNull ItemStack stack) {
        final ItemMeta stackMeta = stack.getItemMeta();
        Set<ItemFlag> finalFlags = new HashSet<>();

        if (glow) {
            stackMeta.addEnchant(Enchantment.DURABILITY, 1, true);

            finalFlags.add(ItemFlag.HIDE_ENCHANTS);
        }

        if (enchants != null) {
            if (material == Material.ENCHANTED_BOOK) {
                this.enchants.forEach((enchantment, level) -> ((EnchantmentStorageMeta) stackMeta).addStoredEnchant(enchantment, level, true));
            } else {
                for (final Enchantment enchantment : enchants.keySet()) {
                    stackMeta.addEnchant(enchantment, enchants.get(enchantment), true);
                }
            }
        }

        // We'll add a '&r' in front so Minecraft doesn't add a purple color to the name/lore
        if (name != null) {
            stackMeta.displayName(Messages.toComponent("&r" + name));
        }

        if (description != null && !description.isEmpty()) {
            if (description.size() == 1) {
                String[] split = description.get(0).split("\n");

                stackMeta.lore(Arrays.stream(split).map(s -> Messages.toComponent("&r" + s)).collect(Collectors.toList()));
            } else {
                stackMeta.lore(description.stream().map(s -> Messages.toComponent("&r" + s)).collect(Collectors.toList()));
            }
        }

        if (unbreakable) {
            stackMeta.setUnbreakable(true);
        }

        if (flags != null) {
            finalFlags.addAll(flags);
        }

        // Add all the {@link ItemFlag}s values to hide everything
        if (allFlags) {
            finalFlags.addAll(Arrays.asList(ItemFlag.values()));
        }

        finalFlags.forEach(stackMeta::addItemFlags);

        return stackMeta;
    }

    /**
     * Build the {@link ItemStack} from all parameters provided
     *
     * @return The constructed {@link ItemStack} or null if something went wrong
     */
    public ItemStack make() {
        final ItemStack stack = material != null ? new ItemStack(material, amount) : null;

        Validate.notNull(stack, "Material must be set!");

        final ItemMeta stackMeta = getItemMeta(stack);

        if (material == Material.AIR) return stack;

        if (modelData != -1) {
            stackMeta.setCustomModelData(modelData);
        }

        stack.setItemMeta(stackMeta);

        if (durability != -1) {
            try {
                // If the item is still using durability then set it
                stack.setDurability(durability);
            } catch (final Throwable ignored) {
            }
        }

        return stack;
    }
}