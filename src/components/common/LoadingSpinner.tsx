import { LoaderCircle } from 'lucide-react'
import styles from '@/assets/styles/common/loadingSpinner.module.scss'

interface LoadingSpinnerProps {
  label?: string
}

export default function LoadingSpinner({ label = '처리 중' }: LoadingSpinnerProps) {
  return (
    <span className={styles['loading-spinner']} role="status">
      <LoaderCircle aria-hidden="true" />
      <span>{label}</span>
    </span>
  )
}
