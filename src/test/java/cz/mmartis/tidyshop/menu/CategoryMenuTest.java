package cz.mmartis.tidyshop.menu;

import cz.mmartis.tidyshop.TidyShopPlugin;
import cz.mmartis.tidyshop.trade.FakeWallet;
import cz.mmartis.tidyshop.trade.TradeService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryType;
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

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryMenuTest {

    // shipped config.yml: "ores" sits on main menu slot 15 and holds coal (sell 2), iron_ingot (20/8), diamond (200/100)
    private static final int ORES_SLOT = 15;
    private static final int BLOCKS_SLOT = 11;
    private static final int COAL = 0;
    private static final int IRON = 1;
    private static final int DIAMOND = 2;
    private static final int GLASS = 2; // in "blocks": oak_log, stone, glass (buy only)
    private static final int BACK = 49;

    private ServerMock server;
    private TidyShopPlugin plugin;
    private PlayerMock player;
    private FakeWallet wallet;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock(new HolderServerMock());
        plugin = MockBukkit.load(TidyShopPlugin.class);
        player = server.addPlayer();
        wallet = new FakeWallet();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // --- helpers ---

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /** Clicks, then runs one tick: switching menus is scheduled because the view must not change inside the click event. */
    private InventoryView click(ClickType type, int rawSlot) {
        new PlayerSimulation(player).simulateInventoryClick(player.getOpenInventory(), type, rawSlot);
        server.getScheduler().performOneTick();
        return player.getOpenInventory();
    }

    /** Opens the main menu with the fake wallet and clicks a category. */
    private Inventory openCategory(int categorySlot) {
        new MainMenu(plugin.shopConfig(), plugin.messages(), wallet, new TradeService(wallet)).open(player);
        return click(ClickType.LEFT, categorySlot).getTopInventory();
    }

    private int pieces(Material material) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == material) {
                count += stack.getAmount();
            }
        }
        return count;
    }

    private void assertBalance(String expected) {
        assertEquals(0, new BigDecimal(expected).compareTo(wallet.balance(player)), "balance " + wallet.balance(player));
    }

    private String nextMessage() {
        Component message = player.nextComponentMessage();
        return message == null ? null : plain(message);
    }

    // --- page layout ---

    @Test
    void categoryClickOpensItsPage() {
        Inventory page = openCategory(ORES_SLOT);

        assertInstanceOf(CategoryMenu.class, page.getHolder(false));
        assertEquals(54, page.getSize());
        assertEquals(Material.COAL, page.getItem(COAL).getType());
        assertEquals(Material.IRON_INGOT, page.getItem(IRON).getType());
        assertEquals(Material.DIAMOND, page.getItem(DIAMOND).getType());
        assertNull(page.getItem(3));
        assertEquals(Material.ARROW, page.getItem(BACK).getType());
    }

    @Test
    void loreShowsOnlyEnabledActionsAndIsNotItalic() {
        Inventory page = openCategory(ORES_SLOT);

        List<Component> coalLore = page.getItem(COAL).lore();
        String coal = String.join("\n", coalLore.stream().map(CategoryMenuTest::plain).toList());
        assertTrue(coal.contains("$2"), coal);
        assertFalse(coal.contains("Buy"), "coal cannot be bought: " + coal);
        assertFalse(coal.contains("Left"), "no buy hint for coal: " + coal);
        assertEquals(TextDecoration.State.FALSE, coalLore.getFirst().decoration(TextDecoration.ITALIC));

        String diamond = String.join("\n", page.getItem(DIAMOND).lore().stream().map(CategoryMenuTest::plain).toList());
        assertTrue(diamond.contains("$200"), diamond);
        assertTrue(diamond.contains("$12800"), "stack price 64 x 200: " + diamond);
        assertTrue(diamond.contains("$100"), diamond);
    }

    // --- click table from SPEC ---

    @Test
    void leftClickBuysOne() {
        wallet.set(player, "1000");
        openCategory(ORES_SLOT);

        click(ClickType.LEFT, DIAMOND);

        assertEquals(1, pieces(Material.DIAMOND));
        assertBalance("800");
        String message = nextMessage();
        assertTrue(message.contains("Bought 1x") && message.contains("$200"), message);
    }

    @Test
    void shiftLeftBuysStack() {
        wallet.set(player, "2000");
        openCategory(ORES_SLOT);

        click(ClickType.SHIFT_LEFT, IRON);

        assertEquals(64, pieces(Material.IRON_INGOT));
        assertBalance("720");
    }

    @Test
    void rightClickSellsOne() {
        player.getInventory().setItem(0, ItemStack.of(Material.COAL, 5));
        openCategory(ORES_SLOT);

        click(ClickType.RIGHT, COAL);

        assertEquals(4, pieces(Material.COAL));
        assertBalance("2");
        assertTrue(nextMessage().contains("Sold 1x"));
    }

    @Test
    void shiftRightSellsAll() {
        player.getInventory().setItem(0, ItemStack.of(Material.COAL, 5));
        player.getInventory().setItem(10, ItemStack.of(Material.COAL, 3));
        openCategory(ORES_SLOT);

        click(ClickType.SHIFT_RIGHT, COAL);

        assertEquals(0, pieces(Material.COAL));
        assertBalance("16");
        String message = nextMessage();
        assertTrue(message.contains("Sold 8x") && message.contains("$16"), message);
    }

    @Test
    void disabledActionDoesNothing() {
        wallet.set(player, "1000");
        player.getInventory().setItem(0, ItemStack.of(Material.GLASS, 5));
        openCategory(ORES_SLOT);
        click(ClickType.LEFT, COAL); // coal has no buy price
        assertNull(nextMessage());

        player.closeInventory();
        openCategory(BLOCKS_SLOT);
        click(ClickType.RIGHT, GLASS); // glass has no sell price

        assertNull(nextMessage());
        assertBalance("1000");
        assertEquals(0, pieces(Material.COAL));
        assertEquals(5, pieces(Material.GLASS));
    }

    @Test
    void otherClickTypesDoNothing() {
        wallet.set(player, "1000");
        player.getInventory().setItem(0, ItemStack.of(Material.DIAMOND, 1));
        openCategory(ORES_SLOT);

        for (ClickType type : List.of(ClickType.NUMBER_KEY, ClickType.DOUBLE_CLICK, ClickType.SWAP_OFFHAND,
                ClickType.MIDDLE, ClickType.DROP, ClickType.CONTROL_DROP, ClickType.CREATIVE)) {
            assertTrue(new PlayerSimulation(player)
                    .simulateInventoryClick(player.getOpenInventory(), type, DIAMOND).isCancelled(), type.name());
        }

        assertNull(nextMessage());
        assertBalance("1000");
        assertEquals(1, pieces(Material.DIAMOND));
    }

    @Test
    void emptySlotDoesNothing() {
        openCategory(ORES_SLOT);

        click(ClickType.LEFT, 20);

        assertNull(nextMessage());
        assertInstanceOf(CategoryMenu.class, player.getOpenInventory().getTopInventory().getHolder(false));
    }

    @Test
    void backButtonReturnsToMainMenu() {
        openCategory(ORES_SLOT);

        InventoryView view = click(ClickType.LEFT, BACK);

        assertInstanceOf(MainMenu.class, view.getTopInventory().getHolder(false));
    }

    // --- a message for every trade result ---

    @Test
    void noMoneyMessage() {
        wallet.set(player, "199.99");
        openCategory(ORES_SLOT);

        click(ClickType.LEFT, DIAMOND);

        assertTrue(nextMessage().contains("cannot afford"));
        assertEquals(0, pieces(Material.DIAMOND));
    }

    @Test
    void noSpaceMessage() {
        wallet.set(player, "1000");
        for (int slot = 0; slot < 36; slot++) {
            player.getInventory().setItem(slot, ItemStack.of(Material.STONE, 64));
        }
        openCategory(ORES_SLOT);

        click(ClickType.LEFT, DIAMOND);

        assertTrue(nextMessage().contains("Not enough room"));
        assertBalance("1000");
    }

    @Test
    void nothingToSellMessage() {
        openCategory(ORES_SLOT);

        click(ClickType.RIGHT, COAL);

        assertTrue(nextMessage().contains("to sell"));
    }

    @Test
    void noEconomyMessage() {
        wallet.available = false;
        openCategory(ORES_SLOT);

        click(ClickType.LEFT, DIAMOND);

        assertTrue(nextMessage().contains("no economy plugin"));
    }

    @Test
    void economyFailedMessage() {
        wallet.set(player, "1000");
        wallet.refuse = true;
        openCategory(ORES_SLOT);

        click(ClickType.LEFT, DIAMOND);

        assertTrue(nextMessage().contains("payment failed"));
        assertEquals(0, pieces(Material.DIAMOND));
    }

    // --- edge 10: two players trading at the same time ---

    private void clickAs(PlayerMock who, ClickType type, int rawSlot) {
        new PlayerSimulation(who).simulateInventoryClick(who.getOpenInventory(), type, rawSlot);
        server.getScheduler().performOneTick();
    }

    @Test
    void twoPlayersTradeIndependently() {
        PlayerMock other = server.addPlayer();
        wallet.set(player, "1000");
        other.getInventory().setItem(0, ItemStack.of(Material.COAL, 10));
        openCategory(ORES_SLOT);
        new MainMenu(plugin.shopConfig(), plugin.messages(), wallet, new TradeService(wallet)).open(other);
        clickAs(other, ClickType.LEFT, ORES_SLOT);

        clickAs(player, ClickType.LEFT, DIAMOND);
        clickAs(other, ClickType.SHIFT_RIGHT, COAL);
        clickAs(player, ClickType.LEFT, DIAMOND);

        assertBalance("600");
        assertEquals(2, pieces(Material.DIAMOND));
        assertEquals(0, new BigDecimal("20").compareTo(wallet.balance(other)));
        assertEquals(0, other.getInventory().all(Material.COAL).size());
        assertEquals(0, other.getInventory().all(Material.DIAMOND).size());
    }

    // --- edge 11: permission taken away while the menu is open ---

    @Test
    void tradeNeedsPermissionEvenWhenMenuIsAlreadyOpen() {
        wallet.set(player, "1000");
        openCategory(ORES_SLOT);
        player.addAttachment(plugin, "tidyshop.use", false); // e.g. negated in LuckPerms

        click(ClickType.LEFT, DIAMOND);

        assertEquals(0, pieces(Material.DIAMOND));
        assertBalance("1000");
        assertNull(nextMessage());
    }

    // --- edge 7: /shop reload with menus open ---

    @Test
    void reloadClosesShopMenusOfAllPlayers() {
        PlayerMock other = server.addPlayer();
        PlayerMock admin = server.addPlayer();
        admin.setOp(true);
        player.performCommand("shop");
        other.performCommand("shop");
        new PlayerSimulation(other).simulateInventoryClick(other.getOpenInventory(), ClickType.LEFT, ORES_SLOT);
        server.getScheduler().performOneTick();
        assertInstanceOf(CategoryMenu.class, other.getOpenInventory().getTopInventory().getHolder(false));

        admin.performCommand("shop reload");

        assertEquals(InventoryType.CRAFTING, player.getOpenInventory().getType(), "main menu closed");
        assertEquals(InventoryType.CRAFTING, other.getOpenInventory().getType(), "category page closed");
    }

    @Test
    void reloadLeavesOtherInventoriesOpen() {
        PlayerMock admin = server.addPlayer();
        admin.setOp(true);
        player.openInventory(server.createInventory(null, 27));

        admin.performCommand("shop reload");

        assertEquals(InventoryType.CHEST, player.getOpenInventory().getType());
    }

    @Test
    void pageDoesNotOpenIfMenuWasClosedBeforeNextTick() {
        new MainMenu(plugin.shopConfig(), plugin.messages(), wallet, new TradeService(wallet)).open(player);
        new PlayerSimulation(player).simulateInventoryClick(player.getOpenInventory(), ClickType.LEFT, ORES_SLOT);

        player.closeInventory(); // Esc, death or /shop reload in the same tick
        server.getScheduler().performOneTick();

        assertEquals(InventoryType.CRAFTING, player.getOpenInventory().getType());
    }

    // --- edge 9: plugin stops (server restart, /reload) with a menu open ---

    @Test
    void disablingPluginClosesShopMenus() {
        player.performCommand("shop");

        server.getPluginManager().disablePlugin(plugin);

        assertEquals(InventoryType.CRAFTING, player.getOpenInventory().getType(),
                "without our listener the icons could be taken, so the menu must not stay open");
    }
}
