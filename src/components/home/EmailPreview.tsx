import { Link } from 'react-router-dom'
import styles from '@/assets/styles/pages/home/home.module.scss'

export default function EmailPreview() {
  return (
    <section className={styles['home-page__email-preview']} aria-labelledby="emailPreviewTitle">
      <p className={styles['home-page__eyebrow']}>이메일 뉴스 알림</p>
      <h2 id="emailPreviewTitle">
        필요한 뉴스가
        <br></br>
        보기 좋게 도착합니다.
      </h2>
      <p className={styles['home-page__email-description']}>
        관심 종목에서 새로 나온 주요 소식을 한 번에 확인하고, 원문이 궁금한 뉴스만 골라서
        확인하세요.
      </p>
      <Link className={styles['home-page__preview-link']} to="/email-settings">
        실제 메일 예시 보기 →
      </Link>

      <article className={styles['home-page__mail']} aria-label="StockNews 이메일 예시">
        <section className={styles['home-page__mail-header']}>
          <strong>StockNews</strong>
          <time dateTime="2026-07-28">2026.07.28</time>
        </section>
        <p className={styles['home-page__mail-kicker']}>관심 종목 뉴스 브리핑</p>
        <h3>오늘 확인하면 좋은 관심 종목 뉴스</h3>
        <p className={styles['home-page__mail-summary']}>
          등록한 종목에서 새로 나온 소식 3건을 정리했습니다.
        </p>
        <ul className={styles['home-page__mail-news']}>
          <li>
            <strong>차세대 AI 반도체 공급 확대 계획 발표</strong>
            <span>Reuters · 인공지능 · 반도체</span>
          </li>
          <li>
            <strong>새로운 운영체제와 AI 기능 공개</strong>
            <span>CNBC · 모바일 · 인공지능</span>
          </li>
        </ul>
        <aside
          className={`${styles['home-page__stock-note']} ${styles['home-page__stock-note--nvidia']}`}
        >
          <strong>NVDA</strong>
          <p>AI 반도체 공급 확대 계획 발표</p>
          <span>Reuters · 18분 전</span>
        </aside>
        <aside
          className={`${styles['home-page__stock-note']} ${styles['home-page__stock-note--tesla']}`}
        >
          <strong>TSLA</strong>
          <p>신규 생산 라인과 인도량 전망</p>
          <span>Bloomberg · 1시간 전</span>
        </aside>
      </article>
    </section>
  )
}
