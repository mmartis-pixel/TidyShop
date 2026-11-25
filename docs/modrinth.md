# Modrinth page: TidyShop

Copy the parts below into the Modrinth project form.

**Name:** TidyShop
**Summary (max 256 chars):** A small, safe chest-GUI shop for Paper. Players buy and sell vanilla items for Vault money with simple clicks; nothing can be stolen or duplicated through the menu.
**Categories:** Economy, Management
**Environment:** Server only
**Loaders / versions:** Paper, 26.3
**Dependencies:** Vault (required), an economy plugin such as EssentialsX (required at runtime)

## Description (paste as the project body)

TidyShop is a shop that stays out of your way: one command, a chest menu with your categories, and clear prices on every item.

**How players use it**
- `/shop` opens the categories you defined.
- Left-click buys 1, shift+left buys a stack.
- Right-click sells 1, shift+right sells every clean piece they carry.

**Why it is safe**
- Every click and drag in the menu is cancelled, so menu items can never be taken.
- A purchase is all or nothing: not enough money or room means nothing happens.
- If the economy refuses a payment, sold items go straight back to the player.
- Renamed, enchanted or damaged items are never bought by accident.
- Prices use exact decimals, so `0.1 x 64` is exactly `6.40`.

**Easy to set up**
- Commented `config.yml`: categories, icons, slots and prices.
- Every text, including the item lore, in `messages.yml` with MiniMessage colors.
- `/shop reload` applies changes without a restart; a typo only skips that one entry and the console tells you which.

**Requirements:** Paper 26.3, Java 25, Vault or VaultUnlocked, and an economy plugin (for example EssentialsX).

**Permissions:** `tidyshop.use` (everyone, opens the shop), `tidyshop.admin` (operators, `/shop reload`).

**Not included (yet):** paging (max 45 items per category), choosing an amount, `/sell` commands, custom items, per-category permissions, PlaceholderAPI.

Source code, issues and full documentation: see the GitHub link on this page.
