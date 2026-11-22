package cz.mmartis.guishop.trade;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicesManager;

import java.math.BigDecimal;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The only class that knows Vault. The Economy provider is looked up on every use, not in onEnable,
 * so an economy plugin that registers later is still found. Missing economy = one console warning, no crash.
 */
public final class VaultWallet implements Wallet {

    private final ServicesManager services;
    private final Logger logger;
    private boolean warned;

    public VaultWallet(ServicesManager services, Logger logger) {
        this.services = services;
        this.logger = logger;
    }

    @Override
    public boolean available() {
        return economy() != null;
    }

    @Override
    public boolean has(Player player, BigDecimal amount) {
        Economy economy = economy();
        return economy != null && economy.has(player, amount.doubleValue());
    }

    @Override
    public boolean withdraw(Player player, BigDecimal amount) {
        return transact(player, amount, Economy::withdrawPlayer);
    }

    @Override
    public boolean deposit(Player player, BigDecimal amount) {
        return transact(player, amount, Economy::depositPlayer);
    }

    @Override
    public String format(BigDecimal amount) {
        Economy economy = economy();
        return economy != null ? economy.format(amount.doubleValue()) : amount.toPlainString();
    }

    private Economy economy() {
        Economy economy = services.load(Economy.class);
        if (economy != null && economy.isEnabled()) {
            return economy;
        }
        if (!warned) {
            warned = true;
            logger.warning("No economy plugin is registered in Vault (for example EssentialsX). "
                    + "Buying and selling are disabled until one is installed.");
        }
        return null;
    }

    private interface Transaction {
        EconomyResponse run(Economy economy, OfflinePlayer player, double amount);
    }

    /** BigDecimal turns into double only here, at the Vault call. */
    private boolean transact(Player player, BigDecimal amount, Transaction transaction) {
        Economy economy = economy();
        if (economy == null) {
            return false;
        }
        try {
            EconomyResponse response = transaction.run(economy, player, amount.doubleValue());
            if (response != null && response.transactionSuccess()) {
                return true;
            }
            logger.warning(economy.getName() + " refused a transaction of " + amount.toPlainString()
                    + " for " + player.getName() + ": " + (response == null ? "no response" : response.errorMessage));
            return false;
        } catch (RuntimeException e) {
            // a broken economy plugin must not crash a trade halfway; the caller treats it as a refusal
            logger.log(Level.WARNING, economy.getName() + " failed during a transaction for " + player.getName(), e);
            return false;
        }
    }
}
