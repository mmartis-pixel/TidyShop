package cz.mmartis.guishop.config;

import org.bukkit.Material;

import java.util.List;

/** A shop category. {@code name} is raw MiniMessage, {@code slot} is its position in the main menu. */
public record Category(String id, String name, Material icon, int slot, List<ShopItem> items) {

    public Category {
        items = List.copyOf(items);
    }
}
