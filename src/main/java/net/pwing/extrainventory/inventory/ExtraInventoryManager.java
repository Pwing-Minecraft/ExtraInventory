package net.pwing.extrainventory.inventory;

import net.pwing.extrainventory.ExtraInventoryPlugin;
import net.pwing.extrainventory.util.ExtraInventoryUtil;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ExtraInventoryManager {

    private ExtraInventoryPlugin plugin;

    private Map<UUID, ExtraInventory> loadedInventories = new HashMap<>();
    private Map<UUID, Inventory> cachedInventories = new HashMap<>();
    private List<UUID> spying = new ArrayList<>();

    public ExtraInventoryManager(ExtraInventoryPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean has(OfflinePlayer player) {
        Path path = Paths.get(this.plugin.getDataFolder().toString(), "data", player.getUniqueId().toString() + ".yml");
        return Files.exists(path);
    }

    public ExtraInventory load(OfflinePlayer player) throws ClassNotFoundException, IOException {
        Path path = Paths.get(this.plugin.getDataFolder().toString(), "data", player.getUniqueId().toString() + ".yml");
        if (Files.notExists(path)) {
            Files.createFile(path);
        }

        FileConfiguration invConfig = YamlConfiguration.loadConfiguration(Files.newBufferedReader(path));
        String serializedData = invConfig.getString("inv");
        if (serializedData != null && !serializedData.isEmpty()) {
            Optional<Inventory> inventory = ExtraInventoryUtil.fromBase64(invConfig.getString("inv"));
            if (inventory.isPresent()) {
                return new ExtraInventory(inventory.get().getSize(), inventory.get().getContents());
            }
        }

        return new ExtraInventory(0, new ItemStack[0]);
    }

    public void save(OfflinePlayer player) {
        if (!this.loadedInventories.containsKey(player.getUniqueId())) {
            return;
        }

        ExtraInventory inventory = this.loadedInventories.get(player.getUniqueId());
        Path path = Paths.get(this.plugin.getDataFolder().toString(), "data", player.getUniqueId().toString() + ".yml");
        try {
            FileConfiguration invConfig = YamlConfiguration.loadConfiguration(Files.newBufferedReader(path));
            invConfig.set("inv", ExtraInventoryUtil.toBase64(inventory.slots(), inventory.contents()));
            invConfig.save(path.toFile());
        } catch (IOException ex) {
            this.plugin.getLogger().warning("Failed to save inventory for " + player.getName() + "!");
            ex.printStackTrace();
        }
    }

    public void refresh(Player player) {
        int slots = 0;
        for (int i = 0; i < 7; i++) {
            if (player.hasPermission("extraInventory.rows." + i)) {
                slots = i * 9;
            }
        }
        this.loadedInventories.get(player.getUniqueId()).slots(slots);
    }

    public Map<UUID, ExtraInventory> loadedInventories() {
        return this.loadedInventories;
    }

    public Map<UUID, Inventory> cachedInventories() {
        return this.cachedInventories;
    }

    public boolean spying(UUID uuid) {
        return this.spying.contains(uuid);
    }

    public List<UUID> spying() {
        return this.spying;
    }
}
