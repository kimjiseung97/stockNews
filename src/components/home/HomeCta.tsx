import { Link } from 'react-router-dom'
import styles from '@/assets/styles/pages/home/home.module.scss'
import mediaStyles from '@/assets/styles/pages/home/homeMedia.module.scss'

export default function HomeCta() {
  return (
    <section className={`${styles['home-page__cta']} ${mediaStyles['home-page__cta']}`}>
      <h2>관심 종목의 중요한 변화를 놓치지 마세요.</h2>
      <p>
        지금 종목을 등록하고 필요한 뉴스만 편리하게 받아보세요. 언제든지 관심 종목과 이메일
        설정을 변경할 수 있습니다.
      </p>
      <Link to="/watchlist">무료로 시작하기</Link>
    </section>
  )
}
