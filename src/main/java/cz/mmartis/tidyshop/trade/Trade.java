package cz.mmartis.tidyshop.trade;

import java.math.BigDecimal;

/**
 * Outcome of a buy or sell, with the pieces and money involved (or that would have been),
 * so messages can say "sold 35 for $140". Sell results report 0 pieces when nothing was counted.
 */
public record Trade(TradeResult result, int amount, BigDecimal money) {
}
