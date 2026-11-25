package cz.mmartis.tidyshop.trade;

import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** In-memory wallet with exact BigDecimal balances. */
public class FakeWallet implements Wallet {

    public boolean available = true;
    /** Simulates an economy that rejects every transaction (transactionSuccess() == false). */
    public boolean refuse;

    private final Map<UUID, BigDecimal> balances = new HashMap<>();

    public void set(Player player, String amount) {
        balances.put(player.getUniqueId(), new BigDecimal(amount));
    }

    public BigDecimal balance(Player player) {
        return balances.getOrDefault(player.getUniqueId(), BigDecimal.ZERO);
    }

    @Override
    public boolean available() {
        return available;
    }

    @Override
    public boolean has(Player player, BigDecimal amount) {
        return balance(player).compareTo(amount) >= 0;
    }

    @Override
    public boolean withdraw(Player player, BigDecimal amount) {
        if (refuse || !has(player, amount)) {
            return false;
        }
        balances.put(player.getUniqueId(), balance(player).subtract(amount));
        return true;
    }

    @Override
    public boolean deposit(Player player, BigDecimal amount) {
        if (refuse) {
            return false;
        }
        balances.put(player.getUniqueId(), balance(player).add(amount));
        return true;
    }

    @Override
    public String format(BigDecimal amount) {
        return "$" + amount.toPlainString();
    }
}
