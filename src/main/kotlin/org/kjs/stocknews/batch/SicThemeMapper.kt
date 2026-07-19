package org.kjs.stocknews.batch

import org.kjs.stocknews.model.table.StockTheme

// SEC SIC code ranges mapped to internal StockTheme. Narrower/exception ranges
// are listed before the broader ranges that contain them, since `when` picks
// the first matching branch. Source: SEC SIC code list (sec.gov/info/edgar/siccodes.htm).
object SicThemeMapper {
    fun themeForSic(sic: Int): StockTheme? = when (sic) {
        in 100..999 -> StockTheme.MATERIALS
        in 1000..1099 -> StockTheme.MATERIALS
        in 1200..1399 -> StockTheme.ENERGY
        in 1400..1499 -> StockTheme.MATERIALS
        in 1500..1799 -> StockTheme.INDUSTRIALS
        in 2000..2199 -> StockTheme.CONSUMER_STAPLES
        in 2200..2399 -> StockTheme.CONSUMER_DISCRETIONARY
        in 2400..2499 -> StockTheme.MATERIALS
        in 2500..2599 -> StockTheme.CONSUMER_DISCRETIONARY
        in 2600..2699 -> StockTheme.MATERIALS
        in 2700..2799 -> StockTheme.COMMUNICATION_SERVICES
        in 2830..2836 -> StockTheme.HEALTHCARE
        in 2800..2899 -> StockTheme.MATERIALS
        in 2900..2999 -> StockTheme.ENERGY
        in 3000..3099 -> StockTheme.MATERIALS
        in 3100..3199 -> StockTheme.CONSUMER_DISCRETIONARY
        in 3200..3299 -> StockTheme.MATERIALS
        in 3300..3399 -> StockTheme.MATERIALS
        in 3570..3579 -> StockTheme.IT
        in 3400..3499 -> StockTheme.INDUSTRIALS
        in 3500..3599 -> StockTheme.INDUSTRIALS
        in 3600..3699 -> StockTheme.IT
        3711 -> StockTheme.CONSUMER_DISCRETIONARY
        in 3700..3799 -> StockTheme.INDUSTRIALS
        in 3820..3849 -> StockTheme.HEALTHCARE
        in 3800..3899 -> StockTheme.INDUSTRIALS
        in 3900..3999 -> StockTheme.CONSUMER_DISCRETIONARY
        in 4600..4699 -> StockTheme.ENERGY
        in 4800..4899 -> StockTheme.COMMUNICATION_SERVICES
        in 4900..4999 -> StockTheme.UTILITIES
        in 4000..4799 -> StockTheme.INDUSTRIALS
        in 5000..5199 -> StockTheme.INDUSTRIALS
        5411 -> StockTheme.CONSUMER_STAPLES
        in 5200..5999 -> StockTheme.CONSUMER_DISCRETIONARY
        in 6000..6499 -> StockTheme.FINANCE
        6798 -> StockTheme.REAL_ESTATE
        in 6500..6599 -> StockTheme.REAL_ESTATE
        in 6600..6799 -> StockTheme.FINANCE
        in 7370..7379 -> StockTheme.IT
        in 7000..7099 -> StockTheme.CONSUMER_DISCRETIONARY
        in 7200..7299 -> StockTheme.CONSUMER_DISCRETIONARY
        in 7300..7399 -> StockTheme.INDUSTRIALS
        in 7400..7599 -> StockTheme.CONSUMER_DISCRETIONARY
        in 7800..7899 -> StockTheme.COMMUNICATION_SERVICES
        in 7900..7999 -> StockTheme.CONSUMER_DISCRETIONARY
        in 8000..8099 -> StockTheme.HEALTHCARE
        in 8200..8299 -> StockTheme.CONSUMER_DISCRETIONARY
        in 8700..8799 -> StockTheme.INDUSTRIALS
        else -> null
    }
}
