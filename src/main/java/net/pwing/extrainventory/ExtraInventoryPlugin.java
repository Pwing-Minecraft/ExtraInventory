package net.pwing.extrainventory;

import net.pwing.extrainventory.command.ExtraInventoryCommand;
import net.pwing.extrainventory.inventory.ExtraInventoryListener;
import net.pwing.extrainventory.inventory.ExtraInventoryManager;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ExtraInventoryPlugin extends JavaPlugin {

    private ExtraInventoryManager inventoryManager;

    @Override
    public void onEnable() {
        Path dir = Paths.get(this.getDataFolder().toString(), "data");
        if (Files.notExists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        this.inventoryManager = new ExtraInventoryManager(this);

        this.getCommand("inv").setExecutor(new ExtraInventoryCommand(this));

        this.getServer().getPluginManager().registerEvents(new ExtraInventoryListener(this), this);
        this.getServer().getScheduler().runTaskTimerAsynchronously(this, () -> Bukkit.getOnlinePlayers().forEach(this.inventoryManager::save), 6000, 6000);
    }

    public ExtraInventoryManager inventoryManager() {
        return this.inventoryManager;
    }
}
