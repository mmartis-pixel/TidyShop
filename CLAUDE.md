# guishop: pravidla pro každou session

## Stack
- Paper 26.3, Java 25, Gradle 9.9.1, MockBukkit 4.120.0, JUnit 5.
- Hlavní třída pluginu (`GuiShopPlugin`) nesmí být `final` (MockBukkit ji dědí).

## Postup
- Pracuj podle `SPEC.md`. Dělej vždy jen modul, který zadám, nic mimo jeho scope.
- Nejdřív testy logiky (JUnit/MockBukkit), pak implementace.
- Paper API ověřuj v javadocu (https://jd.papermc.io), nehádej z paměti.
  Ověřená a neověřená fakta:
  `C:\Users\mmart\OneDrive\Plocha\ultimate ai brain\03 Projects\Minecraft Commissions\Build-out\00 Pre-build context & examples\Research - Paper 26.3 API.md`
- Kliknutí v MockBukkitu simuluj přes `PlayerSimulation`, ne přes zastaralé metody na `PlayerMock`.
- Co MockBukkit neumí nasimulovat, netestuj oklikou. Dej to do herního checklistu.

## Stroj
- Pomalý stroj (Celeron, 7,8 GB RAM, build ~5 min): `./gradlew build` spusť jen jednou, na konci úkolu.

## Komunikace
- Odpovídej česky.
- Paper-specifické věci vysvětluj krátce, Javu ne.
- Na konci úkolu: shrnutí změn a checklist, co mám otestovat ve hře.
- Necommituj, dokud nepotvrdím test ve hře.
