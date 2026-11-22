package cz.mmartis.guishop;

import com.mojang.brigadier.Command;
import cz.mmartis.guishop.config.Messages;
import cz.mmartis.guishop.config.ShopConfig;
import cz.mmartis.guishop.menu.MainMenu;
import cz.mmartis.guishop.menu.MenuListener;
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

public class GuiShopPlugin extends JavaPlugin {

    private static final String MESSAGES_FILE = "messages.yml";

    private YamlConfiguration bundledMessages;
    private ShopConfig shopConfig = ShopConfig.EMPTY;
    private Messages messages;

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

        getServer().getPluginManager().registerEvents(new MenuListener(), this);

        // Paper way: commands are registered via Brigadier in the lifecycle event,
        // not in plugin.yml. Survives /minecraft:reload.
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(
                        Commands.literal("shop")
                                .requires(source -> source.getSender().hasPermission("guishop.use"))
                                .executes(ctx -> openShop(ctx.getSource().getSender()))
                                .then(Commands.literal("reload")
                                        .requires(source -> source.getSender().hasPermission("guishop.admin"))
                                        .executes(ctx -> reload(ctx.getSource().getSender())))
                                .build(),
                        "Open the shop"
                )
        );
    }

    public ShopConfig shopConfig() {
        return shopConfig;
    }

    public Messages messages() {
        return messages;
    }

    private int openShop(CommandSender sender) {
        if (sender instanceof Player player) {
            new MainMenu(shopConfig, messages).open(player);
        } else {
            sender.sendMessage(messages.get("players-only"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int reload(CommandSender sender) {
        String error = reloadFiles();
        sender.sendMessage(error == null
                ? messages.get("reload-success")
                : messages.get("reload-failed", Map.of("error", error)));
        return Command.SINGLE_SUCCESS;
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
