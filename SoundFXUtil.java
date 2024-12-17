package net.rhythmcore.exceedplus;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.PacketType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * SoundFXUtil merges the original SmithingSoundFX functionality (intercepting vanilla smithing table sounds)
 * with the new config-based sound logic for success/failure and max-level upgrade.
 */
public class SoundFXUtil {

    private final JavaPlugin plugin;

    public SoundFXUtil(JavaPlugin plugin) {
        this.plugin = plugin;
        registerSoundInterceptor();
    }

    /**
     * Play success sound using config-based keys.
     * If isMaxLevel is true, uses max_level_sound, otherwise success_sound.
     */
    public void playSuccessSound(Player player, boolean isMaxLevel) {
        String soundKey = isMaxLevel ?
                plugin.getConfig().getString("sounds.max_level_sound", "minecraft:ui.toast.challenge_complete") :
                plugin.getConfig().getString("sounds.success_sound", "minecraft:entity.player.levelup");
        player.playSound(player.getLocation(), soundKey, 1f, 1f);
    }

    /**
     * Play failure sound using config-based key.
     */
    public void playFailureSound(Player player) {
        String soundKey = plugin.getConfig().getString("sounds.failure_sound", "minecraft:entity.item.break");
        player.playSound(player.getLocation(), soundKey, 1f, 1f);
    }

    /**
     * Intercept the vanilla smithing table use sound and cancel it,
     * so we can control all sounds through custom logic.
     */
    private void registerSoundInterceptor() {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Server.NAMED_SOUND_EFFECT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                try {
                    String soundName = event.getPacket().getStrings().readSafely(0);
                    // Cancel vanilla smithing table use sound
                    if ("minecraft:block.smithing_table.use".equals(soundName)) {
                        event.setCancelled(true);
                    }
                } catch (Exception ex) {
                    plugin.getLogger().warning("[ExceedPlus] Error processing sound packet: " + ex.getMessage());
                }
            }
        });
    }
}
