package net.rhythmcore.exceedplus;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.Map;

public class ExceedPlusItemUtil {
    public static ItemStack disenchantItem(ItemStack item) {
        ItemStack clone = item.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta != null) {
            Map<Enchantment,Integer> enchants = meta.getEnchants();
            for (Enchantment e : enchants.keySet()) {
                meta.removeEnchant(e);
            }
            clone.setItemMeta(meta);
        }
        return clone;
    }

    public static ItemStack cleanItem(ItemStack item) {
        // Return a vanilla equivalent
        Material type = item.getType();
        return new ItemStack(type);
    }
}
