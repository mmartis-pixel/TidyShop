package cz.mmartis.guishop.menu;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * A chest GUI. The inventory's holder is the menu itself, which is how {@link MenuListener}
 * recognises our inventories (Paper's recommended pattern, no player-to-menu maps).
 */
public abstract class Menu implements InventoryHolder {

    private final Inventory inventory;

    protected Menu(int rows, Component title) {
        this.inventory = Bukkit.createInventory(this, rows * 9, title);
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
     */
    public abstract void onClick(Player player, int slot, ClickType click);
}
