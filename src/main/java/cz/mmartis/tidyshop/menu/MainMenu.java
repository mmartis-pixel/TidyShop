package cz.mmartis.tidyshop.menu;

import cz.mmartis.tidyshop.config.Category;
import cz.mmartis.tidyshop.config.Messages;
import cz.mmartis.tidyshop.config.ShopConfig;
import cz.mmartis.tidyshop.trade.TradeService;
import cz.mmartis.tidyshop.trade.Wallet;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/** /shop: one icon per category on the slot from config.yml. */
public final class MainMenu extends Menu {

    private final Messages messages;
    private final Wallet wallet;
    private final TradeService trade;
    private final Map<Integer, Category> categoriesBySlot = new HashMap<>();

    public MainMenu(ShopConfig config, Messages messages, Wallet wallet, TradeService trade) {
        super(rowsFor(config), messages.get("main-menu-title"));
        this.messages = messages;
        this.wallet = wallet;
        this.trade = trade;
        for (Category category : config.categories()) {
            ItemStack icon = ItemStack.of(category.icon());
            // itemName (not customName): not italic and not shown as "renamed"
            icon.editMeta(meta -> meta.itemName(MiniMessage.miniMessage().deserialize(category.name())));
            getInventory().setItem(category.slot(), icon);
            categoriesBySlot.put(category.slot(), category);
        }
    }

    /** Just enough rows to fit the highest category slot. */
    private static int rowsFor(ShopConfig config) {
        int maxSlot = config.categories().stream().mapToInt(Category::slot).max().orElse(0);
        return maxSlot / 9 + 1;
    }

    @Override
    public Menu onClick(Player player, int slot, ClickType click) {
        Category category = categoriesBySlot.get(slot);
        if (category == null || (click != ClickType.LEFT && click != ClickType.RIGHT)) {
            return null;
        }
        return new CategoryMenu(category, this, messages, wallet, trade);
    }
}
