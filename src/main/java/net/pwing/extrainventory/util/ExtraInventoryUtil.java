package net.pwing.extrainventory.util;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

public class ExtraInventoryUtil {

    /**
     * @deprecated updated to new storage format
     */
    @Deprecated
    public static String toBase64(int size, ItemStack[] contents) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BukkitObjectOutputStream objectStream = new BukkitObjectOutputStream(outputStream);
        objectStream.writeInt(size);
        for (ItemStack content : contents) {
            objectStream.writeObject(content);
        }
        outputStream.close();
        objectStream.close();
        return Base64Coder.encodeLines(outputStream.toByteArray());
    }

    /**
     * @deprecated updated to new storage format
     */
    @Deprecated
    public static Optional<Inventory> fromBase64(String base64) throws ClassNotFoundException, IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(base64));
        BukkitObjectInputStream objectStream = new BukkitObjectInputStream(inputStream);
        int slots = objectStream.readInt();
        // if (slots < 9) {
        //     return Optional.empty();
        // }
        Inventory inventory = Bukkit.createInventory(null, slots);
        for (int i = 0; i < slots; i++) {
            if (inputStream.available() == 0) {
                break;
            }
            inventory.setItem(i, (ItemStack) objectStream.readObject());
        }
        inputStream.close();
        objectStream.close();
        return Optional.of(inventory);
    }
}
