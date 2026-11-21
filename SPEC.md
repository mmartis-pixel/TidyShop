---
type: spec
date: 2026-11-16
project: Minecraft Commissions
plugin: a) GUI shop
status: schváleno ke stavbě, začít modulem 0
---

# Spec a) GUI shop + Vault

Pracovní název: `guishop` (repo `mmartis-pixel/guishop`, balíček `cz.mmartis.guishop`). **Veřejný název změnit před vydáním:** „GUIShop“ už je známý cizí plugin (pablo67340). Návrh: `TidyShop`. Ověřit ručně, že je na Modrinthu volný.
Cíl: do **2026-12-15** vydat na GitHub + Modrinth. Odhad: **22–24 h**. Při 5–10 h týdně jsou to 4 týdny bez rezervy na dolní hranici, proto je scope malý a má pravidlo pro škrtání (viz Plán).

## Proč tenhle plugin
Na něm se učím Paper API a zároveň z něj vznikne šablona pro zakázky. Klientovi má za 30 sekund ukázat tři věci, které se objednávají nejčastěji:
- GUI v inventáři, ze kterého nejde nic ukrást ani zduplikovat
- peníze přes Vault, bez ztráty peněz nebo itemů při chybě
- čistý config a zprávy, které si majitel serveru upraví sám

Nemá porazit EconomyShopGUI. Má být malý, bez chyb a s čitelným kódem. Části, které se pak kopírují do zakázek: načítání configu a zpráv (modul 1), menu framework (modul 2), napojení na Vault (modul 3).

## Výchozí stav repa (2026-11-16)
- Gradle 9.9.1, Paper API `26.3.build.252-stable`, Java 25, run-paper 3.1.0, MockBukkit 4.120.0, JUnit 5.
- `GuiShopPlugin` registruje `/shop` přes Brigadier (`LifecycleEvents.COMMANDS`) a odpoví „hello“. Dva testy (plugin se zapne, `/shop` odpoví) procházejí.
- `plugin.yml`: `api-version: '26.3'`, žádné závislosti, žádný config.
- **Neověřeno:** `./gradlew runServer` a `/shop` ve hře. To je modul 0.

## Scope V1

**Menu**
- `/shop` otevře hlavní menu s kategoriemi z configu (ikona, název, slot). Klik na kategorii otevře její stránku.
- Stránka kategorie: 6 řad. Horních 5 řad (45 slotů) jsou itemy, spodní řada má tlačítko Zpět. **Bez stránkování:** víc než 45 itemů v kategorii = varování v konzoli a itemy navíc se nezobrazí.
- U každého itemu lore z šablony ve `messages.yml`: nákupní cena, výkupní cena, nápověda ke klikání.

**Nákup a prodej (jen klikáním)**
| Klik | Akce |
|---|---|
| levý | koupit 1 |
| shift + levý | koupit celý stack (podle `getMaxStackSize()`) |
| pravý | prodat 1 |
| shift + pravý | prodat všechny kusy toho itemu z inventáře |
| cokoli jiného | nic (klik se zruší) |

- Item může mít jen nákupní cenu, jen výkupní, nebo obě. Chybějící cena = ta akce je vypnutá a lore ji neukazuje.
- Nákup je všechno, nebo nic: nedostatek peněz nebo místa v inventáři = zpráva a nic se nestane.
- Prodávají se jen neupravené itemy (`isSimilar` s čistým itemem). Přejmenované, očarované nebo poškozené kusy se nepočítají.
- Jen vanilla itemy podle jména materiálu (`diamond`, `oak_log`).

**Peníze**
- Vault je tvrdá závislost (`depend: [Vault]`). Funguje s Vaultem i VaultUnlocked.
- Poskytovatel ekonomiky se hledá až při prvním použití, ne v `onEnable` (ekonomický plugin se může zaregistrovat později). Když žádný není: hráč dostane zprávu, konzole jedno varování, plugin běží dál.
- Pořadí kroků proti duplikaci: **nákup** = strhnout peníze → ověřit úspěch → dát itemy. **Prodej** = odebrat itemy → připsat peníze → když připsání selže, itemy vrátit.

**Příkazy a permissions**
| Příkaz | Permission | Default |
|---|---|---|
| `/shop` | `guishop.use` | všichni |
| `/shop reload` | `guishop.admin` | OP |

`/shop` z konzole odpoví, že je jen pro hráče.

**Config**
- `config.yml`: kategorie (id, název, ikona, slot) a jejich itemy (materiál, `buy`, `sell`). Každý klíč s komentářem.
- `messages.yml`: všechny texty v MiniMessage včetně prefixu a šablony lore. Nic natvrdo v kódu.
- Chyba v jednom itemu (neznámý materiál, materiál není item, záporná cena, obsazený slot) = varování s názvem kategorie a itemu, item se přeskočí, zbytek funguje.
- `/shop reload`: zavře všem otevřená menu shopu, načte oba soubory. Rozbitý YAML = zůstane stará konfigurace a odesílatel dostane chybu.

## Mimo scope (do README jako „planned“)
Stránkování, výběr množství v GUI, `/sell hand|all`, vlastní itemy (jména, enchanty, NBT), permission na kategorii, slevy podle ranku, dynamické ceny, víc měn, PlaceholderAPI, editor ve hře, log transakcí, databáze, Folia, starší verze než 26.3.

## Hrany, které musí být otestované
1. Klikání v menu: levý, pravý, shift, čísla 1–9, dvojklik, klávesa F (offhand), tažení přes sloty, klik do vlastního inventáře se shiftem. Nic nesmí jít vzít ani vložit.
2. Nákup: přesně tolik peněz, o haléř míň, plný inventář, místo jen na část stacku.
3. Prodej: 0 kusů, 1 kus, kusy rozložené ve víc slotech, přejmenovaný kus mezi čistými (zůstane).
4. Chybí poskytovatel ekonomiky (jen Vault bez EssentialsX): zpráva, žádný stack trace.
5. Chybí Vault: server plugin nenačte a v logu je srozumitelná hláška.
6. Ekonomika odmítne transakci (`transactionSuccess() == false`): žádné itemy, žádná ztráta.
7. `/shop reload` při otevřeném menu u dvou hráčů.
8. Rozbitý `config.yml` při startu a při reloadu; item s chybou uprostřed kategorie.
9. Hráč umře, je teleportován nebo vyhozen s otevřeným menu; restart serveru s otevřeným menu.
10. Dva hráči nakupují a prodávají současně.
11. Permissions: hráč bez `guishop.use`, ne-OP na `/shop reload`, záporná permission v LuckPerms.
12. Cena s desetinami (0.1 × 64) se nezaokrouhlí špatně; cena 0 (zdarma) funguje.

## Technická rozhodnutí
(Ověření tvrzení: [[Research - Paper 26.3 API]].)
- Klasický `plugin.yml`, `api-version: '26.3'`. Jar proto nenaběhne na 26.1 a 26.2, což je pro portfolio v pořádku (testuju jen 26.3).
- Příkazy: Brigadier v `onEnable`, už hotovo. `/shop reload` jako podpříkaz s `.requires(...)` na permission. Permissions deklarované v `plugin.yml`.
- GUI: třída menu implementuje `InventoryHolder` (vzor z Paper docs). V listeneru `event.getInventory().getHolder(false) instanceof ShopMenu` a klik se **vždy** zruší. Stejně `InventoryDragEvent`. Žádné mapy „hráč → otevřené menu“.
- Názvy itemů v menu přes `itemName(Component)` nebo `customName(Component)`. `displayName(Component)` je v 26.3 zastaralé.
- Peníze za vlastním malým rozhraním `Wallet` (`has`, `withdraw`, `deposit`, `format`). Implementace `VaultWallet` je jediné místo, které zná Vault. Testy používají `FakeWallet`, takže nepotřebují Vault ani plný `Economy` (má desítky metod).
- Vault API: `compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")` z `https://jitpack.io`, bez tranzitivních závislostí.
- Výpočet ceny přes `BigDecimal` (cena × množství), na `double` až při volání Vaultu.
- Logika obchodu (`TradeService`: kontroly, pořadí kroků, vracení při chybě) oddělená od GUI. GUI jen překládá klik na „kup 1 / prodej vše“.
- Config se načte do neměnných objektů (`ShopConfig`, `Category`, `ShopItem`). Reload = vyrobit nový objekt a prohodit, když je validní.
- Testovací server přes run-paper `downloadPlugins`: VaultUnlocked 2.20.3, LuckPerms 5.5.71, ekonomika EssentialsX. ⚠️ EssentialsX nemá vydání označené pro 26.3, v modulu 0 ověřit, že běží.
- Hlavní třída pluginu nesmí být `final` (MockBukkit ji dědí). Žádné NMS.

## Paper věci, které se tu naučím
- Životní cyklus pluginu, `plugin.yml`, závislosti, permissions
- Brigadier příkazy a podpříkazy
- Inventáře: `InventoryHolder`, `InventoryClickEvent`, `InventoryDragEvent`, typy kliknutí
- `ItemStack`, `ItemMeta`, Adventure komponenty a MiniMessage
- `ServicesManager` a Vault
- Config API (`saveDefaultConfig`, druhý soubor přes `saveResource`)
- MockBukkit: co jde otestovat bez serveru a co ne

## Plán po týdnech
| Týden | Moduly | Kontrola |
|---|---|---|
| 16.–22. 11. | 0, 1 | `/shop` ve hře funguje, reload načítá config |
| 23.–29. 11. | 2, 3 | hlavní menu se otevře, logika obchodu má testy |
| 30. 11.–6. 12. | 4, 5 | nákup a prodej ve hře, hrany 1–12 prošly |
| 7.–13. 12. | 6 | README, GIF, **odeslat na Modrinth do 9. 12.** |
| 14.–15. 12. | rezerva | – |

**Pravidlo pro škrtání:** když 6. 12. není hotový modul 4, vypouští se shift-kliky (zůstane koupit 1 / prodat 1) a hrana 12. Stránkování se smí přidat jen tehdy, když je 6. 12. hotový modul 5. Modrinth nový projekt schvaluje ručně a délku fronty neznám (⚠️), proto odeslat s týdenním předstihem.

## Moduly a prompty pro Claude Code

**Příprava (ručně):** zkopírovat tenhle soubor do repa jako `SPEC.md`.

**Společná hlavička (vložit před každý prompt):**
```
Přečti SPEC.md. Udělej jen modul uvedený níže, nic mimo jeho scope.
Postup: nejdřív testy logiky (JUnit/MockBukkit), pak implementace.
Paper 26.3 API ověřuj v javadocu (jd.papermc.io), nehádej z paměti.
V MockBukkitu simuluj kliknutí přes PlayerSimulation, ne přes zastaralé metody na PlayerMock.
Co MockBukkit neumí nasimulovat, netestuj oklikou: dej to do herního checklistu.
Build na mém stroji trvá asi 5 minut. Spusť ./gradlew build jednou na konci, ne po každé změně.
Paper-specifické věci mi krátce vysvětli, Javu ne.
Na konci: shrnutí změn a checklist, co mám otestovat ve hře.
Necommituj, dokud nepotvrdím test ve hře.
```

| # | Modul | Hodiny | Model / effort | Prompt (za hlavičku) |
|---|---|---|---|---|
| 0 | Ověření prostředí | 1 | ručně + Sonnet, low | `Modul 0: nic neprogramuj. Přidej do build.gradle.kts repozitář jitpack, compileOnly VaultAPI 1.7.1 bez tranzitivních závislostí a do runServer blok downloadPlugins (VaultUnlocked 2.20.3, LuckPerms, EssentialsX) a jvmArgs -Xmx1G. Do plugin.yml přidej depend: [Vault] a permissions guishop.use a guishop.admin. Pak mi řekni, jak spustit runServer a co zkontrolovat v konzoli.` |
| 1 | Config, zprávy, reload | 3 | Sonnet, medium | viz „První prompt“ níže |
| 2 | Menu framework + hlavní menu | 4 | Sonnet, high | `Modul 2: abstraktní Menu implementující InventoryHolder, jeden listener na InventoryClickEvent a InventoryDragEvent, který pro naše menu klik vždy zruší a předá menu (slot, ClickType). Hlavní menu s kategoriemi z ShopConfig, /shop ho otevře hráči, konzoli odpoví zprávou. Klik na kategorii zatím jen pošle zprávu s jejím id. Testy: /shop otevře inventář s naším holderem, klik se zruší. Herní checklist pro hranu 1.` |
| 3 | Wallet + logika obchodu | 4 | **Opus, high** | `Modul 3: rozhraní Wallet, VaultWallet (líné hledání Economy přes ServicesManager, žádný pád když chybí) a TradeService s metodami buy a sell podle sekcí Nákup a prodej a Peníze. Výsledek jako enum (OK, NO_MONEY, NO_SPACE, NOTHING_TO_SELL, NO_ECONOMY, ECONOMY_FAILED). Testy s FakeWallet a PlayerMock inventářem pro hrany 2, 3, 4, 6 a 12, včetně toho, že při selhání nikdo nepřijde o peníze ani itemy.` |
| 4 | Stránka kategorie + napojení | 4 | Sonnet, high | `Modul 4: menu kategorie (45 slotů itemů, tlačítko Zpět), lore ze šablony v messages.yml, kliky podle tabulky ve SPEC.md volají TradeService a pošlou zprávu podle výsledku. /shop reload zavře otevřená menu. Hrany 7 a 9.` |
| 5 | Hrany ve hře | 3 | Sonnet, medium | `Modul 5: nic nového. Projdu s tebou hrany 1–12 ze SPEC.md. Ke každé mi napiš přesný postup testu ve hře. Co nahlásím jako chybu, oprav s testem, který ji zachytí.` |
| 6 | Review a vydání | 4 | **Opus, high** | `Modul 6: nic neopravuj. Projdi celý kód proti SPEC.md, vypiš nálezy podle závažnosti (soubor, řádek, návrh opravy), zvlášť cesty, kde může zmizet nebo vzniknout item či peníze. Po mém schválení oprav. Pak napiš README podle šablony (instalace, příkazy, permissions, config) a text stránky na Modrinth.` |

**První prompt (modul 1), za společnou hlavičku:**
```
Modul 1: načítání config.yml a messages.yml.
- config.yml: kategorie (id, název v MiniMessage, ikona, slot) a jejich itemy (materiál, buy, sell).
  Přibal ukázkový config se dvěma kategoriemi a komentářem u každého klíče.
- Načti ho do neměnných objektů ShopConfig, Category, ShopItem. Validace podle sekce Config ve SPEC.md:
  chybný item přeskoč s varováním (kategorie + item + důvod), plugin běží dál.
- messages.yml: všechny texty v MiniMessage, prefix, třída Messages s metodou, která doplní
  placeholdery (<item>, <amount>, <price>, <balance>) a vrátí Component.
- /shop reload jako Brigadier podpříkaz s permission guishop.admin. Rozbitý YAML: nech starou
  konfiguraci a pošli odesílateli chybu.
- /shop zatím dál odpovídá zprávou, ale už z messages.yml.
Testy: validní config, každý typ chyby v itemu, reload s rozbitým souborem, /shop reload bez permission.
Žádné GUI ani Vault.
```

## Hotovo, když
- [ ] Modul 0: `runServer` naběhne, `/shop` odpoví ve hře, ekonomika na serveru funguje
- [ ] Všech 12 hran prošlo ručním testem na lokálním serveru
- [ ] `./gradlew build` zelený, logika obchodu a config pokryté testy
- [ ] README + komentovaný config + GIF (otevření menu, nákup, prodej)
- [ ] Veřejný název ověřený a změněný, repo přepnuté na veřejné, licence v repu
- [ ] Vydáno na GitHubu a Modrinthu

## Změny po review
Oponentní průchod vlastního návrhu, 2026-11-16:
- **Vyškrtnuto ze scope:** stránkování, výběr množství, `/sell`, permission na kategorii, log transakcí. Každé z toho je 2–3 h a termín nemá rezervu.
- **Doplněno:** modul 0. Bez ověřeného `runServer` a funkční ekonomiky na 26.3 by se chyba prostředí našla až ve 3. týdnu.
- **Změněno:** hledání ekonomiky až při použití místo v `onEnable` (pořadí načítání pluginů). Nákup jako „všechno, nebo nic“ místo částečného nákupu (míň hran).
- **Změněno:** `Wallet` místo přímého použití `Economy` v logice, aby testy nepotřebovaly Vault.
- **Opraveno podle researche:** `displayName` → `itemName`/`customName`; simulace kliknutí přes `PlayerSimulation`.
- **Přidáno:** pravidlo pro škrtání k 6. 12. a termín odeslání na Modrinth 9. 12.
- **Riziko, které zůstává:** hrana 10 potřebuje dva účty. Na stroji se 7,8 GB RAM druhý klient nejspíš nepoběží, je potřeba druhé zařízení nebo kamarád.
