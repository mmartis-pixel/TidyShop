package cz.mmartis.tidyshop.menu;

import cz.mmartis.tidyshop.TidyShopPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.simulate.entity.PlayerSimulation;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainMenuTest {

    // slots of the two categories in the shipped config.yml
    private static final int BLOCKS_SLOT = 11;
    private static final int ORES_SLOT = 15;

    private ServerMock server;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock(new HolderServerMock());
        MockBukkit.load(TidyShopPlugin.class);
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private InventoryView openShop() {
        player.performCommand("shop");
        return player.getOpenInventory();
    }

    private InventoryClickEvent click(InventoryView view, ClickType type, int rawSlot) {
        return new PlayerSimulation(player).simulateInventoryClick(view, type, rawSlot);
    }

    @Test
    void shopOpensMainMenuWithCategoryIcons() {
        Inventory top = openShop().getTopInventory();

        assertInstanceOf(MainMenu.class, top.getHolder(false));
        assertEquals(Material.GRASS_BLOCK, top.getItem(BLOCKS_SLOT).getType());
        assertEquals(Material.DIAMOND, top.getItem(ORES_SLOT).getType());
        assertEquals("Blocks", plain(top.getItem(BLOCKS_SLOT).getItemMeta().itemName()));
        assertNull(top.getItem(0), "slots without a category stay empty");
    }

    @Test
    void consoleGetsPlayersOnlyMessage() {
        server.dispatchCommand(server.getConsoleSender(), "shop");

        assertTrue(plain(server.getConsoleSender().nextComponentMessage()).contains("Only players"));
    }

    @Test
    void clickOnCategoryIsCancelledAndOpensItsPageNextTick() {
        InventoryView view = openShop();

        InventoryClickEvent event = click(view, ClickType.LEFT, ORES_SLOT);

        assertTrue(event.isCancelled());
        assertEquals(Material.DIAMOND, view.getTopInventory().getItem(ORES_SLOT).getType(), "icon stays in place");
        assertInstanceOf(MainMenu.class, player.getOpenInventory().getTopInventory().getHolder(false),
                "the view must not change inside the click event");

        server.getScheduler().performOneTick();

        assertInstanceOf(CategoryMenu.class, player.getOpenInventory().getTopInventory().getHolder(false));
    }

    @Test
    void everyClickTypeInMenuIsCancelled() {
        InventoryView view = openShop();
        for (ClickType type : ClickType.values()) {
            assertTrue(click(view, type, BLOCKS_SLOT).isCancelled(), type + " on icon");
            assertTrue(click(view, type, 0).isCancelled(), type + " on empty slot");
        }
    }

    @Test
    void onlyLeftOrRightClickOpensCategory() {
        InventoryView view = openShop();

        click(view, ClickType.NUMBER_KEY, BLOCKS_SLOT);
        click(view, ClickType.DOUBLE_CLICK, BLOCKS_SLOT);
        click(view, ClickType.SWAP_OFFHAND, BLOCKS_SLOT);
        server.getScheduler().performOneTick();

        assertInstanceOf(MainMenu.class, player.getOpenInventory().getTopInventory().getHolder(false));
    }

    @Test
    void clickInOwnInventoryWhileMenuIsOpenIsCancelled() {
        player.getInventory().setItem(0, new ItemStack(Material.DIRT, 5));
        InventoryView view = openShop();
        int firstBottomRawSlot = view.getTopInventory().getSize();

        InventoryClickEvent event = click(view, ClickType.SHIFT_LEFT, firstBottomRawSlot);

        assertTrue(event.isCancelled(), "shift-click from own inventory must not move items into the menu");
        assertNull(player.nextComponentMessage(), "bottom inventory clicks are not passed to the menu");
    }

    @Test
    void dragInMenuIsCancelled() {
        InventoryView view = openShop();
        InventoryDragEvent drag = new InventoryDragEvent(view, new ItemStack(Material.DIRT), new ItemStack(Material.DIRT, 2),
                false, Map.of(0, new ItemStack(Material.DIRT), 1, new ItemStack(Material.DIRT)));

        Bukkit.getPluginManager().callEvent(drag);

        assertTrue(drag.isCancelled());
    }

    @Test
    void otherInventoriesAreNotTouched() {
        Inventory chest = Bukkit.createInventory(null, 27);
        InventoryView view = player.openInventory(chest);

        assertFalse(click(view, ClickType.LEFT, 0).isCancelled());
    }
}
