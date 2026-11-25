package cz.mmartis.tidyshop.config;

import org.bukkit.Material;

import java.math.BigDecimal;

/** One item in a category. A {@code null} price means that action is disabled. */
public record ShopItem(Material material, BigDecimal buyPrice, BigDecimal sellPrice) {
}
