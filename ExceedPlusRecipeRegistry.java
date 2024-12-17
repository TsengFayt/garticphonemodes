package net.rhythmcore.exceedplus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Merged class combining original logic and new special template logic.
 * Retains duplication event handling and standard templates.
 * Adds special templates for level 10 items.
 */
public class ExceedPlusRecipeRegistry implements Listener {

    private final JavaPlugin plugin;
    private NamespacedKey duplicationKey;

    public ExceedPlusRecipeRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void registerRecipes() {
        // Original standard templates
        registerGrindTemplateRecipe();
        registerFrameTemplateRecipe();
        registerExceedTemplateRecipe();
        registerDuplicationRecipe();

        // New special templates
        registerAllSpecialTemplates();
    }

    private void registerGrindTemplateRecipe() {
        ItemStack result = createTemplateItem(700000, "grind_template_lore", "Grind Template");
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "grind_template"), result);
        recipe.shape("ACA", "APA", "AAA");
        recipe.setIngredient('A', Material.AMETHYST_SHARD);
        recipe.setIngredient('C', Material.END_CRYSTAL);
        recipe.setIngredient('P', Material.PAPER);
        Bukkit.addRecipe(recipe);
    }

    private void registerFrameTemplateRecipe() {
        ItemStack result = createTemplateItem(700001, "frame_template_lore", "Frame Template");
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "frame_template"), result);
        recipe.shape("ANA", "APA", "AAA");
        recipe.setIngredient('A', Material.AMETHYST_SHARD);
        recipe.setIngredient('N', Material.NETHER_STAR);
        recipe.setIngredient('P', Material.PAPER);
        Bukkit.addRecipe(recipe);
    }

    private void registerExceedTemplateRecipe() {
        ItemStack result = createTemplateItem(700002, "exceed_template_lore", "Exceed Template");
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "exceed_template"), result);
        recipe.shape("ENE", "EPE", "EEE");
        recipe.setIngredient('E', Material.ECHO_SHARD);
        recipe.setIngredient('N', Material.NETHER_STAR);
        recipe.setIngredient('P', Material.PAPER);
        Bukkit.addRecipe(recipe);
    }

    private void registerDuplicationRecipe() {
        // Start with a generic structure block for duplication
        ItemStack result = new ItemStack(Material.STRUCTURE_BLOCK, 2);
        duplicationKey = new NamespacedKey(plugin, "template_duplication");
        ShapedRecipe recipe = new ShapedRecipe(duplicationKey, result);
        recipe.shape("DXD", "DPD", "DDD");
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('P', Material.PAPER);
        // X ingredient is a choice of any known template - standard or special.
        // We only listed standard templates in original code. Let's add them here.
        recipe.setIngredient('X', new RecipeChoice.ExactChoice(
            createTemplateItem(700000, "grind_template_lore", "Grind Template"),
            createTemplateItem(700001, "frame_template_lore", "Frame Template"),
            createTemplateItem(700002, "exceed_template_lore", "Exceed Template"),
            createTemplateItem(700010, "template_sword_a_lore", "Sword Unlock Potential Strike Template"),
            createTemplateItem(700011, "template_sword_b_lore", "Sword Unlock Potential Swift Template"),
            createTemplateItem(700012, "template_axe_a_lore", "Axe Unlock Potential Strike Template"),
            createTemplateItem(700013, "template_axe_b_lore", "Axe Unlock Potential Swift Template"),
            createTemplateItem(700014, "template_pickaxe_a_lore", "Pickaxe Unlock Potential Strike Template"),
            createTemplateItem(700015, "template_pickaxe_b_lore", "Pickaxe Unlock Potential Swift Template"),
            createTemplateItem(700016, "template_shovel_a_lore", "Shovel Unlock Potential Strike Template"),
            createTemplateItem(700017, "template_shovel_b_lore", "Shovel Unlock Potential Swift Template"),
            createTemplateItem(700017, "template_hoe_a_lore", "Hoe Unlock Potential Strike Template"),
            createTemplateItem(700019, "template_hoe_b_lore", "Hoe Unlock Potential Swift Template"),
            createTemplateItem(700020, "template_bow_a_lore", "Bow Unlock Potential Strike Template"),
            createTemplateItem(700021, "template_bow_b_lore", "Bow Unlock Potential Swift Template"),
            createTemplateItem(700022, "template_crossbow_a_lore", "Crossbow Unlock Potential Strike Template"),
            createTemplateItem(700023, "template_crossbow_b_lore", "Crossbow Unlock Potential Swift Template"),
            createTemplateItem(700024, "template_trident_a_lore", "Trident Unlock Potential Strike Template"),
            createTemplateItem(700025, "template_trident_b_lore", "Trident Unlock Potential Swift Template"),
            createTemplateItem(700026, "template_mace_a_lore", "Mace Unlock Potential Strike Template"),
            createTemplateItem(700027, "template_mace_b_lore", "Mace Unlock Potential Swift Template")
        ));
        Bukkit.addRecipe(recipe);
    }

    /**
     * Create standard templates from config for normal templates
     */
    private ItemStack createTemplateItem(int customModelData, String loreKey, String defaultName) {
        ItemStack item = new ItemStack(Material.STRUCTURE_BLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String rawName = plugin.getConfig().getString("templates." + loreKey + ".name", defaultName);
            List<String> rawLore = plugin.getConfig().getStringList("templates." + loreKey + ".lore");

            String translatedName = ColorUtil.translate(rawName);
            List<String> translatedLore = new ArrayList<>();
            for (String line : rawLore) {
                translatedLore.add(ColorUtil.translate(line));
            }

            meta.setDisplayName(translatedName);
            meta.setCustomModelData(customModelData);
            if (!translatedLore.isEmpty()) {
                meta.setLore(translatedLore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Register all special templates listed.
     * The user gave a full list of templates, each with a special item.
     * We'll create a map to handle them all.
     */
    private void registerAllSpecialTemplates() {
        // Map template_name -> (customModelData, specialMaterial)
        Map<String, Object[]> specialTemplates = Map.ofEntries(
            Map.entry("template_sword_a", new Object[]{700010, Material.BLAZE_ROD}),
            Map.entry("template_sword_b", new Object[]{700011, Material.GHAST_TEAR}),
            Map.entry("template_axe_a", new Object[]{700012, Material.CRIMSON_HYPHAE}),
            Map.entry("template_axe_b", new Object[]{700013, Material.WARPED_HYPHAE}),
            Map.entry("template_pickaxe_a", new Object[]{700014, Material.REDSTONE_ORE}),
            Map.entry("template_pickaxe_b", new Object[]{700015, Material.LAPIS_ORE}),
            Map.entry("template_shovel_a", new Object[]{700016, Material.SOUL_SAND}),
            Map.entry("template_shovel_b", new Object[]{700017, Material.SOUL_SOIL}),
            Map.entry("template_hoe_a", new Object[]{700018, Material.NETHER_WART}),
            Map.entry("template_hoe_b", new Object[]{700019, Material.BEETROOT_SEEDS}),
            Map.entry("template_bow_a", new Object[]{700020, Material.SPECTRAL_ARROW}),
            Map.entry("template_bow_b", new Object[]{700021, Material.TIPPED_ARROW}),
            Map.entry("template_crossbow_a", new Object[]{700022, Material.TRIPWIRE_HOOK}), // arrow of strength variant, also TIPPED_ARROW
            Map.entry("template_crossbow_b", new Object[]{700023, Material.IRON_BARS}), // arrow of swiftness variant
            Map.entry("template_trident_a", new Object[]{700024, Material.HEART_OF_THE_SEA}),
            Map.entry("template_trident_b", new Object[]{700025, Material.PRISMARINE_CRYSTALS}),
            Map.entry("template_mace_a", new Object[]{700026, Material.HEAVY_CORE}),
            Map.entry("template_mace_b", new Object[]{700027, Material.BREEZE_ROD})
        );

        for (Map.Entry<String, Object[]> entry : specialTemplates.entrySet()) {
            String keyName = entry.getKey();
            int cmd = (int) entry.getValue()[0];
            Material specialItem = (Material) entry.getValue()[1];
            registerSpecialTemplate(keyName, cmd, specialItem);
        }
    }

    /**
     * Register a special template as described:
     * shape: ESE, EFE, EEE
     * E=Echo Shard, F=Fire Charge, S=Special Item
     */
    private void registerSpecialTemplate(String keyName, int customModelData, Material specialItem) {
        ItemStack result = createSpecialTemplateItem(customModelData, keyName);
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, keyName), result);
        recipe.shape("ESE","EFE","EEE");
        recipe.setIngredient('E', Material.ECHO_SHARD);
        recipe.setIngredient('F', Material.FIRE_CHARGE);
        recipe.setIngredient('S', specialItem);
        Bukkit.addRecipe(recipe);
    }

    /**
     * Create special template item from config:
     * specialtemplates.<keyName>.name and .lore
     */
    private ItemStack createSpecialTemplateItem(int customModelData, String configKey) {
        ItemStack item = new ItemStack(Material.STRUCTURE_BLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = plugin.getConfig().getString("specialtemplates."+configKey+".name","&fSpecial Template");
            name = ColorUtil.translate(name);
            meta.setDisplayName(name);
            meta.setCustomModelData(customModelData);

            List<String> rawLore = plugin.getConfig().getStringList("specialtemplates."+configKey+".lore");
            List<String> lore = new ArrayList<>();
            for (String line : rawLore) {
                lore.add(ColorUtil.translate(line));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Duplication event handling from original code.
     * If we want to include special templates in duplication:
     * we'd need to expand the 'X' choice in registerDuplicationRecipe or
     * handle them dynamically here by reading from matrix.
     */
    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null) return;
        if (!(event.getRecipe() instanceof ShapedRecipe shapedRecipe)) return;

        // Check if this is the duplication recipe
        if (duplicationKey != null && shapedRecipe.getKey().equals(duplicationKey)) {
            CraftingInventory inv = event.getInventory();
            ItemStack[] matrix = inv.getMatrix();

            // 'X' is at matrix index 1 as per original logic
            ItemStack templateItem = matrix[1];
            if (templateItem == null || templateItem.getType() == Material.AIR) {
                return;
            }

            ItemStack result = inv.getResult();
            if (result == null || result.getType() == Material.AIR) return;

            ItemMeta templateMeta = templateItem.getItemMeta();
            if (templateMeta == null) return;
            int customModelData = templateMeta.hasCustomModelData() ? templateMeta.getCustomModelData() : 700000;

            ItemMeta resultMeta = result.getItemMeta();
            if (resultMeta != null) {
                resultMeta.setCustomModelData(customModelData);
                if (templateMeta.hasDisplayName()) {
                    resultMeta.setDisplayName(templateMeta.getDisplayName());
                }
                if (templateMeta.hasLore()) {
                    resultMeta.setLore(new ArrayList<>(templateMeta.getLore()));
                }
                result.setItemMeta(resultMeta);
            }

            inv.setResult(result);
        }
    }
}
