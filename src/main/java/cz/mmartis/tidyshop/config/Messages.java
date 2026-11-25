package cz.mmartis.tidyshop.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Map;

/** Texts from messages.yml in MiniMessage format. Every message may use {@code <prefix>}. */
public final class Messages {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final ConfigurationSection yaml;

    /** Missing keys fall back to {@code yaml}'s defaults (the bundled messages.yml). */
    public Messages(ConfigurationSection yaml) {
        this.yaml = yaml;
    }

    public Component get(String key) {
        return get(key, Map.of());
    }

    /**
     * @param placeholders e.g. {@code item, amount, price}. Components (like a translated item name) are inserted
     *                     as they are; anything else as plain text, so a value can never inject formatting or click events.
     */
    public Component get(String key, Map<String, ?> placeholders) {
        TagResolver.Builder tags = TagResolver.builder()
                .resolver(Placeholder.parsed("prefix", text("prefix", "")));
        placeholders.forEach((name, value) -> tags.resolver(value instanceof ComponentLike component
                ? Placeholder.component(name, component)
                : Placeholder.unparsed(name, String.valueOf(value))));
        return MINI_MESSAGE.deserialize(text(key, key), tags.build());
    }

    private String text(String key, String fallback) {
        String raw = yaml.getString(key); // getString(key, def) would bypass setDefaults()
        return raw != null ? raw : fallback;
    }
}
