package net.pwing.extrainventory.inventory;

import net.pwing.extrainventory.ExtraInventoryPlugin;
import net.pwing.extrainventory.util.ExtraInventoryUtil;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
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

    public boolean needsConversion(OfflinePlayer player) {
        if (!has(player)) {
            return false;
        }
        Path path = Paths.get(this.plugin.getDataFolder().toString(), "data", player.getUniqueId().toString() + ".yml");
        FileConfiguration invConfig;
        try {
            invConfig = YamlConfiguration.loadConfiguration(Files.newBufferedReader(path));
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
        return !invConfig.contains("inventory");
    }

    public ExtraInventory load(OfflinePlayer player) throws ClassNotFoundException, IOException {
        Path path = Paths.get(this.plugin.getDataFolder().toString(), "data", player.getUniqueId().toString() + ".yml");
        if (Files.notExists(path)) {
            Files.createFile(path);
        }

        FileConfiguration invConfig = YamlConfiguration.loadConfiguration(Files.newBufferedReader(path));
        ConfigurationSection section = invConfig.getConfigurationSection("inventory");
        if (section != null) {
            int slots = section.getInt("slots");
            ItemStack[] items = new ItemStack[slots];
            for (String key : section.getKeys(false)) {
                if (key.equalsIgnoreCase("slots")) {
                    continue;
                }
                if (!integer(key)) {
                    continue;
                }
                items[Integer.parseInt(key)] = section.getItemStack(key);
            }
            return new ExtraInventory(slots, items);
        }

        // legacy storage
        if (invConfig.contains("inv")) {
            this.plugin.getLogger().info("Loading legacy inventory for " + player.getName() + ".");
            String serializedData = invConfig.getString("inv");
            if (serializedData != null) {
                Optional<Inventory> inventory = ExtraInventoryUtil.fromBase64(serializedData);
                if (inventory.isPresent()) {
                    return new ExtraInventory(inventory.get().getSize(), inventory.get().getContents());
                } else {
                    this.plugin.getLogger().warning("Failed to load extra inventory for " + player.getName() + "!");
                }
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
            invConfig.set("inventory.slots", inventory.slots());
            for (int slot = 0; slot < inventory.contents().length; slot++) {
                invConfig.set("inventory." + slot, inventory.contents()[slot]);
            }
            invConfig.set("inv", null);
            invConfig.save(path.toFile());
        } catch (IOException ex) {
            this.plugin.getLogger().warning("Failed to save inventory for " + player.getName() + "!");
            ex.printStackTrace();
        }
    }

    public void convert(OfflinePlayer player) throws IOException, ClassNotFoundException {
        this.loadedInventories.put(player.getUniqueId(), this.load(player));
        this.save(player);
        this.loadedInventories.remove(player.getUniqueId());
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

    private boolean integer(String key) {
        try {
            Integer.parseInt(key);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
