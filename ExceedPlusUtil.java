package net.rhythmcore.exceedplus;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ExceedPlusUtil {

    /**
     * Retrieve the current item level from the weapon's Persistent Data Container (PDC).
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

        // Update the lore with stars and bonus stats
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

        // Remove existing stars and bonus lines
        removeStarLines(lore);
        clearBonusLoreLines(lore);

        // Add updated stars
        lore.addAll(generateStarsLore(plugin, level)); // Add stars at the beginning of the lore

        // Add bonus damage and speed lines
        addBonusLoreLines(plugin, lore);

        meta.setLore(lore);
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

        List<String> lore = item.hasItemMeta() && item.getItemMeta().hasLore()
                ? new ArrayList<>(item.getItemMeta().getLore())
                : new ArrayList<>();

        // Remove existing stars and bonus lines
        removeStarLines(lore);
        clearBonusLoreLines(lore);

        // Add updated stars
        lore.addAll(generateStarsLore(plugin, newLevel));

        // Add bonus damage and speed lines
        addBonusLoreLines(plugin, lore);

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
        if (value == (int) value) {
            return String.valueOf((int) value);
        } else {
            return String.format("%.2f", value);
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

    /**
     * Remove existing stars from the lore.
     */
    private static void removeStarLines(List<String> lore) {
        if (!lore.isEmpty()) {
            String first = ColorUtil.stripColor(lore.get(0)).toLowerCase();
            if (first.contains("★") || first.contains("☆")) lore.remove(0);
        }
    }

    /**
     * Remove bonus damage and speed lines from the lore.
     */
    private static void removeBonusLoreLines(List<String> lore) {
        lore.removeIf(line -> ColorUtil.stripColor(line).contains("Bonus Damage") ||
                              ColorUtil.stripColor(line).contains("Bonus Speed"));
    }

    /**
     * Clear existing bonus lore lines before adding updated ones.
     */
    private static void clearBonusLoreLines(List<String> lore) {
        removeBonusLoreLines(lore);
    }

    /**
     * Add bonus damage and speed lines to the lore based on current values.
     */
    private static void addBonusLoreLines(ExceedPlus plugin, List<String> lore) {
        // Retrieve current Bonus Damage and Bonus Speed
        double bonusDamage = getBonusDamage(plugin, lore);
        double bonusSpeed = getBonusSpeed(plugin, lore);

        // Only add if values are greater than 0
        if (bonusDamage > 0.0) {
            String dmgLine = plugin.getConfig().getString("lore.bonus_damage_line", "&a+{value} Bonus Damage")
                    .replace("{value}", formatIncrementValue(bonusDamage));
            lore.add(ColorUtil.translate(dmgLine));
        }

        if (bonusSpeed > 0.0) {
            String spdLine = plugin.getConfig().getString("lore.bonus_speed_line", "&a+{value} Bonus Speed")
                    .replace("{value}", formatIncrementValue(bonusSpeed));
            lore.add(ColorUtil.translate(spdLine));
        }
    }

    /**
     * Retrieve the current Bonus Damage from the item's lore.
     */
    public static double getBonusDamage(ExceedPlus plugin, List<String> lore) {
        for (String line : lore) {
            String stripped = ColorUtil.stripColor(line).toLowerCase();
            if (stripped.contains("bonus damage")) {
                // Extract the numerical value
                String[] parts = stripped.split("\\+| ");
                for (String part : parts) {
                    try {
                        return Double.parseDouble(part);
                    } catch (NumberFormatException e) {
                        // Continue searching
                    }
                }
            }
        }
        return 0.0;
    }

    /**
     * Retrieve the current Bonus Speed from the item's lore.
     */
    public static double getBonusSpeed(ExceedPlus plugin, List<String> lore) {
        for (String line : lore) {
            String stripped = ColorUtil.stripColor(line).toLowerCase();
            if (stripped.contains("bonus speed")) {
                // Extract the numerical value
                String[] parts = stripped.split("\\+| ");
                for (String part : parts) {
                    try {
                        return Double.parseDouble(part);
                    } catch (NumberFormatException e) {
                        // Continue searching
                    }
                }
            }
        }
        return 0.0;
    }

    /**
     * Set the Bonus Damage value in the item's Persistent Data Container.
     */
    public static void setBonusDamage(ExceedPlus plugin, ItemStack item, double damage) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey DAMAGE_KEY = new NamespacedKey(plugin, "damage_increment");
        meta.getPersistentDataContainer().set(DAMAGE_KEY, PersistentDataType.DOUBLE, damage);
        item.setItemMeta(meta);
    }

    /**
     * Set the Bonus Speed value in the item's Persistent Data Container.
     */
    public static void setBonusSpeed(ExceedPlus plugin, ItemStack item, double speed) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey SPEED_KEY = new NamespacedKey(plugin, "speed_increment");
        meta.getPersistentDataContainer().set(SPEED_KEY, PersistentDataType.DOUBLE, speed);
        item.setItemMeta(meta);
    }

    /**
     * Clear Bonus Damage and Bonus Speed from the item's lore and PDC.
     */
    public static void clearBonusIncrements(ExceedPlus plugin, ItemStack item) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey DAMAGE_KEY = new NamespacedKey(plugin, "damage_increment");
        NamespacedKey SPEED_KEY = new NamespacedKey(plugin, "speed_increment");
        meta.getPersistentDataContainer().remove(DAMAGE_KEY);
        meta.getPersistentDataContainer().remove(SPEED_KEY);
        item.setItemMeta(meta);
    }
}
