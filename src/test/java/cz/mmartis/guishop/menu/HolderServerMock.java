package cz.mmartis.guishop.menu;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.InventoryHolder;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.inventory.ChestInventoryMock;
import org.mockbukkit.mockbukkit.inventory.InventoryMock;

/**
 * MockBukkit 4.120.0 throws UnimplementedOperationException from {@code Inventory#getHolder(boolean)},
 * which Paper recommends for menu detection. Chest inventories created by this server implement it,
 * so the real listener code runs unchanged in tests.
 */
class HolderServerMock extends ServerMock {

    @Override
    public InventoryMock createInventory(InventoryHolder owner, int size) {
        return chest(owner, size, null);
    }

    @Override
    public InventoryMock createInventory(InventoryHolder owner, int size, Component title) {
        return chest(owner, size, title);
    }

    private static InventoryMock chest(InventoryHolder owner, int size, Component title) {
        ChestInventoryMock inventory = new ChestInventoryMock(owner, size) {
            @Override
            public InventoryHolder getHolder(boolean useSnapshot) {
                return getHolder();
            }
        };
        inventory.setCustomTitle(title);
        return inventory;
    }
}
