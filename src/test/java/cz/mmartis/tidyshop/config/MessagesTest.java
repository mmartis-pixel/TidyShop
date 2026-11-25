package cz.mmartis.tidyshop.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessagesTest {

    private static Messages messages(String yaml) throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return new Messages(config);
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Test
    void fillsPrefixAndPlaceholders() throws Exception {
        Messages messages = messages("""
                prefix: "<gold>[Shop]</gold> "
                bought: "<prefix>Bought <amount>x <item> for <price>. Balance: <balance>"
                """);

        Component result = messages.get("bought",
                Map.of("item", "Diamond", "amount", "2", "price", "10.50", "balance", "89.50"));

        assertEquals("[Shop] Bought 2x Diamond for 10.50. Balance: 89.50", plain(result));
    }

    @Test
    void placeholderValuesAreNotParsedAsMiniMessage() throws Exception {
        Messages messages = messages("hi: \"Hi <item>\"\n");

        assertEquals("Hi <red>X", plain(messages.get("hi", Map.of("item", "<red>X"))));
    }

    @Test
    void componentPlaceholderKeepsItsContent() throws Exception {
        Messages messages = messages("hi: \"Hi <item>\"\n");

        Component item = Component.text("Diamond");

        assertEquals("Hi Diamond", plain(messages.get("hi", Map.of("item", item))));
    }

    @Test
    void missingKeyFallsBackToDefaults() throws Exception {
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.loadFromString("hi: \"default hi\"\n");
        YamlConfiguration user = new YamlConfiguration();
        user.setDefaults(defaults);

        assertEquals("default hi", plain(new Messages(user).get("hi")));
    }

    @Test
    void unknownKeyShowsTheKey() throws Exception {
        assertEquals("nope", plain(messages("a: b\n").get("nope")));
    }
}
