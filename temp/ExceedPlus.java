package net.rhythmcore.exceedplus;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

public final class ExceedPlus extends JavaPlugin {

    @Override
    public void onEnable() {
        // Load or generate config
        saveDefaultConfig();
        reloadConfig();

        // Check datapack presence if enabled
        if (getConfig().getBoolean("advanced.show_datapack_warning", true)) {
            validateDatapack();
        }

        // Register commands
        ExceedPlusCommandHandler commandHandler = new ExceedPlusCommandHandler(this);
        getCommand("exceed").setExecutor(commandHandler);
        getCommand("exceed").setTabCompleter(commandHandler);

        // Register recipes
        new ExceedPlusRecipeRegistry(this).registerRecipes();

        // Register event handler
        getServer().getPluginManager().registerEvents(new ExceedPlusEventHandler(this), this);

        // Placeholder integration
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ExceedPlusPlaceholderExpansion(this).register();
        }

        getLogger().info("ExceedPlus enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("ExceedPlus disabled.");
    }

    // Validate the presence of the datapack
    private void validateDatapack() {
        String worldName = getConfig().getString("advanced.world", "world");
        File datapackFile = new File(Bukkit.getWorldContainer(), worldName + "/datapacks/ExceedPlus-datapack.zip");

        if (!datapackFile.exists()) {
            getLogger().log(Level.WARNING,
                "[ExceedPlus] Datapack not found! This is due to the file 'ExceedPlus-datapack.zip' " +
                "missing from the datapacks folder inside of your world folder or because you have a custom world name. " +
                "To resolve this issue, verify that you have added the datapack to your world folder under 'datapacks', " +
                "that the file is named exactly as 'ExceedPlus-datapack.zip', or if your world has a custom name, " +
                "that it is added to your config.yml settings. Otherwise, to proceed without errors, " +
                "you can edit the config.yml file by setting 'show_datapack_warning:' to 'false'."
            );
        }
    }
}
