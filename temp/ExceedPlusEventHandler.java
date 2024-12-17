package net.rhythmcore.exceedplus;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class ExceedPlusEventHandler implements Listener {

    private final ExceedPlus plugin;
    private final NamespacedKey LEVEL_KEY;
    private final NamespacedKey SPECIAL_TEMPLATE_KEY;
    private final NamespacedKey INCREMENTS_KEY;
    private final NamespacedKey APPLIED_TEMPLATE_KEY;
    private final SoundFXUtil soundFX;
    private final Map<UUID, UpgradeAttempt> upgradeAttempts = new HashMap<>();

    private boolean toolTrimsLoaded = false;
    private NamespacedKey toolTrimKey;
    private Class<?> toolTrimClass;
    private Field toolTrim_Trims;
    private Field trimMaterialField;
    private Field trimTemplateField;

    private static final Set<Material> ALLOWED_BASE_ITEMS = Set.of(
        Material.NETHERITE_SWORD, Material.NETHERITE_AXE, Material.NETHERITE_PICKAXE,
        Material.NETHERITE_SHOVEL, Material.NETHERITE_HOE, Material.BOW,
        Material.CROSSBOW, Material.TRIDENT, Material.MACE
    );

    private static final Map<Integer, List<Integer>> TEMPLATE_LEVEL_MAP = Map.of(
        700000, Arrays.asList(0,1,2,3,4,5),
        700001, Arrays.asList(6,7,8),
        700002, Arrays.asList(9)
    );

    private static final Set<Integer> SPECIAL_TEMPLATES = Set.of(
        700010,700011,700012,700013,700014,700015,700016,700017,
        700018,700019,700020,700021,700022,700023,700024,700025,
        700026,700027
    );

    public ExceedPlusEventHandler(ExceedPlus plugin) {
        this.plugin = plugin;
        this.LEVEL_KEY = new NamespacedKey(plugin, "weapon_level");
        this.SPECIAL_TEMPLATE_KEY = new NamespacedKey(plugin, "special_template");
        this.INCREMENTS_KEY = new NamespacedKey(plugin, "upgrade_increments");
        this.APPLIED_TEMPLATE_KEY = new NamespacedKey(plugin, "applied_template");
        this.soundFX = new SoundFXUtil(plugin);
        detectToolTrims();
    }

    private void detectToolTrims() {
        try {
            Class<?> ttClass = Class.forName("net.sideways_sky.tooltrims.ToolTrim");
            toolTrimClass = ttClass;
            Field itemPDCField = toolTrimClass.getDeclaredField("ItemPDCKey");
            itemPDCField.setAccessible(true);
            Object keyObj = itemPDCField.get(null);
            if (keyObj instanceof NamespacedKey nk) {
                toolTrimKey = nk;
            } else {
                toolTrimsLoaded = false;
                return;
            }
            toolTrim_Trims = toolTrimClass.getDeclaredField("Trims");
            toolTrim_Trims.setAccessible(true);

            trimMaterialField = toolTrimClass.getDeclaredField("TrimMaterial");
            trimMaterialField.setAccessible(true);
            trimTemplateField = toolTrimClass.getDeclaredField("TrimTemplate");
            trimTemplateField.setAccessible(true);

            toolTrimsLoaded = true;
        } catch (ClassNotFoundException e) {
            toolTrimsLoaded = false;
        } catch (Exception e) {
            toolTrimsLoaded = false;
        }
    }

    @EventHandler
public void onPrepareSmithing(PrepareSmithingEvent event) {
    if (event.getView().getType() != InventoryType.SMITHING) return;

    ItemStack template = event.getInventory().getItem(0);
    ItemStack base = event.getInventory().getItem(1);
    ItemStack addition = event.getInventory().getItem(2);

    if (template == null || base == null || addition == null) return;

    // Stand down for vanilla or ToolTrim recipes; do NOT nullify event result
    if (isVanillaOrToolTrimRecipe(template, base, addition)) {
        if (event.getView().getPlayer() instanceof Player p) {
            upgradeAttempts.remove(p.getUniqueId());
        }
        return;
    }

    // Validate template
    if (!template.hasItemMeta() || !template.getItemMeta().hasCustomModelData()) return;
    int cmd = template.getItemMeta().getCustomModelData();
    if (!isApprovedTemplate(cmd)) return;

    // Validate base item and level
    if (!ALLOWED_BASE_ITEMS.contains(base.getType())) return;
    int currentLevel = ExceedPlusUtil.getItemLevel(plugin, base); // Fetch the current level
    if (!isValidTemplateForLevel(cmd, currentLevel)) return;

    // Ensure valid materials for upgrade
    if (!isValidForUpgrade(template, base, addition, currentLevel)) return;

    // Determine new level and prepare item
    int newLevel = Math.min(currentLevel + 1, 10);
    ItemStack result = base.clone();
    ExceedPlusUtil.setItemLevel(plugin, result, newLevel); // Set the new level and display name

    ItemMeta meta = result.getItemMeta();
    if (meta == null) return;

    // Clear and update lore using ExceedPlusUtil
    List<String> lore = new ArrayList<>();
    lore.addAll(ExceedPlusUtil.generateStarsLore(plugin, newLevel)); // Generate stars for the level

    // Retrieve increments from config
    String wtype = getWeaponType(base.getType());
    double damageInc = plugin.getConfig().getDouble("upgrades." + wtype + "_attack_damage_modifier", 0.0);
    double speedInc = plugin.getConfig().getDouble("upgrades." + wtype + "_attack_speed_modifier", 0.0);

    // Append damage and speed increments
    if (damageInc != 0.0) {
        String dmgLine = plugin.getConfig().getString("lore.bonus_damage_line", "&a+{value} Bonus Damage")
                .replace("{value}", ExceedPlusUtil.formatIncrementValue(damageInc));
        lore.add(ColorUtil.translate(dmgLine));
    }
    if (speedInc != 0.0) {
        String spdLine = plugin.getConfig().getString("lore.bonus_speed_line", "&a+{value} Bonus Speed")
                .replace("{value}", ExceedPlusUtil.formatIncrementValue(speedInc));
        lore.add(ColorUtil.translate(spdLine));
    }

    // Add tool trim lines if applicable
    if (hasToolTrim(base)) {
        lore.add("");
        lore = addTrimLinesFromPD(base, lore);
    }

    // Add RNG or skill requirement information
    boolean rngEnabled = plugin.getConfig().getBoolean("advanced.enable_rng", false);
    boolean mcmmoEnabled = plugin.getConfig().getBoolean("advanced.mcmmo_enabled", false);
    boolean skillReqEnabled = plugin.getConfig().getBoolean("skillrequirements.enabled", false);

    int successChance = 100;
    if (rngEnabled) {
        successChance = calculateSuccessChance((Player) event.getView().getPlayer(), base.getType(), mcmmoEnabled);
        lore.add(formatSuccessChanceLine(successChance));
    }

    int requiredSkill = 0;
    if (skillReqEnabled && mcmmoEnabled) {
        Player player = (Player) event.getView().getPlayer();
        requiredSkill = getRequiredSkillLevelForUpgrade(base.getType(), newLevel, template);
        int playerSkill = getSkillLevelFromMCMMO(player, wtype);
        if (playerSkill < requiredSkill) {
            lore.add(ChatColor.RED + "Requires " + capitalize(wtype) + " Level " + requiredSkill);
        }
    }

    // Update increments into PersistentDataContainer
    PersistentDataContainer pdc = meta.getPersistentDataContainer();
    pdc.set(INCREMENTS_KEY, PersistentDataType.STRING, damageInc + ":" + speedInc + ":" + successChance + ":" + requiredSkill);

    meta.setLore(lore);
    result.setItemMeta(meta);
    event.setResult(result);

    // Manage upgrade attempts for RNG
    if (rngEnabled) {
        Player player = (Player) event.getView().getPlayer();
        upgradeAttempts.put(player.getUniqueId(), new UpgradeAttempt(base.clone(), result.clone(), newLevel, successChance));
    } else if (event.getView().getPlayer() instanceof Player player) {
        upgradeAttempts.remove(player.getUniqueId());
    }
}


@EventHandler
public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) return;
    if (event.getView().getTopInventory().getType() == InventoryType.SMITHING) {
        if (event.getSlotType() == InventoryType.SlotType.RESULT && event.getCurrentItem() != null && !event.isCancelled()) {
            ItemStack finalItem = event.getCurrentItem().clone();
            ItemMeta meta = finalItem.getItemMeta();
            if (meta == null) {
                event.setCancelled(true);
                return;
            }
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            String incrementsData = pdc.get(INCREMENTS_KEY, PersistentDataType.STRING);
            UpgradeAttempt attempt = upgradeAttempts.get(player.getUniqueId());
            boolean rngEnabled = plugin.getConfig().getBoolean("advanced.enable_rng", false);

            if (incrementsData == null) {
                if (attempt != null) upgradeAttempts.remove(player.getUniqueId());
                event.setCancelled(false);
                return;
            }

            String[] incParts = incrementsData.split(":");
            double damageInc = Double.parseDouble(incParts[0]);
            double speedInc = Double.parseDouble(incParts[1]);
            int successChance = Integer.parseInt(incParts[2]);
            int requiredSkill = Integer.parseInt(incParts[3]);

            boolean success = !rngEnabled || runRNG(successChance);
            boolean skillReqEnabled = plugin.getConfig().getBoolean("skillrequirements.enabled", false);
            boolean mcmmoEnabled = plugin.getConfig().getBoolean("advanced.mcmmo_enabled", false);
            String wtype = getWeaponType(finalItem.getType());

            if (skillReqEnabled && mcmmoEnabled && requiredSkill > 0) {
                int playerSkill = getSkillLevelFromMCMMO(player, wtype);
                if (playerSkill < requiredSkill) {
                    // Skill too low
                    if (attempt != null) {
                        failUpgrade(attempt.originalItem, player);
                        event.setCurrentItem(attempt.originalItem);
                        upgradeAttempts.remove(player.getUniqueId());
                    } else {
                        event.setCancelled(true);
                    }
                    return;
                }
            }

            if (attempt != null && rngEnabled) {
                if (success) {
                    removeSuccessChanceLineItem(finalItem);
                    removeSkillRequirementLineItem(finalItem);
                    applyAttributeIncrements(finalItem, damageInc, speedInc);
                    int newLevel = getItemLevel(finalItem);
                    if (newLevel == 10) {
                        applySpecialTemplateIfPresent(finalItem);
                    }
                    clearIncrementsPD(finalItem);

                    soundFX.playSuccessSound(player, newLevel == 10);
                    player.sendMessage(ColorUtil.translate("&aUpgrade successful!"));
                } else {
                    failUpgrade(attempt.originalItem, player);
                    event.setCurrentItem(attempt.originalItem);
                }
                upgradeAttempts.remove(player.getUniqueId());
            } else {
                // No RNG scenario or no attempt scenario
                removeSuccessChanceLineItem(finalItem);
                removeSkillRequirementLineItem(finalItem);
                int newLevel = getItemLevel(finalItem);
                applyAttributeIncrements(finalItem, damageInc, speedInc);
                if (newLevel == 10) {
                    applySpecialTemplateIfPresent(finalItem);
                }
                clearIncrementsPD(finalItem);

                soundFX.playSuccessSound(player, newLevel == 10);
                player.sendMessage(ColorUtil.translate("&aUpgrade successful!"));
                if (attempt != null) upgradeAttempts.remove(player.getUniqueId());
                event.setCurrentItem(finalItem);
            }
            
            // Explicitly replace the item in the result slot
            event.setCurrentItem(finalItem);

            // Force update the player's inventory to reflect changes visually
            player.updateInventory();

            event.setCancelled(false);
        }
    }
}

    private void failUpgrade(ItemStack original, Player player) {
        soundFX.playFailureSound(player);
        player.sendMessage(ColorUtil.translate("&cUpgrade failed!"));
    }

    private boolean runRNG(int successChance) {
        return new Random().nextInt(100)+1 <= successChance;
    }

    private void clearIncrementsPD(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta!=null) {
            meta.getPersistentDataContainer().remove(INCREMENTS_KEY);
            item.setItemMeta(meta);
        }
    }

    private boolean isVanillaOrToolTrimRecipe(ItemStack template, ItemStack equipment, ItemStack material) {
        if (template.getType()==Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE) return true;
        if (template.getType().name().endsWith("_SMITHING_TEMPLATE")) return true;

        String eqName = equipment.getType().name();
        if (eqName.endsWith("_HELMET")||eqName.endsWith("_CHESTPLATE")||eqName.endsWith("_LEGGINGS")||eqName.endsWith("_BOOTS"))
            return true;

        if (toolTrimsLoaded && template.getType()==Material.STRUCTURE_BLOCK) {
            ItemMeta meta = template.getItemMeta();
            if (meta!=null && meta.hasLore()) {
                for (String line:meta.getLore()) {
                    if (line.contains("tool_trim_pattern.tooltrims.")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isApprovedTemplate(int customModelData) {
        return TEMPLATE_LEVEL_MAP.containsKey(customModelData) || SPECIAL_TEMPLATES.contains(customModelData);
    }

    private boolean isValidTemplateForLevel(int templateModelData, int currentLevel) {
        if (TEMPLATE_LEVEL_MAP.containsKey(templateModelData)) {
            return TEMPLATE_LEVEL_MAP.get(templateModelData).contains(currentLevel);
        }
        return SPECIAL_TEMPLATES.contains(templateModelData) && currentLevel==10;
    }

    private boolean isValidForUpgrade(ItemStack template, ItemStack base, ItemStack addition, int currentLevel) {
        if (currentLevel<10 && addition.getType()!=Material.NETHERITE_INGOT) return false;
        return true;
    }

    private int calculateSuccessChance(Player player, Material mat, boolean mcmmoEnabled) {
        int base=plugin.getConfig().getInt("advanced.base_success_rate",10);
        boolean rngEnabled=plugin.getConfig().getBoolean("advanced.enable_rng",false);
        if (!rngEnabled)return 100;
        int relatedSkillLevel=getSkillLevelFromMCMMO(player,getWeaponType(mat));
        int repairLevel=getSkillLevelFromMCMMO(player,"repair");
        int calc;
        if (mcmmoEnabled) {
            calc=base+((relatedSkillLevel+repairLevel)/2);
        } else {
            int modifier=plugin.getConfig().getInt("advanced.rng_modifier",10);
            calc=base*modifier;
        }
        return Math.min(calc,100);
    }

    private int getSkillLevelFromMCMMO(Player player, String skill) {
        String placeholder = "%mcmmo_level_"+skill+"%";
        String parsed=PlaceholderAPI.setPlaceholders(player,placeholder);
        try {
            return Math.max(0,Integer.parseInt(parsed));
        } catch(Exception e) {
            return 0;
        }
    }

    private int getRequiredSkillLevelForUpgrade(Material mat, int newLevel, ItemStack template) {
        String wtype=getWeaponType(mat);
        String path="skillrequirements."+wtype+".level_"+newLevel;
        int cmd=template.hasItemMeta() && template.getItemMeta().hasCustomModelData()?template.getItemMeta().getCustomModelData():0;
        if (newLevel==10 && SPECIAL_TEMPLATES.contains(cmd)) {
            String name=stripColor(template.getItemMeta().getDisplayName()).toLowerCase();
            if (name.contains("a")) path="skillrequirements."+wtype+".template_a";
            else path="skillrequirements."+wtype+".template_b";
        }
        return plugin.getConfig().getInt(path,0);
    }

    private void removeSuccessChanceLines(List<String> lore) {
        lore.removeIf(line -> stripColor(line).contains("Success Chance"));
        lore.removeIf(line -> ChatColor.stripColor(line).toLowerCase().contains("success chance"));
        lore.removeIf(String::isEmpty);
    }

    private void removeSuccessChanceLineItem(ItemStack item) {
        ItemMeta meta=item.getItemMeta();
        if (meta==null||!meta.hasLore())return;
        List<String> lore=new ArrayList<>(meta.getLore());
        plugin.getLogger().info("Lore before removing Success Chance: " + lore); // LOGGER
        removeSuccessChanceLines(lore);
        meta.setLore(lore);
        item.setItemMeta(meta);
        plugin.getLogger().info("Lore after removing Success Chance: " + meta.getLore()); // LOGGER
    }

    private void removeSkillRequirementLineItem(ItemStack item) {
        ItemMeta meta=item.getItemMeta();
        if (meta==null||!meta.hasLore())return;
        List<String> lore=new ArrayList<>(meta.getLore());
        removeSkillRequirementLine(lore);
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    private void removeSkillRequirementLine(List<String> lore) {
        lore.removeIf(line->{
            String s=stripColor(line).toLowerCase();
            return s.contains("requires")&&s.contains("level");
        });
    }

    private void removeStarLines(List<String> lore) {
        if (!lore.isEmpty()) {
            String first=stripColor(lore.get(0)).toLowerCase();
            if (first.contains("★")||first.contains("☆")) lore.remove(0);
        }
    }

    private void removeSpecialTemplateLines(List<String> lore) {
        lore.removeIf(line->stripColor(line).toLowerCase().contains("unlock potential"));
    }

    private void removeToolTrimLines(List<String> lore) {
        lore.removeIf(line->{
            String s=stripColor(line).toLowerCase();
            return s.contains("item.tooltrims.smithing_template.upgrade")
                    || s.contains("tool_trim_pattern.tooltrims.")
                    || s.contains("trim_material.tooltrims.")
                    || s.contains("tool trim")
                    || s.contains(" material");
        });
        lore.removeIf(String::isEmpty);
    }

    private boolean hasToolTrim(ItemStack item) {
        if (!toolTrimsLoaded) return false;
        ItemMeta meta=item.getItemMeta();
        if (meta==null)return false;
        return meta.getPersistentDataContainer().has(toolTrimKey, PersistentDataType.STRING);
    }

    private List<String> addTrimLinesFromPD(ItemStack base, List<String> lore) {
        if (!toolTrimsLoaded) return lore;
        ItemMeta meta = base.getItemMeta();
        if (meta == null) return lore;
        if (!meta.getPersistentDataContainer().has(toolTrimKey, PersistentDataType.STRING)) return lore;

        String trimKey = meta.getPersistentDataContainer().get(toolTrimKey, PersistentDataType.STRING);
        if (trimKey == null) return lore;

        try {
            Object trimsMap = toolTrim_Trims.get(null);
            if (!(trimsMap instanceof Map<?,?> map)) return lore;
            Object currentTrim = map.get(trimKey);
            if (currentTrim == null) return lore;

            trimMaterialField.setAccessible(true);
            trimTemplateField.setAccessible(true);
            Object trimMaterialObj = trimMaterialField.get(currentTrim);
            Object trimTemplateObj = trimTemplateField.get(currentTrim);

            Method materialNameMethod = findNameMethod(trimMaterialObj.getClass());
            Method templateNameMethod = findNameMethod(trimTemplateObj.getClass());

            if (materialNameMethod == null || templateNameMethod == null) return lore;

            String patternName = ((String) templateNameMethod.invoke(trimTemplateObj)).toLowerCase();
            String materialName = ((String) materialNameMethod.invoke(trimMaterialObj)).toLowerCase();

            String header = plugin.getConfig().getString("tooltrims.lore.header", "&7Trim");
            header = ChatColor.translateAlternateColorCodes('&', header);

            String patternLine = plugin.getConfig().getString("tooltrims.lore.patterns." + patternName, "&7Unknown Pattern");
            patternLine = ChatColor.translateAlternateColorCodes('&', patternLine);

            String matLine = plugin.getConfig().getString("tooltrims.lore.materials." + materialName, "&7Unknown Material");
            matLine = ChatColor.translateAlternateColorCodes('&', matLine);

            removeToolTrimLines(lore);
            lore.add(header);
            lore.add(patternLine);
            lore.add(matLine);

        } catch (Exception e) {
            // If reflection fails, no trim lines
        }
        return lore;
    }

    private Method findNameMethod(Class<?> clazz) {
        for (Method m : clazz.getMethods()) {
            if (m.getName().equals("name") && m.getParameterCount() == 0 && m.getReturnType() == String.class) {
                return m;
            }
        }
        return null;
    }

private void applyAttributeIncrements(ItemStack item, double damageInc, double speedInc) {
    ItemMeta meta = item.getItemMeta();
    if (meta == null) return;

    PersistentDataContainer pdc = meta.getPersistentDataContainer();
    String currentIncrements = pdc.getOrDefault(INCREMENTS_KEY, PersistentDataType.STRING, "0:0");
    String[] parts = currentIncrements.split(":");

    double currentDamage = Double.parseDouble(parts[0]);
    double currentSpeed = Double.parseDouble(parts[1]);

    // Calculate new cumulative values
    double newDamage = currentDamage + damageInc;
    double newSpeed = currentSpeed + speedInc;

    // Remove existing "ExceedPlus" attribute modifiers to avoid duplication
    removeExistingExceedPlusModifiers(meta, Attribute.GENERIC_ATTACK_DAMAGE);
    removeExistingExceedPlusModifiers(meta, Attribute.GENERIC_ATTACK_SPEED);

    // Add new custom attribute modifiers
    meta.addAttributeModifier(
        Attribute.GENERIC_ATTACK_DAMAGE,
        new AttributeModifier(UUID.randomUUID(), "ExceedPlusDamage", newDamage, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND)
    );
    meta.addAttributeModifier(
        Attribute.GENERIC_ATTACK_SPEED,
        new AttributeModifier(UUID.randomUUID(), "ExceedPlusSpeed", newSpeed, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND)
    );

    // Update PersistentDataContainer with new increments
    pdc.set(INCREMENTS_KEY, PersistentDataType.STRING, newDamage + ":" + newSpeed);

    // Update lore to reflect the cumulative values
    List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
    
    // Remove old increment lines if they exist
    removeBonusLoreLines(lore);
    
    // Add new increment lines
    updateLoreWithIncrements(lore, newDamage, newSpeed);

    meta.setLore(lore);

    // Update the ItemMeta on the ItemStack
    item.setItemMeta(meta);
}

private void removeBonusLoreLines(List<String> lore) {
    lore.removeIf(line -> ChatColor.stripColor(line).contains("Bonus Damage") ||
                          ChatColor.stripColor(line).contains("Bonus Speed"));
}
/**
 * Remove custom ExceedPlus modifiers to avoid duplication.
 */
private void removeExistingExceedPlusModifiers(ItemMeta meta, Attribute attribute) {
    if (meta.getAttributeModifiers(attribute) != null) {
        meta.getAttributeModifiers(attribute).stream()
            .filter(modifier -> modifier.getName().startsWith("ExceedPlus"))
            .forEach(modifier -> meta.removeAttributeModifier(attribute, modifier));
    }
}
/**
 * Update lore to display cumulative Bonus Damage and Bonus Speed.
 */
private void updateLoreWithIncrements(List<String> lore, double newBonusDamage, double newBonusSpeed) {
    String damageLine = ColorUtil.translate("&a+" + ExceedPlusUtil.formatIncrementValue(newBonusDamage) + " Bonus Damage");
    String speedLine = ColorUtil.translate("&a+" + ExceedPlusUtil.formatIncrementValue(newBonusSpeed) + " Bonus Speed");

    // Add increment lines to the lore
    lore.add(damageLine);
    lore.add(speedLine);
}



    private String formatIncrementValue(double value) {
    return value == (int) value ? String.valueOf((int) value) : String.format("%.2f", value);
}
    
    
// Helper method to retrieve existing attribute value
private double getCurrentAttributeValue(ItemMeta meta, Attribute attribute) {
    if (!meta.hasAttributeModifiers()) return 0.0;
    return meta.getAttributeModifiers(attribute).stream()
            .filter(mod -> mod.getOperation() == AttributeModifier.Operation.ADD_NUMBER)
            .mapToDouble(AttributeModifier::getAmount)
            .sum();
}


    private void applySpecialTemplateIfPresent(ItemStack item) {
        ItemMeta meta=item.getItemMeta();
        if (meta==null)return;
        List<String> lore=meta.hasLore()?new ArrayList<>(meta.getLore()):new ArrayList<>();
        removeSpecialTemplateLines(lore);

        int cmd=meta.hasCustomModelData()?meta.getCustomModelData():0;
        String templateKey=getTemplateKeyFromCMD(cmd);
        if (templateKey==null)return;

        double damageInc=plugin.getConfig().getDouble("specialtemplates."+templateKey+".damage_increase",0.0);
        double speedInc=plugin.getConfig().getDouble("specialtemplates."+templateKey+".speed_increase",0.0);
        double durabilityInc=plugin.getConfig().getDouble("specialtemplates."+templateKey+".durability_increase",0.0);

        String formattedDamage = ExceedPlusUtil.formatIncrementValue(damageInc);
        String formattedSpeed = ExceedPlusUtil.formatIncrementValue(speedInc);

        String name=plugin.getConfig().getString("specialtemplates."+templateKey+".name","&fSpecial Template");
        name=ColorUtil.translate(name);
        List<String> rawLore=plugin.getConfig().getStringList("specialtemplates."+templateKey+".lore");
        List<String> specialLore=new ArrayList<>();
        for(String line:rawLore) {
            line=line.replace("{damage}", formattedDamage);
            line=line.replace("{speed}", formattedSpeed);
            line=line.replace("{durability}", String.valueOf((int)durabilityInc));
            specialLore.add(ColorUtil.translate(line));
        }
        lore.addAll(specialLore);

            // Remove existing "ExceedPlus" attribute modifiers to avoid duplication
    removeExistingExceedPlusModifiers(meta, Attribute.GENERIC_ATTACK_DAMAGE);
    removeExistingExceedPlusModifiers(meta, Attribute.GENERIC_ATTACK_SPEED);
        if (damageInc!=0.0) {
            meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE,
               new AttributeModifier(UUID.randomUUID(),"ExceedPlusSTDamage",damageInc,AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));
        }
        if (speedInc!=0.0) {
            meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED,
               new AttributeModifier(UUID.randomUUID(),"ExceedPlusSTSpeed",speedInc,AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));
        }

        if (isBowType(item.getType()) && durabilityInc>0.0) {
            int unbreakingLevel=meta.hasEnchant(Enchantment.UNBREAKING)?meta.getEnchantLevel(Enchantment.UNBREAKING):0;
            double effectiveDurability = 384*(unbreakingLevel+1)*(1+durabilityInc/100.0);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin,"max_durability"), PersistentDataType.INTEGER,(int)effectiveDurability);
            lore.add(ChatColor.GREEN+"+"+(int)durabilityInc+"% Durability");
        }

        meta.setLore(lore);
        meta.getPersistentDataContainer().set(SPECIAL_TEMPLATE_KEY,PersistentDataType.STRING,templateKey);
        item.setItemMeta(meta);
    }

    private String getTemplateKeyFromCMD(int cmd) {
        // Mapping if needed. For now null.
        return null;
    }

    private String formatSuccessChanceLine(int successChance) {
        String lineName = plugin.getConfig().getString("text.success_chance_lore", "Success Chance");
        String sep = plugin.getConfig().getString("text.separator", "•");
        String baseColor = plugin.getConfig().getString("text.success_chance_color", "&6");

        String color;
        if (successChance == 100) {
            color = plugin.getConfig().getString("maxColor", "&6");
        } else if (successChance >= 86) {
            color = plugin.getConfig().getString("veryhighColor", "&2");
        } else if (successChance >= 67) {
            color = plugin.getConfig().getString("highColor", "&e");
        } else if (successChance >= 34) {
            color = plugin.getConfig().getString("midColor", "&#EB8E27");
        } else {
            color = plugin.getConfig().getString("lowColor", "&c");
        }

        String raw = baseColor + lineName + " " + sep + " " + color + successChance + "%";
        return ColorUtil.translate(raw);
    }

    private boolean isWeapon(Material m) {
        String w=getWeaponType(m);
        return w.equals("swords")||w.equals("axes")||w.equals("pickaxe")||w.equals("shovel")||w.equals("hoe")||w.equals("mace")||w.equals("trident");
    }

    private boolean isBowType(Material m) {
        return m==Material.BOW||m==Material.CROSSBOW;
    }

    private String capitalize(String s) {
        if (s.isEmpty()) return s;
        return s.substring(0,1).toUpperCase()+s.substring(1);
    }

    private String getWeaponType(Material mat) {
        String name=mat.name().toLowerCase();
        if (name.contains("sword"))return "swords";
        if (name.contains("axe"))return "axes";
        if (name.contains("pickaxe"))return "pickaxe";
        if (name.contains("shovel"))return "shovel";
        if (name.contains("hoe"))return "hoe";
        if (name.contains("bow"))return "bow";
        if (name.contains("crossbow"))return "crossbow";
        if (name.contains("trident"))return "trident";
        if (name.contains("mace"))return "mace";
        return "repair";
    }

    private int getItemLevel(ItemStack item) {
        if (item==null||!item.hasItemMeta())return 0;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(LEVEL_KEY,PersistentDataType.INTEGER,0);
    }

    private String stripColor(String input) {
        return input==null?"":ChatColor.stripColor(input);
    }

    private static class UpgradeAttempt {
        final ItemStack originalItem;
        final ItemStack resultItem;
        final int newLevel;
        final int successChance;
        UpgradeAttempt(ItemStack originalItem, ItemStack resultItem, int newLevel, int successChance) {
            this.originalItem=originalItem;
            this.resultItem=resultItem;
            this.newLevel=newLevel;
            this.successChance=successChance;
        }
    }
}
