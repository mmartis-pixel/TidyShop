package cz.mmartis.tidyshop.menu;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;

/**
 * A chest GUI. The inventory's holder is the menu itself, which is how {@link MenuListener}
 * recognises our inventories (Paper's recommended pattern, no player-to-menu maps).
 */
public abstract class Menu implements InventoryHolder {

    private final Inventory inventory;

    protected Menu(int rows, Component title) {
        this.inventory = Bukkit.createInventory(this, rows * 9, title);
    }

    /** The shop menu the player is looking at, or null. */
    public static Menu openMenu(Player player) {
        InventoryView view = player.getOpenInventory();
        // our menus are chests; the type check skips e.g. the player's own crafting view without touching its holder
        return view.getType() == InventoryType.CHEST && view.getTopInventory().getHolder(false) instanceof Menu menu
                ? menu : null;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    /**
     * Called for clicks on a slot of this menu (top inventory). The event is already cancelled,
     * so nothing can be taken or put in whatever this method does.
     *
     * @return the menu to show next, or null to stay. Must not be opened here: the view may not change
     *         inside InventoryClickEvent, so {@link MenuListener} opens it on the next tick.
     */
    public abstract Menu onClick(Player player, int slot, ClickType click);
}
