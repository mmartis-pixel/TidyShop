package cz.mmartis.tidyshop;

import cz.mmartis.tidyshop.config.ShopConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TidyShopPluginTest {

    private static final String ONE_CATEGORY = """
            categories:
              only: { icon: stone, slot: 0, items: { dirt: { buy: 1 } } }
            """;
    private static final String BROKEN_YAML = "categories: [unclosed\n";

    private ServerMock server;
    private TidyShopPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(TidyShopPlugin.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private void writeDataFile(String name, String content) throws IOException {
        Files.writeString(new File(plugin.getDataFolder(), name).toPath(), content);
    }

    private PlayerMock admin() {
        PlayerMock player = server.addPlayer();
        player.setOp(true);
        return player;
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Test
    void enableCreatesDefaultFilesAndLoadsShippedConfig() {
        assertTrue(plugin.isEnabled());
        assertTrue(new File(plugin.getDataFolder(), "config.yml").isFile());
        assertTrue(new File(plugin.getDataFolder(), "messages.yml").isFile());
        assertEquals(2, plugin.shopConfig().categories().size());
    }

    @Test
    void shopCommandRepliesFromMessagesFile() throws IOException {
        PlayerMock admin = admin();
        writeDataFile("messages.yml", "players-only: \"custom text\"\n");
        admin.performCommand("shop reload");
        admin.nextComponentMessage(); // reload confirmation

        server.dispatchCommand(server.getConsoleSender(), "shop");

        assertEquals("custom text", plain(server.getConsoleSender().nextComponentMessage()));
    }

    @Test
    void reloadAppliesChangedConfig() throws IOException {
        PlayerMock admin = admin();
        writeDataFile("config.yml", ONE_CATEGORY);

        admin.performCommand("shop reload");

        assertEquals(1, plugin.shopConfig().categories().size());
        assertEquals(plugin.messages().get("reload-success"), admin.nextComponentMessage());
    }

    @Test
    void reloadWithBrokenConfigKeepsOldConfigAndReportsError() throws IOException {
        PlayerMock admin = admin();
        ShopConfig before = plugin.shopConfig();
        writeDataFile("config.yml", BROKEN_YAML);

        admin.performCommand("shop reload");

        assertSame(before, plugin.shopConfig());
        assertTrue(plain(admin.nextComponentMessage()).contains("config.yml"));
    }

    @Test
    void reloadWithBrokenMessagesKeepsEverythingOld() throws IOException {
        PlayerMock admin = admin();
        ShopConfig before = plugin.shopConfig();
        writeDataFile("config.yml", ONE_CATEGORY); // valid, but must not be applied alone
        writeDataFile("messages.yml", BROKEN_YAML);

        admin.performCommand("shop reload");

        assertSame(before, plugin.shopConfig());
        assertTrue(plain(admin.nextComponentMessage()).contains("messages.yml"));
    }

    @Test
    void reloadWithoutPermissionDoesNothing() throws IOException {
        PlayerMock player = server.addPlayer(); // not op, so no tidyshop.admin
        ShopConfig before = plugin.shopConfig();
        writeDataFile("config.yml", ONE_CATEGORY);

        player.performCommand("shop reload");

        assertSame(before, plugin.shopConfig());
    }
}
