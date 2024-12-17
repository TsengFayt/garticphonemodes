package net.rhythmcore.exceedplus;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ColorUtil {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat() // To ensure hex colors are serialized properly
            .build();

    public static String translate(String input) {
        if (input == null) return "";

        // Step 1: Convert legacy & codes into MiniMessage tags
        String replaced = input
                .replace("&0", "<black>")
                .replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>")
                .replace("&5", "<dark_purple>")
                .replace("&6", "<gold>")
                .replace("&7", "<gray>")
                .replace("&8", "<dark_gray>")
                .replace("&9", "<blue>")
                .replace("&a", "<green>")
                .replace("&b", "<aqua>")
                .replace("&c", "<red>")
                .replace("&d", "<light_purple>")
                .replace("&e", "<yellow>")
                .replace("&f", "<white>")
                .replace("&l", "<bold>")
                .replace("&n", "<underlined>")
                .replace("&m", "<strikethrough>")
                .replace("&o", "<italic>")
                .replace("&r", "<reset>");

        // Step 2: Convert &#RRGGBB to <#RRGGBB>
        replaced = replaced.replaceAll("(?i)&(#([0-9A-F]{6}))", "<#$2>");

        // Step 3: Parse using MiniMessage
        Component component = MINI_MESSAGE.deserialize(replaced);

        // Step 4: Convert back to a legacy string with full color support
        return LEGACY_SERIALIZER.serialize(component);
    }
}
