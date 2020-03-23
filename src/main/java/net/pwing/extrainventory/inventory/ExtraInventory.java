package net.pwing.extrainventory.inventory;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class ExtraInventory {

    private int slots;
    private ItemStack[] contents;

    public ExtraInventory(int slots, ItemStack[] contents) {
        this.slots = slots;
        this.contents = contents;
    }

    public Optional<Inventory> inventory() {
        if (this.slots == 0) {
            return Optional.empty();
        }
        Inventory inventory = Bukkit.createInventory(null, this.slots, "Extra Inventory");
        if (inventory.getSize() > this.slots) {
            for (int i = 0; i < this.slots; i++) {
                if (this.contents[i] == null) {
                    continue;
                }
                inventory.setItem(i, this.contents[i]);
            }
        } else {
            inventory.setContents(this.contents);
        }
        return Optional.of(inventory);
    }

    public int slots() {
        return slots;
    }

    public void slots(int slots) {
        this.slots = slots;
    }

    public ItemStack[] contents() {
        return contents;
    }

    public void contents(ItemStack[] contents) {
        this.contents = contents;
    }
}
