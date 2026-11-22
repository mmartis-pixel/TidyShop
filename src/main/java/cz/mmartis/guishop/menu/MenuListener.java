package cz.mmartis.guishop.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * While one of our menus is open, every click and drag in the whole view is cancelled,
 * including the player's own inventory (shift-click, number keys, double-click, offhand swap).
 */
public final class MenuListener implements Listener {

    // LOWEST runs first, so other plugins already see the event as cancelled.
    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        // getInventory() is the top inventory of the view; getHolder(false) skips the block-state snapshot
        if (!(event.getInventory().getHolder(false) instanceof Menu menu)) {
            return;
        }
        event.setCancelled(true);

        int slot = event.getRawSlot(); // raw slots below the top size belong to the menu
        if (slot >= 0 && slot < event.getInventory().getSize() && event.getWhoClicked() instanceof Player player) {
            menu.onClick(player, slot, event.getClick());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder(false) instanceof Menu) {
            event.setCancelled(true);
        }
    }
}
