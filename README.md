# TidyShop

A small chest-GUI shop for Paper servers: players buy and sell vanilla items for Vault money, and nothing can be stolen or duplicated through the menu.

<!-- TODO before release: record docs/demo.gif (open /shop, buy, sell, under 15 s) and uncomment the next line -->
<!-- ![demo](docs/demo.gif) -->

## Features
- `/shop` opens a menu with your categories; each category page shows up to 45 items with their prices.
- Click to trade: left buys 1, shift+left buys a stack, right sells 1, shift+right sells everything of that item.
- Safe by design: every click in the menu is cancelled, a purchase is all or nothing, and if the economy refuses a payment the items go back to the player.
- Only clean items are bought back: renamed, enchanted or damaged items stay in the inventory.
- Exact prices with decimals (`0.1 x 64 = 6.40`), free items (price `0`), buy-only or sell-only items.
- All texts in `messages.yml` ([MiniMessage](https://docs.advntr.dev/minimessage/format.html)), shop layout in a commented `config.yml`, `/shop reload` without restart.

## Requirements
- Paper 26.3 (not tested on other versions or server software)
- Java 25
- Required plugins: [Vault](https://www.spigotmc.org/resources/vault.34315/) or [VaultUnlocked](https://modrinth.com/plugin/vaultunlocked), and an economy plugin, for example [EssentialsX](https://essentialsx.net/)

Without Vault the server does not load TidyShop. Without an economy plugin the shop opens, but every trade answers that the shop is closed, and the console shows one warning.

## Installation
1. Stop the server.
2. Put `TidyShop-0.1.0.jar` into the `plugins` folder.
3. Make sure Vault (or VaultUnlocked) and an economy plugin are installed.
4. Start the server. The config files are created in `plugins/TidyShop/`.
5. Edit the config, then run `/shop reload`.

## Commands and permissions
| Command | What it does | Permission | Default |
|---|---|---|---|
| `/shop` | Opens the shop (players only) | `tidyshop.use` | everyone |
| `/shop reload` | Reloads `config.yml` and `messages.yml` and closes all open shop menus | `tidyshop.admin` | operators |

## Clicks on an item
| Click | Action |
|---|---|
| left | buy 1 |
| shift + left | buy a full stack |
| right | sell 1 |
| shift + right | sell all clean pieces from the inventory (not armor or offhand) |
| anything else | nothing |

## Configuration
Every option has a comment in the file itself. The main ones:

| File | Option | What it does | Default |
|---|---|---|---|
| `config.yml` | `categories.<id>` | One category; the key is its id | `blocks`, `ores` |
| `config.yml` | `name` | Category title, MiniMessage | `<green>Blocks` |
| `config.yml` | `icon` | Icon in the main menu, vanilla material name | `grass_block` |
| `config.yml` | `slot` | Position in the main menu, 0-53 | `11` |
| `config.yml` | `items.<material>.buy` | Price of 1 piece; leave out = cannot be bought | `oak_log: 4` |
| `config.yml` | `items.<material>.sell` | Money for 1 piece; leave out = cannot be sold | `oak_log: 1` |
| `messages.yml` | all texts | Every message, the item lore and menu titles, in MiniMessage format | English |

A mistake in one category or item (unknown material, negative price, used slot) only skips that entry; the console says which one and why. A broken YAML file keeps the previous configuration.

After editing, run `/shop reload`. Do not use `/reload`; use `/shop reload` or restart the server.

## Data
The plugin stores nothing. Money is kept by your economy plugin.

## Known limits
- No paging: at most 45 items per category, the rest is skipped with a console warning.
- Vanilla items only, chosen by material name (no custom names, enchantments or NBT).
- Fixed prices, one currency, no transaction log.
- Paper 26.3 only, no Folia.

## Planned
- Paging for larger categories
- Choosing the amount in the GUI
- `/sell hand` and `/sell all`
- Permission per category, PlaceholderAPI support

## Support
Found a bug? [Open an issue](../../issues). Need a custom plugin? Contact me through my GitHub profile.

## Licence
MIT. See `LICENSE`.
