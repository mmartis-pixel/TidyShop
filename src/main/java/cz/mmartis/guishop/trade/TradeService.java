package cz.mmartis.guishop.trade;

import cz.mmartis.guishop.config.ShopItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.math.BigDecimal;

/**
 * Buy and sell logic, independent of the GUI. Only clean items (isSimilar with a fresh stack) count,
 * and only the 36 main inventory slots are used, never armor or offhand.
 */
public final class TradeService {

    private final Wallet wallet;

    public TradeService(Wallet wallet) {
        this.wallet = wallet;
    }

    /** All or nothing. Order: check, take money, give items. */
    public Trade buy(Player player, ShopItem item, int amount) {
        if (item.buyPrice() == null || amount < 1) {
            throw new IllegalArgumentException("Cannot buy " + amount + " " + item.material());
        }
        BigDecimal price = item.buyPrice().multiply(BigDecimal.valueOf(amount));
        ItemStack clean = ItemStack.of(item.material());

        if (!wallet.available()) {
            return new Trade(TradeResult.NO_ECONOMY, amount, price);
        }
        if (freeSpace(player.getInventory(), clean) < amount) {
            return new Trade(TradeResult.NO_SPACE, amount, price);
        }
        if (price.signum() > 0) { // free items never touch the economy
            if (!wallet.has(player, price)) {
                return new Trade(TradeResult.NO_MONEY, amount, price);
            }
            if (!wallet.withdraw(player, price)) {
                return new Trade(TradeResult.ECONOMY_FAILED, amount, price);
            }
        }
        give(player, clean, amount);
        return new Trade(TradeResult.OK, amount, price);
    }

    /**
     * Sells up to {@code maxAmount} pieces ({@code Integer.MAX_VALUE} = all).
     * Order: take items, pay, give the items back if paying fails.
     */
    public Trade sell(Player player, ShopItem item, int maxAmount) {
        if (item.sellPrice() == null || maxAmount < 1) {
            throw new IllegalArgumentException("Cannot sell " + maxAmount + " " + item.material());
        }
        ItemStack clean = ItemStack.of(item.material());
        PlayerInventory inventory = player.getInventory();

        if (!wallet.available()) {
            return new Trade(TradeResult.NO_ECONOMY, 0, BigDecimal.ZERO);
        }
        int amount = Math.min(maxAmount, count(inventory, clean));
        if (amount == 0) {
            return new Trade(TradeResult.NOTHING_TO_SELL, 0, BigDecimal.ZERO);
        }
        BigDecimal money = item.sellPrice().multiply(BigDecimal.valueOf(amount));

        take(inventory, clean, amount);
        if (money.signum() > 0 && !wallet.deposit(player, money)) {
            give(player, clean, amount);
            return new Trade(TradeResult.ECONOMY_FAILED, amount, money);
        }
        return new Trade(TradeResult.OK, amount, money);
    }

    private static int freeSpace(PlayerInventory inventory, ItemStack clean) {
        int max = clean.getMaxStackSize();
        int free = 0;
        for (ItemStack stack : inventory.getStorageContents()) {
            if (stack == null || stack.isEmpty()) {
                free += max;
            } else if (stack.isSimilar(clean)) {
                free += Math.max(0, max - stack.getAmount());
            }
        }
        return free;
    }

    private static int count(PlayerInventory inventory, ItemStack clean) {
        int count = 0;
        for (ItemStack stack : inventory.getStorageContents()) {
            if (stack != null && stack.isSimilar(clean)) {
                count += stack.getAmount();
            }
        }
        return count;
    }

    /** Storage contents index = player inventory slot (0-8 hotbar, 9-35 main). */
    private static void take(PlayerInventory inventory, ItemStack clean, int amount) {
        ItemStack[] storage = inventory.getStorageContents();
        for (int slot = 0; slot < storage.length && amount > 0; slot++) {
            ItemStack stack = storage[slot];
            if (stack == null || !stack.isSimilar(clean)) {
                continue;
            }
            int taken = Math.min(amount, stack.getAmount());
            amount -= taken;
            int left = stack.getAmount() - taken;
            inventory.setItem(slot, left == 0 ? null : stack.asQuantity(left));
        }
    }

    /** Space was checked before; whatever still does not fit is dropped at the player's feet, never lost. */
    private static void give(Player player, ItemStack clean, int amount) {
        int max = clean.getMaxStackSize();
        for (int left = amount; left > 0; left -= max) {
            player.getInventory().addItem(clean.asQuantity(Math.min(left, max))).values()
                    .forEach(rest -> player.getWorld().dropItem(player.getLocation(), rest));
        }
    }
}
