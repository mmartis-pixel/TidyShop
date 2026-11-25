package cz.mmartis.tidyshop.config;

import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopConfigTest {

    private final List<String> warnings = new ArrayList<>();

    @BeforeEach
    void setUp() {
        MockBukkit.mock(); // Material.isItem() needs the server registries
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private ShopConfig parse(String yaml) throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return ShopConfig.parse(config, warnings::add);
    }

    private static void assertPrice(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    private void assertOneWarningContaining(String... parts) {
        assertEquals(1, warnings.size(), warnings.toString());
        for (String part : parts) {
            assertTrue(warnings.getFirst().contains(part), warnings.getFirst() + " should contain " + part);
        }
    }

    @Test
    void validConfigIsLoadedInOrder() throws Exception {
        ShopConfig config = parse("""
                categories:
                  blocks:
                    name: "<green>Blocks"
                    icon: grass_block
                    slot: 11
                    items:
                      oak_log: { buy: 4, sell: 1 }
                      stone: { buy: 2 }
                  ores:
                    name: "<aqua>Ores"
                    icon: diamond
                    slot: 15
                    items:
                      coal: { sell: 0.5 }
                      diamond: { buy: 0, sell: 100 }
                """);

        assertTrue(warnings.isEmpty(), warnings.toString());
        assertEquals(2, config.categories().size());

        Category blocks = config.categories().get(0);
        assertEquals("blocks", blocks.id());
        assertEquals("<green>Blocks", blocks.name());
        assertEquals(Material.GRASS_BLOCK, blocks.icon());
        assertEquals(11, blocks.slot());
        assertEquals(Material.OAK_LOG, blocks.items().get(0).material());
        assertPrice("4", blocks.items().get(0).buyPrice());
        assertPrice("1", blocks.items().get(0).sellPrice());
        assertNull(blocks.items().get(1).sellPrice(), "missing sell = selling disabled");

        Category ores = config.categories().get(1);
        assertNull(ores.items().get(0).buyPrice(), "missing buy = buying disabled");
        assertPrice("0.5", ores.items().get(0).sellPrice());
        assertPrice("0", ores.items().get(1).buyPrice()); // free is allowed
    }

    @Test
    void decimalPriceIsExact() throws Exception {
        ShopConfig config = parse("""
                categories:
                  c: { icon: stone, slot: 0, items: { dirt: { buy: 0.1 } } }
                """);
        // 0.1 must not become 0.1000000000000000055511151231257827 (double)
        assertEquals(new BigDecimal("0.1"), config.categories().getFirst().items().getFirst().buyPrice());
    }

    @Test
    void unknownMaterialIsSkippedAndRestStillLoads() throws Exception {
        ShopConfig config = parse("""
                categories:
                  ores:
                    icon: diamond
                    slot: 0
                    items:
                      coal: { buy: 1 }
                      diamnd: { buy: 2 }
                      iron_ingot: { buy: 3 }
                """);
        assertOneWarningContaining("ores", "diamnd", "unknown material");
        List<ShopItem> items = config.categories().getFirst().items();
        assertEquals(List.of(Material.COAL, Material.IRON_INGOT), items.stream().map(ShopItem::material).toList());
    }

    @Test
    void materialThatIsNotAnItemIsSkipped() throws Exception {
        ShopConfig config = parse("""
                categories:
                  c: { icon: stone, slot: 0, items: { water: { buy: 1 } } }
                """);
        assertOneWarningContaining("'c'", "water", "not an item");
        assertTrue(config.categories().getFirst().items().isEmpty());
    }

    @Test
    void negativePriceIsSkipped() throws Exception {
        ShopConfig config = parse("""
                categories:
                  c: { icon: stone, slot: 0, items: { dirt: { buy: 1, sell: -1 } } }
                """);
        assertOneWarningContaining("'c'", "dirt", "negative");
        assertTrue(config.categories().getFirst().items().isEmpty());
    }

    @Test
    void priceThatIsNotANumberIsSkipped() throws Exception {
        ShopConfig config = parse("""
                categories:
                  c: { icon: stone, slot: 0, items: { dirt: { buy: cheap } } }
                """);
        assertOneWarningContaining("'c'", "dirt", "not a number");
        assertTrue(config.categories().getFirst().items().isEmpty());
    }

    @Test
    void itemWithoutAnyPriceIsSkipped() throws Exception {
        ShopConfig config = parse("""
                categories:
                  c: { icon: stone, slot: 0, items: { dirt: {} } }
                """);
        assertOneWarningContaining("'c'", "dirt", "no buy or sell price");
        assertTrue(config.categories().getFirst().items().isEmpty());
    }

    @Test
    void categoryOnOccupiedSlotIsSkipped() throws Exception {
        ShopConfig config = parse("""
                categories:
                  first: { icon: stone, slot: 4 }
                  second: { icon: dirt, slot: 4 }
                """);
        assertOneWarningContaining("second", "slot 4");
        assertEquals(List.of("first"), config.categories().stream().map(Category::id).toList());
    }

    @Test
    void categoryWithInvalidIconOrSlotIsSkipped() throws Exception {
        ShopConfig config = parse("""
                categories:
                  badicon: { icon: notablock, slot: 0 }
                  badslot: { icon: stone, slot: 54 }
                  ok: { icon: stone, slot: 1 }
                """);
        assertEquals(2, warnings.size(), warnings.toString());
        assertTrue(warnings.get(0).contains("badicon"));
        assertTrue(warnings.get(1).contains("badslot"));
        assertEquals(List.of("ok"), config.categories().stream().map(Category::id).toList());
    }

    @Test
    void moreThan45ItemsAreCutWithWarning() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("categories.big.icon", "chest");
        yaml.set("categories.big.slot", 0);
        Arrays.stream(Material.values())
                .filter(m -> !m.name().startsWith("LEGACY_") && m.isItem() && !m.isAir())
                .limit(ShopConfig.MAX_ITEMS + 1)
                .forEach(m -> yaml.set("categories.big.items." + m.name().toLowerCase(Locale.ROOT) + ".buy", 1));

        ShopConfig config = ShopConfig.parse(yaml, warnings::add);

        assertEquals(ShopConfig.MAX_ITEMS, config.categories().getFirst().items().size());
        assertOneWarningContaining("big", "45");
    }

    @Test
    void missingCategoriesSectionGivesEmptyShop() throws Exception {
        ShopConfig config = parse("something: else\n");
        assertTrue(config.categories().isEmpty());
        assertEquals(1, warnings.size());
    }

    @Test
    void shippedConfigIsValid() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(
                Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("config.yml")),
                StandardCharsets.UTF_8));

        ShopConfig config = ShopConfig.parse(yaml, warnings::add);

        assertTrue(warnings.isEmpty(), warnings.toString());
        assertEquals(2, config.categories().size());
    }
}
