package dev.fadest.partycrackers.cracker.reward;

import lombok.Builder;
import lombok.Getter;
import org.bukkit.inventory.ItemStack;

@Builder
@Getter
public class Reward {
    @Builder.Default
    private final RewardType rewardType = RewardType.COMMAND;
    private final ItemStack itemStack;
    private final String command, message;
    @Builder.Default
    private final int minAmount = 1;
    @Builder.Default
    private final int maxAmount = 1;
    private final double chance;

}
