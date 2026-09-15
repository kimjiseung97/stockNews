import styles from '@/assets/styles/pages/home/home.module.scss'

export default function EmailPreview() {
  return (
    <section className={styles['home-page__email-preview']} aria-labelledby="emailPreviewTitle">
      <div className={styles['home-page__email-preview__inner']}>
        <hgroup className={styles['home-page__email-preview-heading']}>
          <h2 id="emailPreviewTitle">
            필요한 뉴스가
            <br></br>
            보기 좋게 도착합니다.
          </h2>
          <p className={styles['home-page__eyebrow']}>이메일 뉴스 알림</p>
        </hgroup>
        <p className={styles['home-page__email-description']}>
          관심 종목에서 새로 나온 주요 소식을 한 번에 확인하고, 원문이 궁금한 뉴스만 골라서
          확인하세요.
        </p>
        <article className={styles['home-page__mail']} aria-label="StockNews 이메일 예시">
          <p className={styles['home-page__mail-header']}>
            <strong>StockNews</strong>
            <time dateTime="2026-09-15">2026.09.15</time>
          </p>
          <p className={styles['home-page__mail-kicker']}>관심 종목 데일리 뉴스 다이제스트</p>
          <h3>오늘의 관심종목 뉴스</h3>
          <p className={styles['home-page__mail-summary']}>구독 중인 2개 종목의 최신 소식이에요.</p>
          <ul className={styles['home-page__mail-news']}>
            <li>
              <strong>[속보] 엔비디아·하이닉스ADR·마이크론 등 '와르르'…급제동 걸린 반도</strong>
              <span>기사 보러가기 →</span>
            </li>
            <li>
              <strong>[특징주] 애플 첫 폴더블폰 공개…비에이치 수혜 기대감에 6% 상승</strong>
              <span>기사 보러가기 →</span>
            </li>
          </ul>
          <aside
            aria-label="엔비디아 뉴스 예시"
            className={`${styles['home-page__stock-note']} ${styles['home-page__stock-note--nvidia']}`}
          >
            <strong>NVDA</strong>
            <p>AI 반도체 공급 확대 계획 발표</p>
            <span>Reuters · 18분 전</span>
          </aside>
          <aside
            className={`${styles['home-page__stock-note']} ${styles['home-page__stock-note--tesla']}`}
            aria-label="테슬라 뉴스 예시"
          >
            <strong>TSLA</strong>
            <p>신규 생산 라인과 인도량 전망</p>
            <span>Bloomberg · 1시간 전</span>
          </aside>
        </article>
      </div>
    </section>
  )
}
