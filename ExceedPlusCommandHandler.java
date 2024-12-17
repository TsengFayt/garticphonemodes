package net.rhythmcore.exceedplus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Merged command handler with all original logic plus new disenchant/clean commands and full template sets.
 */
public class ExceedPlusCommandHandler implements CommandExecutor, TabCompleter {

    private final ExceedPlus plugin;

    public ExceedPlusCommandHandler(ExceedPlus plugin) {
        this.plugin = plugin;
    }

    // Combine standard and special templates in TEMPLATE_IDS:
    // Standard templates:
    // grind_template (700000)
    // frame_template (700001)
    // exceed_template (700002)
    // special templates (700010 onwards), as listed previously:
    private static final Map<String, Integer> TEMPLATE_IDS = Map.ofEntries(
        Map.entry("grind_template", 700000),
        Map.entry("frame_template", 700001),
        Map.entry("exceed_template", 700002),
        Map.entry("template_sword_a", 700010),
        Map.entry("template_sword_b", 700011),
        Map.entry("template_axe_a", 700012),
        Map.entry("template_axe_b", 700013),
        Map.entry("template_pickaxe_a", 700014),
        Map.entry("template_pickaxe_b", 700015),
        Map.entry("template_shovel_a", 700016),
        Map.entry("template_shovel_b", 700017),
        Map.entry("template_hoe_a", 700018),
        Map.entry("template_hoe_b", 700019),
        Map.entry("template_bow_a", 700020),
        Map.entry("template_bow_b", 700021),
        Map.entry("template_crossbow_a", 700022),
        Map.entry("template_crossbow_b", 700023),
        Map.entry("template_trident_a", 700024),
        Map.entry("template_trident_b", 700025),
        Map.entry("template_mace_a", 700026),
        Map.entry("template_mace_b", 700027)
    );

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // If no args, show usage
        if (args.length < 1) {
            sender.sendMessage("Usage: /exceed <help|reload|give|upgrade|disenchant|clean>");
            return true;
        }

        // help command
        if (args[0].equalsIgnoreCase("help")) {
            sender.sendMessage("Available commands:\n" +
                    "/exceed help - Show this help menu\n" +
                    "/exceed reload - Reloads the plugin configuration\n" +
                    "/exceed give <player> <template> [count] - Gives templates\n" +
                    "/exceed upgrade [count|max] - Upgrades held item\n" +
                    "/exceed disenchant - Remove all enchantments from held item\n" +
                    "/exceed clean - Reset held item to vanilla state"
            );
            return true;
        }

        // reload command
        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            sender.sendMessage("ExceedPlus configuration reloaded.");
            return true;
        }

        // give command
        if (args[0].equalsIgnoreCase("give") && args.length >= 3) {
            String targetName = args[1];
            String templateName = args[2].toLowerCase();
            int count = 1;
            if (args.length > 3) {
                try {
                    count = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("Invalid count. Must be a number.");
                    return true;
                }
            }

            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                sender.sendMessage("Player not found.");
                return true;
            }

            if (templateName.equals("template_set")) {
                giveTemplateSet(target, count);
                sender.sendMessage("Gave " + count + " template set(s) to " + target.getName());
                return true;
            }

            if (TEMPLATE_IDS.containsKey(templateName)) {
                giveTemplate(target, templateName, count);
                sender.sendMessage("Gave " + count + " " + templateName + " to " + target.getName());
                return true;
            } else {
                sender.sendMessage("Invalid template name.");
                return true;
            }
        }

        // upgrade command
        if (args[0].equalsIgnoreCase("upgrade")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can use this command.");
                return true;
            }

            ItemStack handItem = player.getInventory().getItemInMainHand();
            if (handItem == null || handItem.getType() == Material.AIR) {
                player.sendMessage("You must hold an upgradable item in your main hand.");
                return true;
            }

            int currentLevel = ExceedPlusUtil.getItemLevel(plugin, handItem);
            if (currentLevel >= 10) {
                player.sendMessage("Item is already at max level.");
                return true;
            }

            int levelsToAdd = 1;
            if (args.length > 1) {
                if (args[1].equalsIgnoreCase("max")) {
                    levelsToAdd = 10 - currentLevel;
                } else {
                    try {
                        levelsToAdd = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        player.sendMessage("Invalid number. Use a number 1-9 or 'max'.");
                        return true;
                    }
                    if (levelsToAdd < 1) {
                        player.sendMessage("Upgrade count must be >= 1.");
                        return true;
                    }
                }
            }

            int newLevel = Math.min(currentLevel + levelsToAdd, 10);
            ExceedPlusUtil.upgradeItem(plugin, handItem, newLevel);
            player.getInventory().setItemInMainHand(handItem);
            player.sendMessage("Item upgraded to level " + newLevel + ".");
            return true;
        }

        // disenchant command
        if (args[0].equalsIgnoreCase("disenchant")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("Players only.");
                return true;
            }
            if (!p.hasPermission("exceedplus.disenchant")) {
                p.sendMessage("No permission.");
                return true;
            }
            ItemStack item = p.getInventory().getItemInMainHand();
            if (item == null || item.getType() == Material.AIR) {
                p.sendMessage("Hold an item to disenchant.");
                return true;
            }
            ItemStack cleaned = ExceedPlusItemUtil.disenchantItem(item);
            p.getInventory().setItemInMainHand(cleaned);
            p.sendMessage("Item disenchanted.");
            return true;
        }

        // clean command
        if (args[0].equalsIgnoreCase("clean")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("Players only.");
                return true;
            }
            if (!p.hasPermission("exceedplus.clean")) {
                p.sendMessage("No permission.");
                return true;
            }
            ItemStack item = p.getInventory().getItemInMainHand();
            if (item == null || item.getType() == Material.AIR) {
                p.sendMessage("Hold an item to clean.");
                return true;
            }
            ItemStack vanilla = ExceedPlusItemUtil.cleanItem(item);
            p.getInventory().setItemInMainHand(vanilla);
            p.sendMessage("Item reset to vanilla state.");
            return true;
        }

        sender.sendMessage("Invalid subcommand. Use /exceed help for a list of commands.");
        return true;
    }

    private void giveTemplate(Player player, String templateName, int count) {
        int customModelData = TEMPLATE_IDS.get(templateName);
        String loreKey = templateName + "_lore";

        ItemStack template = new ItemStack(Material.STRUCTURE_BLOCK, count);
        ItemMeta meta = template.getItemMeta();
        if (meta != null) {
            // Check if template is special or normal:
            // Normal templates stored under "templates.<loreKey>"
            // Special templates stored under "specialtemplates.<templateName>"

            String configPath;
            if (templateName.startsWith("template_") && templateName.contains("_a") || templateName.contains("_b")) {
                // It's a special template
                configPath = "specialtemplates." + templateName;
            } else {
                // It's a normal template
                configPath = "templates." + loreKey;
            }

            String configName = plugin.getConfig().getString(configPath + ".name", templateName.replace("_", " ").toUpperCase());
            List<String> configLore = plugin.getConfig().getStringList(configPath + ".lore");

            configName = ColorUtil.translate(configName);
            List<String> translatedLore = new ArrayList<>();
            for (String line : configLore) {
                translatedLore.add(ColorUtil.translate(line));
            }

            meta.setDisplayName(configName);
            meta.setCustomModelData(customModelData);
            if (!translatedLore.isEmpty()) {
                meta.setLore(translatedLore);
            }

            template.setItemMeta(meta);
        }
        player.getInventory().addItem(template);
    }

    private void giveTemplateSet(Player player, int count) {
        // The template set presumably includes the three main templates:
        giveTemplate(player, "grind_template", 6 * count);
        giveTemplate(player, "frame_template", 3 * count);
        giveTemplate(player, "exceed_template", count);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("help");
            completions.add("reload");
            completions.add("give");
            completions.add("upgrade");
            completions.add("disenchant");
            completions.add("clean");
        } else if (args[0].equalsIgnoreCase("give")) {
            if (args.length == 2) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            } else if (args.length == 3) {
                // templates
                completions.add("template_set");
                completions.addAll(TEMPLATE_IDS.keySet());
            } else if (args.length == 4) {
                completions.add("<count>");
            }
        } else if (args[0].equalsIgnoreCase("upgrade") && args.length == 2) {
            completions.add("max");
            for (int i=1; i<=9; i++) {
                completions.add(String.valueOf(i));
            }
        }
        return completions;
    }
}
