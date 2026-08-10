import styles from '@/assets/styles/common/listSkeleton.module.scss'

interface ListSkeletonProps {
  count?: number
  label?: string
}

export default function ListSkeleton({
  count = 6,
  label = '목록을 불러오는 중입니다.',
}: ListSkeletonProps) {
  return (
    <ul className={styles['list-skeleton']} aria-label={label} aria-busy="true">
      {Array.from({ length: count }, (_, index) => (
        <li className={styles['list-skeleton__item']} key={index}>
          <span className={styles['list-skeleton__leading']} />
          <span className={styles['list-skeleton__content']}>
            <i />
            <i />
          </span>
          <span className={styles['list-skeleton__trailing']} />
        </li>
      ))}
    </ul>
  )
}
