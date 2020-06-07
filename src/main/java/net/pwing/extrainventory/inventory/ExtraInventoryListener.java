package net.pwing.extrainventory.inventory;

import net.pwing.extrainventory.ExtraInventoryPlugin;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

import java.io.IOException;

public class ExtraInventoryListener implements Listener {

    private ExtraInventoryPlugin plugin;

    public ExtraInventoryListener(ExtraInventoryPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        try {
            this.plugin.inventoryManager().loadedInventories().put(event.getPlayer().getUniqueId(),
                    this.plugin.inventoryManager().load(event.getPlayer()));
            this.plugin.inventoryManager().refresh(event.getPlayer());
        } catch (ClassNotFoundException | IOException ex) {
            this.plugin.getLogger().warning("Failed to load inventory for " + event.getPlayer() + "!");
            ex.printStackTrace();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.plugin.inventoryManager().save(event.getPlayer());
        this.plugin.inventoryManager().loadedInventories().remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals("Extra Inventory")) {
            return;
        }

        HumanEntity human = event.getPlayer();
        boolean spying = this.plugin.inventoryManager().spying(human.getUniqueId());
        ExtraInventory extraInventory = this.plugin.inventoryManager().loadedInventories().get(human.getUniqueId());
        if (!spying && extraInventory != null) {
            this.plugin.inventoryManager().refresh((Player) human);
            extraInventory.contents(event.getView().getTopInventory().getContents());
            return;
        }

        Inventory inventory = this.plugin.inventoryManager().cachedInventories().get(human.getUniqueId());
        if (!spying || inventory == null || !event.getView().getTopInventory().equals(inventory)) {
            return;
        }
        this.plugin.inventoryManager().spying().remove(human.getUniqueId());
        this.plugin.inventoryManager().cachedInventories().remove(human.getUniqueId());
    }

    @EventHandler
    public void onInteract(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("Extra Inventory")) {
            return;
        }

        HumanEntity human = event.getWhoClicked();
        Inventory inventory = this.plugin.inventoryManager().cachedInventories().get(human.getUniqueId());
        if (inventory == null || !event.getView().getTopInventory().equals(inventory)) {
            return;
        }
        boolean spying = this.plugin.inventoryManager().spying(human.getUniqueId());
        if (spying) {
            event.setCancelled(true);
        }
    }
}
