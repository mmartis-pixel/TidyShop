package cz.mmartis.tidyshop.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.plugin.Plugin;

/**
 * While one of our menus is open, every click and drag in the whole view is cancelled,
 * including the player's own inventory (shift-click, number keys, double-click, offhand swap).
 */
public final class MenuListener implements Listener {

    private final Plugin plugin;

    public MenuListener(Plugin plugin) {
        this.plugin = plugin;
    }

    // LOWEST runs first, so other plugins already see the event as cancelled.
    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        // getInventory() is the top inventory of the view; getHolder(false) skips the block-state snapshot
        if (!(event.getInventory().getHolder(false) instanceof Menu menu)) {
            return;
        }
        event.setCancelled(true);

        int slot = event.getRawSlot(); // raw slots below the top size belong to the menu
        if (slot < 0 || slot >= event.getInventory().getSize() || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Menu next = menu.onClick(player, slot, event.getClick());
        if (next != null) {
            // Bukkit forbids changing the view inside InventoryClickEvent (see its javadoc): switch on the next tick,
            // and only if the clicked menu is still open (not closed by Esc, death or /shop reload meanwhile).
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (Menu.openMenu(player) == menu) {
                    next.open(player);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder(false) instanceof Menu) {
            event.setCancelled(true);
        }
    }
}
