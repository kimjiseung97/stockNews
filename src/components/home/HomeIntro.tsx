import { Link } from 'react-router-dom'
import styles from '@/assets/styles/pages/home/home.module.scss'
import mediaStyles from '@/assets/styles/pages/home/homeMedia.module.scss'

export default function HomeIntro() {
  return (
    <section className={`${styles['home-page__intro']} ${mediaStyles['home-page__intro']}`}>
      <p className={styles['home-page__eyebrow']}>StockNews 소개</p>
      <h1>
        뉴스를 찾는 시간은 줄이고
        <br />
        중요한 변화에 집중하세요.
      </h1>
      <p className={styles['home-page__intro-description']}>
        관심 종목을 등록하면 종목별 최신 뉴스를 한곳에서 확인하고 원하는 시간에 이메일로
        받아볼 수 있습니다.
      </p>
      <nav
        className={`${styles['home-page__intro-actions']} ${mediaStyles['home-page__intro-actions']}`}
        aria-label="StockNews 시작 메뉴"
      >
        <Link className={styles['home-page__primary-link']} to="/stock-search">
          관심 종목 등록
        </Link>
        <Link className={styles['home-page__secondary-link']} to="/email-settings">
          이메일 예시
        </Link>
      </nav>
    </section>
  )
}
