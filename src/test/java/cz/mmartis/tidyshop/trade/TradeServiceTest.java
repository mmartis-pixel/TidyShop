package cz.mmartis.tidyshop.trade;

import cz.mmartis.tidyshop.config.ShopItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TradeServiceTest {

    private static final ShopItem DIAMOND = new ShopItem(Material.DIAMOND, new BigDecimal("10"), new BigDecimal("4"));
    private static final int ALL = Integer.MAX_VALUE;

    private PlayerMock player;
    private FakeWallet wallet;
    private TradeService trade;

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        player = server.addPlayer();
        wallet = new FakeWallet();
        trade = new TradeService(wallet);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // --- helpers ---

    /** Clean pieces of the material anywhere in the player inventory (incl. armor and offhand slots). */
    private int clean(Material material) {
        ItemStack like = ItemStack.of(material);
        int count = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.isSimilar(like)) {
                count += stack.getAmount();
            }
        }
        return count;
    }

    private void fillStorage(int slots, Material material) {
        for (int slot = 0; slot < slots; slot++) {
            player.getInventory().setItem(slot, ItemStack.of(material, material.getMaxStackSize()));
        }
    }

    private static ItemStack renamed(Material material) {
        ItemStack stack = ItemStack.of(material);
        stack.editMeta(meta -> meta.customName(Component.text("Shiny")));
        return stack;
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    // --- edge 2: buying ---

    @Test
    void buyWithExactMoneyGivesItem() {
        wallet.set(player, "10");

        Trade result = trade.buy(player, DIAMOND, 1);

        assertEquals(TradeResult.OK, result.result());
        assertEquals(1, result.amount());
        assertMoney("10", result.money());
        assertMoney("0", wallet.balance(player));
        assertEquals(1, clean(Material.DIAMOND));
    }

    @Test
    void buyOneCentShortChangesNothing() {
        wallet.set(player, "9.99");

        assertEquals(TradeResult.NO_MONEY, trade.buy(player, DIAMOND, 1).result());
        assertMoney("9.99", wallet.balance(player));
        assertEquals(0, clean(Material.DIAMOND));
    }

    @Test
    void buyWholeStack() {
        wallet.set(player, "1000");

        assertEquals(TradeResult.OK, trade.buy(player, DIAMOND, 64).result());
        assertMoney("360", wallet.balance(player));
        assertEquals(64, clean(Material.DIAMOND));
    }

    @Test
    void buyWithFullInventoryChangesNothing() {
        fillStorage(36, Material.STONE);
        wallet.set(player, "100");

        assertEquals(TradeResult.NO_SPACE, trade.buy(player, DIAMOND, 1).result());
        assertMoney("100", wallet.balance(player));
        assertEquals(0, clean(Material.DIAMOND), "nothing may land in armor or offhand slots either");
    }

    @Test
    void buyWithRoomForOnlyPartOfStackChangesNothing() {
        fillStorage(35, Material.STONE);
        player.getInventory().setItem(35, ItemStack.of(Material.DIAMOND, 60));
        wallet.set(player, "1000");

        assertEquals(TradeResult.NO_SPACE, trade.buy(player, DIAMOND, 64).result());
        assertMoney("1000", wallet.balance(player));
        assertEquals(60, clean(Material.DIAMOND));
    }

    @Test
    void buyFillsPartialStack() {
        fillStorage(35, Material.STONE);
        player.getInventory().setItem(35, ItemStack.of(Material.DIAMOND, 32));
        wallet.set(player, "1000");

        assertEquals(TradeResult.OK, trade.buy(player, DIAMOND, 32).result());
        assertEquals(64, clean(Material.DIAMOND));
    }

    @Test
    void modifiedStackIsNoRoomForCleanItems() {
        fillStorage(35, Material.STONE);
        player.getInventory().setItem(35, renamed(Material.DIAMOND));
        wallet.set(player, "1000");

        assertEquals(TradeResult.NO_SPACE, trade.buy(player, DIAMOND, 1).result());
        assertMoney("1000", wallet.balance(player));
    }

    // --- edge 3: selling ---

    @Test
    void sellWithNothingToSell() {
        Trade result = trade.sell(player, DIAMOND, ALL);

        assertEquals(TradeResult.NOTHING_TO_SELL, result.result());
        assertEquals(0, result.amount());
        assertMoney("0", wallet.balance(player));
    }

    @Test
    void sellOnePiece() {
        player.getInventory().setItem(0, ItemStack.of(Material.DIAMOND, 5));

        Trade result = trade.sell(player, DIAMOND, 1);

        assertEquals(TradeResult.OK, result.result());
        assertEquals(1, result.amount());
        assertMoney("4", wallet.balance(player));
        assertEquals(4, clean(Material.DIAMOND));
    }

    @Test
    void sellAllFromSeveralSlots() {
        player.getInventory().setItem(0, ItemStack.of(Material.DIAMOND, 10));
        player.getInventory().setItem(9, ItemStack.of(Material.DIAMOND, 20));
        player.getInventory().setItem(30, ItemStack.of(Material.DIAMOND, 5));

        Trade result = trade.sell(player, DIAMOND, ALL);

        assertEquals(TradeResult.OK, result.result());
        assertEquals(35, result.amount());
        assertMoney("140", result.money());
        assertMoney("140", wallet.balance(player));
        assertEquals(0, clean(Material.DIAMOND));
    }

    @Test
    void sellAllKeepsRenamedPiece() {
        ItemStack renamed = renamed(Material.DIAMOND);
        player.getInventory().setItem(0, ItemStack.of(Material.DIAMOND, 10));
        player.getInventory().setItem(1, renamed);
        player.getInventory().setItem(2, ItemStack.of(Material.DIAMOND, 3));

        Trade result = trade.sell(player, DIAMOND, ALL);

        assertEquals(13, result.amount());
        assertMoney("52", wallet.balance(player));
        assertEquals(renamed, player.getInventory().getItem(1), "renamed piece stays");
    }

    // --- edge 4: no economy provider ---

    @Test
    void noEconomyBlocksBuyAndSell() {
        wallet.available = false;
        wallet.set(player, "100");
        player.getInventory().setItem(0, ItemStack.of(Material.DIAMOND, 5));

        assertEquals(TradeResult.NO_ECONOMY, trade.buy(player, DIAMOND, 1).result());
        assertEquals(TradeResult.NO_ECONOMY, trade.sell(player, DIAMOND, ALL).result());
        assertMoney("100", wallet.balance(player));
        assertEquals(5, clean(Material.DIAMOND));
    }

    // --- edge 6: economy refuses the transaction ---

    @Test
    void refusedWithdrawGivesNothing() {
        wallet.set(player, "100");
        wallet.refuse = true;

        assertEquals(TradeResult.ECONOMY_FAILED, trade.buy(player, DIAMOND, 1).result());
        assertMoney("100", wallet.balance(player));
        assertEquals(0, clean(Material.DIAMOND));
    }

    @Test
    void refusedDepositReturnsItems() {
        player.getInventory().setItem(0, ItemStack.of(Material.DIAMOND, 10));
        player.getInventory().setItem(5, ItemStack.of(Material.DIAMOND, 7));
        wallet.refuse = true;

        assertEquals(TradeResult.ECONOMY_FAILED, trade.sell(player, DIAMOND, ALL).result());
        assertMoney("0", wallet.balance(player));
        assertEquals(17, clean(Material.DIAMOND));
    }

    // --- edge 12: decimals and free items ---

    @Test
    void decimalPriceIsExact() {
        ShopItem stone = new ShopItem(Material.STONE, new BigDecimal("0.1"), new BigDecimal("0.1"));
        player.getInventory().setItem(0, ItemStack.of(Material.STONE, 64));

        Trade sold = trade.sell(player, stone, ALL);
        assertMoney("6.4", sold.money());
        assertMoney("6.4", wallet.balance(player));

        Trade bought = trade.buy(player, stone, 3);
        assertMoney("0.3", bought.money()); // 0.1 * 3 in double is 0.30000000000000004
        assertMoney("6.1", wallet.balance(player));
    }

    @Test
    void freeItemWorksWithoutMoneyAndWithoutCallingEconomy() {
        ShopItem free = new ShopItem(Material.STONE, BigDecimal.ZERO, BigDecimal.ZERO);
        wallet.refuse = true; // would fail if the economy were called

        assertEquals(TradeResult.OK, trade.buy(player, free, 1).result());
        assertEquals(1, clean(Material.STONE));
        assertEquals(TradeResult.OK, trade.sell(player, free, 1).result());
        assertEquals(0, clean(Material.STONE));
        assertMoney("0", wallet.balance(player));
    }

    // --- guards ---

    @Test
    void disabledActionIsRejected() {
        ShopItem sellOnly = new ShopItem(Material.COAL, null, new BigDecimal("2"));
        ShopItem buyOnly = new ShopItem(Material.GLASS, new BigDecimal("3"), null);

        assertThrows(IllegalArgumentException.class, () -> trade.buy(player, sellOnly, 1));
        assertThrows(IllegalArgumentException.class, () -> trade.sell(player, buyOnly, 1));
    }
}
