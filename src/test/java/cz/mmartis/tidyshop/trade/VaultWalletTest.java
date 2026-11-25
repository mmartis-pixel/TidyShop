package cz.mmartis.tidyshop.trade;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import org.bukkit.plugin.ServicePriority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VaultWalletTest {

    private ServerMock server;
    private PlayerMock player;
    private final List<LogRecord> log = new ArrayList<>();
    private VaultWallet wallet;

    // behaviour of the fake Economy provider
    private final List<String> calls = new ArrayList<>();
    private ResponseType response = ResponseType.SUCCESS;
    private boolean explode;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        player = server.addPlayer();
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override public void publish(LogRecord record) { log.add(record); }
            @Override public void flush() { }
            @Override public void close() { }
        });
        wallet = new VaultWallet(server.getServicesManager(), logger);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    /** Economy has dozens of methods; a proxy answers only the ones VaultWallet may call. */
    private void registerEconomy() {
        Economy economy = (Economy) Proxy.newProxyInstance(Economy.class.getClassLoader(), new Class<?>[]{Economy.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isEnabled", "has" -> true;
                    case "getName", "toString" -> "FakeEconomy";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "format" -> "$" + args[0];
                    case "withdrawPlayer", "depositPlayer" -> {
                        if (explode) {
                            throw new IllegalStateException("database down");
                        }
                        calls.add(method.getName() + " " + args[1]);
                        yield new EconomyResponse((double) args[1], 0, response, response == ResponseType.SUCCESS ? null : "refused");
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        server.getServicesManager().register(Economy.class, economy, MockBukkit.createMockPlugin(), ServicePriority.Normal);
    }

    // --- edge 4 ---

    @Test
    void missingEconomyFailsQuietlyAndWarnsOnce() {
        assertFalse(wallet.available());
        assertFalse(wallet.has(player, BigDecimal.ONE));
        assertFalse(wallet.withdraw(player, BigDecimal.ONE));
        assertFalse(wallet.deposit(player, BigDecimal.ONE));
        assertEquals("6.4", wallet.format(new BigDecimal("6.4")));

        assertEquals(1, log.size(), "one console warning, not one per call");
    }

    @Test
    void economyRegisteredAfterStartIsFound() {
        assertFalse(wallet.available());

        registerEconomy();

        assertTrue(wallet.available());
    }

    // --- passing amounts and reading the response (edges 6, 12) ---

    @Test
    void successfulTransactionsPassExactAmounts() {
        registerEconomy();

        assertTrue(wallet.withdraw(player, new BigDecimal("6.4")));
        assertTrue(wallet.deposit(player, new BigDecimal("0.3")));
        assertEquals(List.of("withdrawPlayer 6.4", "depositPlayer 0.3"), calls);
        assertEquals("$6.4", wallet.format(new BigDecimal("6.4")));
    }

    @Test
    void refusedTransactionIsFailure() {
        registerEconomy();
        response = ResponseType.FAILURE;

        assertFalse(wallet.withdraw(player, BigDecimal.TEN));
        assertFalse(wallet.deposit(player, BigDecimal.TEN));
    }

    @Test
    void economyExceptionIsFailureNotCrash() {
        registerEconomy();
        explode = true;

        assertFalse(wallet.withdraw(player, BigDecimal.TEN));
        assertFalse(wallet.deposit(player, BigDecimal.TEN));
        assertEquals(2, log.size());
    }
}
