package cz.mmartis.tidyshop.menu;

import cz.mmartis.tidyshop.config.Category;
import cz.mmartis.tidyshop.config.Messages;
import cz.mmartis.tidyshop.config.ShopItem;
import cz.mmartis.tidyshop.trade.Trade;
import cz.mmartis.tidyshop.trade.TradeService;
import cz.mmartis.tidyshop.trade.Wallet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One category: items in the top 5 rows (slot = position in config.yml, at most 45), Back button in the bottom row.
 * Left = buy 1, shift+left = buy a stack, right = sell 1, shift+right = sell all, anything else does nothing.
 */
public final class CategoryMenu extends Menu {

    static final int BACK_SLOT = 49; // middle of the bottom row

    private final Category category;
    private final Menu back;
    private final Messages messages;
    private final Wallet wallet;
    private final TradeService trade;

    CategoryMenu(Category category, Menu back, Messages messages, Wallet wallet, TradeService trade) {
        super(6, MiniMessage.miniMessage().deserialize(category.name()));
        this.category = category;
        this.back = back;
        this.messages = messages;
        this.wallet = wallet;
        this.trade = trade;

        List<ShopItem> items = category.items();
        for (int slot = 0; slot < items.size(); slot++) {
            getInventory().setItem(slot, icon(items.get(slot)));
        }
        ItemStack backIcon = ItemStack.of(Material.ARROW);
        backIcon.editMeta(meta -> meta.itemName(messages.get("back-button")));
        getInventory().setItem(BACK_SLOT, backIcon);
    }

    /** Item with lore from messages.yml; lines of a disabled action are left out. */
    private ItemStack icon(ShopItem item) {
        ItemStack icon = ItemStack.of(item.material());
        int stack = icon.getMaxStackSize();
        List<Component> lore = new ArrayList<>();
        if (item.buyPrice() != null) {
            lore.add(messages.get("lore-buy", Map.of(
                    "price", wallet.format(item.buyPrice()),
                    "stack", stack,
                    "stack-price", wallet.format(item.buyPrice().multiply(BigDecimal.valueOf(stack))))));
        }
        if (item.sellPrice() != null) {
            lore.add(messages.get("lore-sell", Map.of("price", wallet.format(item.sellPrice()))));
        }
        if (item.buyPrice() != null) {
            lore.add(messages.get("lore-buy-hint", Map.of("stack", stack)));
        }
        if (item.sellPrice() != null) {
            lore.add(messages.get("lore-sell-hint"));
        }
        // lore is italic by default in the client
        icon.editMeta(meta -> meta.lore(lore.stream()
                .map(line -> line.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE))
                .toList()));
        return icon;
    }

    @Override
    public Menu onClick(Player player, int slot, ClickType click) {
        if (slot == BACK_SLOT) {
            return click == ClickType.LEFT || click == ClickType.RIGHT ? back : null;
        }
        // checked on every trade, not only on /shop: the permission may be taken away while the menu is open
        if (slot >= category.items().size() || !player.hasPermission("tidyshop.use")) {
            return null;
        }
        ShopItem item = category.items().get(slot);
        boolean buying = click == ClickType.LEFT || click == ClickType.SHIFT_LEFT;
        Trade result = switch (click) {
            case LEFT -> item.buyPrice() == null ? null : trade.buy(player, item, 1);
            case SHIFT_LEFT -> item.buyPrice() == null ? null
                    : trade.buy(player, item, ItemStack.of(item.material()).getMaxStackSize());
            case RIGHT -> item.sellPrice() == null ? null : trade.sell(player, item, 1);
            case SHIFT_RIGHT -> item.sellPrice() == null ? null : trade.sell(player, item, Integer.MAX_VALUE);
            default -> null;
        };
        if (result != null) {
            player.updateInventory(); // contents changed inside InventoryClickEvent, its javadoc asks for this
            player.sendMessage(messages.get(messageKey(result, buying), Map.of(
                    "item", Component.translatable(item.material()),
                    "amount", result.amount(),
                    "price", wallet.format(result.money()))));
        }
        return null;
    }

    private static String messageKey(Trade trade, boolean buying) {
        return switch (trade.result()) {
            case OK -> buying ? "bought" : "sold";
            case NO_MONEY -> "no-money";
            case NO_SPACE -> "no-space";
            case NOTHING_TO_SELL -> "nothing-to-sell";
            case NO_ECONOMY -> "no-economy";
            case ECONOMY_FAILED -> "economy-failed";
        };
    }
}
