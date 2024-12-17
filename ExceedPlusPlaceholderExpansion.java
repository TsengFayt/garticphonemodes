package net.rhythmcore.exceedplus;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class ExceedPlusPlaceholderExpansion extends PlaceholderExpansion {
    private final ExceedPlus plugin;
    public ExceedPlusPlaceholderExpansion(ExceedPlus plugin) {
        this.plugin = plugin;
    }
    @Override
    public String getIdentifier() { return "exceedplus"; }

    @Override
    public String getAuthor() { return "YourName"; }

    @Override
    public String getVersion() { return "1.0.0"; }

    @Override
    public String onPlaceholderRequest(Player p, String identifier) {
        if (identifier.equalsIgnoreCase("is_max_level")) {
            // Check if item in hand is max level
            // Return "true" or "false"
            return "false";
        }
        if (identifier.equalsIgnoreCase("has_first_upgrade")) {
            // Check PD or memory if player ever upgraded something
            return "false";
        }
        return null;
    }
}
