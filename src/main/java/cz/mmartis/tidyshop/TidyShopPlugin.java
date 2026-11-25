package cz.mmartis.tidyshop;

import com.mojang.brigadier.Command;
import cz.mmartis.tidyshop.config.Messages;
import cz.mmartis.tidyshop.config.ShopConfig;
import cz.mmartis.tidyshop.menu.MainMenu;
import cz.mmartis.tidyshop.menu.Menu;
import cz.mmartis.tidyshop.menu.MenuListener;
import cz.mmartis.tidyshop.trade.TradeService;
import cz.mmartis.tidyshop.trade.VaultWallet;
import cz.mmartis.tidyshop.trade.Wallet;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

public class TidyShopPlugin extends JavaPlugin {

    private static final String MESSAGES_FILE = "messages.yml";

    private YamlConfiguration bundledMessages;
    private ShopConfig shopConfig = ShopConfig.EMPTY;
    private Messages messages;
    private Wallet wallet;
    private TradeService trade;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!new File(getDataFolder(), MESSAGES_FILE).exists()) {
            saveResource(MESSAGES_FILE, false);
        }
        bundledMessages = YamlConfiguration.loadConfiguration(new InputStreamReader(
                Objects.requireNonNull(getResource(MESSAGES_FILE)), StandardCharsets.UTF_8));
        messages = new Messages(bundledMessages);

        String error = reloadFiles();
        if (error != null) {
            getLogger().severe("Could not load " + error + ". The shop stays empty until /shop reload succeeds.");
        }

        // Vault's Economy is looked up at first use, so an economy plugin may register after us
        wallet = new VaultWallet(getServer().getServicesManager(), getLogger());
        trade = new TradeService(wallet);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);

        // Paper way: commands are registered via Brigadier in the lifecycle event,
        // not in plugin.yml. Survives /minecraft:reload.
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(
                        Commands.literal("shop")
                                .requires(source -> source.getSender().hasPermission("tidyshop.use"))
                                .executes(ctx -> openShop(ctx.getSource().getSender()))
                                .then(Commands.literal("reload")
                                        .requires(source -> source.getSender().hasPermission("tidyshop.admin"))
                                        .executes(ctx -> reload(ctx.getSource().getSender())))
                                .build(),
                        "Open the shop"
                )
        );
    }

    @Override
    public void onDisable() {
        // without our listener the menu icons could be taken, so no menu may stay open (restart, /reload)
        closeMenus();
    }

    public ShopConfig shopConfig() {
        return shopConfig;
    }

    public Messages messages() {
        return messages;
    }

    private int openShop(CommandSender sender) {
        if (sender instanceof Player player) {
            new MainMenu(shopConfig, messages, wallet, trade).open(player);
        } else {
            sender.sendMessage(messages.get("players-only"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int reload(CommandSender sender) {
        closeMenus(); // open menus hold the old prices and slots
        String error = reloadFiles();
        sender.sendMessage(error == null
                ? messages.get("reload-success")
                : messages.get("reload-failed", Map.of("error", error)));
        return Command.SINGLE_SUCCESS;
    }

    private void closeMenus() {
        for (Player player : getServer().getOnlinePlayers()) {
            if (Menu.openMenu(player) != null) {
                player.closeInventory();
            }
        }
    }

    /**
     * Loads config.yml and messages.yml. All or nothing: if either file is broken YAML,
     * the current config and messages stay active.
     *
     * @return null on success, otherwise a short "file: reason" description
     */
    private String reloadFiles() {
        YamlConfiguration configYaml;
        YamlConfiguration messagesYaml;
        try {
            configYaml = loadYaml("config.yml");
            messagesYaml = loadYaml(MESSAGES_FILE);
        } catch (InvalidConfigurationException e) {
            getLogger().warning(e.getMessage() + System.lineSeparator() + e.getCause().getMessage());
            return e.getMessage();
        }
        messagesYaml.setDefaults(bundledMessages);
        shopConfig = ShopConfig.parse(configYaml, getLogger()::warning);
        messages = new Messages(messagesYaml);
        return null;
    }

    /** Unlike YamlConfiguration.loadConfiguration, this fails on broken YAML instead of returning an empty config. */
    private YamlConfiguration loadYaml(String name) throws InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(new File(getDataFolder(), name));
        } catch (IOException | InvalidConfigurationException e) {
            String reason = String.valueOf(e.getMessage()).lines().findFirst().orElse("");
            throw new InvalidConfigurationException(name + ": " + reason, e);
        }
        return yaml;
    }
}
