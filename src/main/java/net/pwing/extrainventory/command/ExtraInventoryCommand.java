package net.pwing.extrainventory.command;

import net.pwing.extrainventory.ExtraInventoryPlugin;
import net.pwing.extrainventory.inventory.ExtraInventory;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class ExtraInventoryCommand implements CommandExecutor {

    private ExtraInventoryPlugin plugin;

    public ExtraInventoryCommand(ExtraInventoryPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be a player to use this command!");
            return true;
        }

        Player player = (Player) sender;
        if (args.length == 0) {
            if (!player.hasPermission("extraInventory.use")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to execute this command!");
                return true;
            }

            ExtraInventory inventory = this.plugin.inventoryManager().loadedInventories().get(player.getUniqueId());
            if (inventory == null) {
                player.sendMessage(ChatColor.RED + "Failed to load extra inventory (try relogging).");
                return true;
            }
            inventory.inventory().ifPresent(inv -> {
                player.openInventory(inv);
                this.plugin.inventoryManager().cachedInventories().put(player.getUniqueId(), inv);
                this.plugin.inventoryManager().refresh(player);
            });
            return true;
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("spy")) {
                if (!player.hasPermission("extraInventory.spy")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to execute this command!");
                    return true;
                }
                // Offline player lookups are sync (dum Bukkit), so we need to wrap in a future
                CompletableFuture.supplyAsync(() -> Bukkit.getOfflinePlayer(args[1])).whenComplete((target, ex) -> {
                    ExtraInventory inventory = this.plugin.inventoryManager().loadedInventories().get(target.getUniqueId());
                    if (inventory == null) {
                        if (this.plugin.inventoryManager().has(target)) {
                            try {
                                inventory = this.plugin.inventoryManager().load(target);
                            } catch (ClassNotFoundException | IOException ex2) {
                                this.plugin.getLogger().warning("Failed to load inventory for " + target.getName() + "!");
                                ex2.printStackTrace();
                            }
                        }
                    }

                    if (inventory == null) {
                        player.sendMessage(ChatColor.RED + "That player does not have an extra inventory!");
                        return;
                    }

                    if (this.plugin.inventoryManager().needsConversion(target)) {
                        player.sendMessage(ChatColor.YELLOW + "The inventory being loaded is stored in a legacy format, converting...");
                        try {
                            this.plugin.inventoryManager().convert(target);
                        } catch (IOException | ClassNotFoundException e) {
                            this.plugin.getLogger().warning("Failed to convert inventory for " + target.getName() + "! Report this!");
                            e.printStackTrace();
                            return;
                        }
                        player.sendMessage(ChatColor.GREEN + "Conversion of inventory for player " + target.getName() + " successful!");
                    }

                    if (inventory.inventory().isPresent()) {
                        Inventory inv = inventory.inventory().get();
                        Bukkit.getScheduler().runTask(this.plugin, () -> {
                            player.openInventory(inv);
                            this.plugin.inventoryManager().cachedInventories().put(player.getUniqueId(), inv);
                            this.plugin.inventoryManager().spying().add(player.getUniqueId());
                            player.sendMessage(ChatColor.GREEN + "Opened the inventory for " + target.getName() + "!");
                        });
                    } else {
                        player.sendMessage(ChatColor.RED + "That player does not have an extra inventory! Failed to load data even though they have a saved inventory (report this?).");
                    }
                });
            }
            if (args[0].equalsIgnoreCase("convert")) {
                if (!player.hasPermission("extraInventory.convert")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to execute this command!");
                    return true;
                }
                player.sendMessage(ChatColor.BLUE + "Starting conversion of inventory for " + args[1] + "...");
                CompletableFuture.supplyAsync(() -> Bukkit.getOfflinePlayer(args[1])).whenComplete((target, ex) -> {
                    if (!this.plugin.inventoryManager().has(target)) {
                        player.sendMessage(ChatColor.RED + "That player does not have an extra inventory!");
                        return;
                    }
                    if (!this.plugin.inventoryManager().needsConversion(target)) {
                        player.sendMessage(ChatColor.RED + "This inventory does not need conversion!");
                        return;
                    }
                    try {
                        this.plugin.inventoryManager().convert(target);
                    } catch (IOException | ClassNotFoundException ex2) {
                        this.plugin.getLogger().warning("Failed to convert inventory for " + target.getName() + "! Report this!");
                        ex2.printStackTrace();
                        return;
                    }
                    player.sendMessage(ChatColor.GREEN + "Conversion of inventory for player " + target.getName() + " successful!");
                });
            }
        }
        return true;
    }
}
