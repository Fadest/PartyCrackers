package dev.fadest.partycrackers;

import dev.fadest.partycrackers.command.PartyCrackerCommand;
import dev.fadest.partycrackers.cracker.PartyCrackerManager;
import dev.fadest.partycrackers.listeners.PlayerListener;
import dev.fadest.partycrackers.utils.Messages;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

@Getter
public class PartyCrackersPlugin extends JavaPlugin {

    @Getter
    protected static PartyCrackersPlugin instance;
    private PartyCrackerManager manager;

    @Override
    public void onEnable() {
        instance = this;

        Messages.reload();

        this.manager = new PartyCrackerManager(this);

        final PartyCrackerCommand command = new PartyCrackerCommand(this, manager);

        getServer().getPluginManager().registerEvents(new PlayerListener(manager), this);
        Objects.requireNonNull(getCommand("partycracker")).setExecutor(command);
    }

    @Override
    public void onDisable() {
        manager.cancelExplosionsAndGiveAllRewards();
    }
}
