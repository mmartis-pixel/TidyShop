package cz.mmartis.tidyshop.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** Immutable snapshot of config.yml. Reload builds a new one and swaps it. */
public record ShopConfig(List<Category> categories) {

    public static final ShopConfig EMPTY = new ShopConfig(List.of());
    /** Category page has 5 rows of items, the 6th row is for buttons. */
    public static final int MAX_ITEMS = 45;
    /** Largest possible chest inventory has 54 slots. */
    private static final int MAX_SLOT = 53;

    public ShopConfig {
        categories = List.copyOf(categories);
    }

    /**
     * Parses config.yml. Invalid categories and items are skipped and reported through {@code warn},
     * so one typo never takes the whole shop down.
     */
    public static ShopConfig parse(ConfigurationSection root, Consumer<String> warn) {
        ConfigurationSection categories = root.getConfigurationSection("categories");
        if (categories == null) {
            warn.accept("config.yml has no 'categories' section, the shop is empty");
            return EMPTY;
        }

        List<Category> result = new ArrayList<>();
        Set<Integer> usedSlots = new HashSet<>();
        for (String id : categories.getKeys(false)) {
            String where = "Category '" + id + "': ";
            ConfigurationSection section = categories.getConfigurationSection(id);
            if (section == null) {
                warn.accept(where + "is not a section, skipped");
                continue;
            }
            String iconName = section.getString("icon", "");
            Material icon = itemMaterial(iconName);
            if (icon == null) {
                warn.accept(where + "icon '" + iconName + "' is not a vanilla item, skipped");
                continue;
            }
            int slot = section.getInt("slot", -1);
            if (slot < 0 || slot > MAX_SLOT) {
                warn.accept(where + "slot must be 0-" + MAX_SLOT + ", skipped");
                continue;
            }
            if (!usedSlots.add(slot)) {
                warn.accept(where + "slot " + slot + " is already used by another category, skipped");
                continue;
            }
            List<ShopItem> items = parseItems(id, section.getConfigurationSection("items"), warn);
            result.add(new Category(id, section.getString("name", id), icon, slot, items));
        }
        return new ShopConfig(result);
    }

    private static List<ShopItem> parseItems(String categoryId, ConfigurationSection items, Consumer<String> warn) {
        List<ShopItem> result = new ArrayList<>();
        if (items == null) {
            return result;
        }
        for (String key : items.getKeys(false)) {
            String where = "Category '" + categoryId + "', item '" + key + "': ";
            if (Material.matchMaterial(key) == null) {
                warn.accept(where + "unknown material, skipped");
                continue;
            }
            Material material = itemMaterial(key);
            if (material == null) {
                warn.accept(where + "material is not an item, skipped");
                continue;
            }
            BigDecimal buy;
            BigDecimal sell;
            try {
                buy = price(items.get(key + ".buy"));
                sell = price(items.get(key + ".sell"));
            } catch (IllegalArgumentException e) {
                warn.accept(where + e.getMessage() + ", skipped");
                continue;
            }
            if (buy == null && sell == null) {
                warn.accept(where + "has no buy or sell price, skipped");
                continue;
            }
            if (result.size() == MAX_ITEMS) {
                warn.accept("Category '" + categoryId + "': more than " + MAX_ITEMS
                        + " items, the rest is not shown (no paging)");
                break;
            }
            result.add(new ShopItem(material, buy, sell));
        }
        return result;
    }

    /** @return the material if it exists and can be held as an item, otherwise null */
    private static Material itemMaterial(String name) {
        Material material = Material.matchMaterial(name);
        return material != null && material.isItem() && !material.isAir() ? material : null;
    }

    /** @return null when the price is not set; throws IllegalArgumentException when it is invalid */
    private static BigDecimal price(Object raw) {
        if (raw == null) {
            return null;
        }
        BigDecimal price;
        try {
            // via String: 0.1 stays exactly 0.1 instead of the double's binary approximation
            price = new BigDecimal(raw.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("price '" + raw + "' is not a number");
        }
        if (price.signum() < 0) {
            throw new IllegalArgumentException("negative price " + raw);
        }
        return price;
    }
}
