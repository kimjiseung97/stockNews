import styles from '@/assets/styles/common/skeletonLoading.module.scss'

interface SkeletonLoadingProps {
  variant?: 'default' | 'tall'
  label?: string
}

export default function SkeletonLoading({
  variant = 'default',
  label = '화면을 불러오는 중입니다.',
}: SkeletonLoadingProps) {
  const fieldCount = variant === 'tall' ? 5 : 3

  return (
    <section
      className={`${styles['skeleton-loading']} ${styles[`skeleton-loading--${variant}`]}`}
      role="status"
      aria-live="polite"
      aria-label={label}
    >
      <span className={styles['skeleton-loading__back']}></span>
      <span className={styles['skeleton-loading__title']}></span>
      <span className={styles['skeleton-loading__description']}></span>

      <div className={styles['skeleton-loading__fields']}>
        {Array.from({ length: fieldCount }, (_, index) => (
          <div className={styles['skeleton-loading__field']} key={index}>
            <span></span>
            <span></span>
          </div>
        ))}
      </div>

      <span className={styles['skeleton-loading__button']}></span>
      <span className={styles['skeleton-loading__sr-only']}>{label}</span>
    </section>
  )
}
