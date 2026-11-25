package cz.mmartis.tidyshop.trade;

import org.bukkit.entity.Player;

import java.math.BigDecimal;

/** Money operations the shop needs. Amounts are never negative. */
public interface Wallet {

    /** False when no economy is available; then has/withdraw/deposit return false. */
    boolean available();

    boolean has(Player player, BigDecimal amount);

    /** @return true only when the money was really taken */
    boolean withdraw(Player player, BigDecimal amount);

    /** @return true only when the money was really added */
    boolean deposit(Player player, BigDecimal amount);

    String format(BigDecimal amount);
}
