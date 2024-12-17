package net.rhythmcore.exceedplus;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ExceedPlusUtil {

    /**
     * Retrieve the current item level from the weapon's PD.
     */
    public static int getItemLevel(ExceedPlus plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        NamespacedKey LEVEL_KEY = new NamespacedKey(plugin, "weapon_level");
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(LEVEL_KEY, PersistentDataType.INTEGER, 0);
    }

    /**
     * Set the new level for the item and update only the PersistentData and display name.
     * This does not alter the lore beyond updating the name.
     * Use this when you only want to ensure the level is set and name updated without altering lore lines.
     */
    public static void setItemLevel(ExceedPlus plugin, ItemStack item, int level) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey LEVEL_KEY = new NamespacedKey(plugin, "weapon_level");
        meta.getPersistentDataContainer().set(LEVEL_KEY, PersistentDataType.INTEGER, level);

        String weaponNameColor = plugin.getConfig().getString("lore.weapon_name_color", "&f");
        String levelSymbol = plugin.getConfig().getString("lore.weapon_level_symbol.symbol", "+");
        String levelColor = plugin.getConfig().getString("lore.weapon_level_symbol.color", "&c");

        String baseMaterialName = formatMaterialName(item.getType());
        String rawName = weaponNameColor + baseMaterialName + " " + levelColor + levelSymbol + level;
        String finalDisplayName = ColorUtil.translate(rawName);

        meta.setDisplayName(finalDisplayName);
        item.setItemMeta(meta);
    }

    /**
     * Sets the item's level and updates the display name as well as adding the stars lore line.
     * This is a utility method used by commands or other parts of the code to directly
     * upgrade an item outside the smithing table logic and ensure stars are shown.
     */
    public static void upgradeItem(ExceedPlus plugin, ItemStack item, int newLevel) {
        if (item == null || item.getType() == Material.AIR) return;

        // Update display name and add stars line
        String weaponNameColor = plugin.getConfig().getString("lore.weapon_name_color", "&f");
        String levelSymbol = plugin.getConfig().getString("lore.weapon_level_symbol.symbol", "+");
        String levelColor = plugin.getConfig().getString("lore.weapon_level_symbol.color", "&c");

        String baseMaterialName = formatMaterialName(item.getType());
        String rawName = weaponNameColor + baseMaterialName + " " + levelColor + levelSymbol + newLevel;
        String finalDisplayName = ColorUtil.translate(rawName);

        List<String> lore = generateStarsLore(plugin, newLevel);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            NamespacedKey LEVEL_KEY = new NamespacedKey(plugin, "weapon_level");
            meta.getPersistentDataContainer().set(LEVEL_KEY, PersistentDataType.INTEGER, newLevel);
            meta.setDisplayName(finalDisplayName);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
    }

    /**
     * Generate the stars lore line based on the item level.
     */
    public static List<String> generateStarsLore(ExceedPlus plugin, int level) {
        String filledStars = plugin.getConfig().getString("lore.filledstars", "★");
        String emptyStars = plugin.getConfig().getString("lore.emptystars", "☆");
        boolean showEmptyStars = plugin.getConfig().getBoolean("advanced.show_empty_stars", true);

        StringBuilder stars = new StringBuilder();
        for (int i = 1; i <= 10; i++) {
            String rawColor = getStarColor(plugin, i, i <= level);
            if (i <= level) {
                stars.append(rawColor).append(filledStars);
            } else if (showEmptyStars) {
                stars.append(rawColor).append(emptyStars);
            }
        }

        String translatedLine = ColorUtil.translate(stars.toString());
        List<String> line = new ArrayList<>();
        line.add(translatedLine);
        return line;
    }

    private static String getStarColor(ExceedPlus plugin, int starIndex, boolean filled) {
        String colorKey;
        if (starIndex <= 3) {
            colorKey = "lore.level_colors.level_0_to_3";
        } else if (starIndex <= 6) {
            colorKey = "lore.level_colors.level_4_to_6";
        } else if (starIndex <= 9) {
            colorKey = "lore.level_colors.level_7_to_9";
        } else {
            colorKey = "lore.level_colors.level_10";
        }
        String defaultColor = filled ? "&a" : "&7";
        return plugin.getConfig().getString(colorKey, defaultColor);
    }

    /**
     * Format increment values to remove decimal if not needed.
     */
    public static String formatIncrementValue(double value) {
        if (value == (int)value) {
            return String.valueOf((int)value);
        } else {
            return String.valueOf(value);
        }
    }

    private static String formatMaterialName(Material material) {
        String[] parts = material.name().split("_");
        StringBuilder formatted = new StringBuilder();
        for (String part : parts) {
            formatted.append(part.charAt(0)).append(part.substring(1).toLowerCase()).append(" ");
        }
        return formatted.toString().trim();
    }
}
