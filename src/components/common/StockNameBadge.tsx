import styles from '@/assets/styles/common/stockNameBadge.module.scss'
import mediaStyles from '@/assets/styles/common/stockNameBadgeMedia.module.scss'

interface StockNameBadgeProps {
  ticker: string
  displayName: string
  secondaryName?: string | null
  theme?: string | null
  themeClassName?: string
}

export default function StockNameBadge({
  ticker,
  displayName,
  secondaryName,
  theme,
  themeClassName,
}: StockNameBadgeProps) {
  return (
    <>
      <span className={styles['stock-name-badge__ticker']}>{ticker}</span>
      <span
        className={`${styles['stock-name-badge__names']} ${mediaStyles['stock-name-badge__names']}`}
      >
        <strong>{displayName}</strong>
        {secondaryName && <small title={secondaryName}>{secondaryName}</small>}
      </span>
      {theme && (
        <span
          className={`${styles['stock-name-badge__theme']} ${mediaStyles['stock-name-badge__theme']} ${themeClassName ?? ''}`}
          title={theme}
        >
          {theme}
        </span>
      )}
    </>
  )
}
