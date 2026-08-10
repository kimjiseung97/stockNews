import { Link } from 'react-router-dom'
import styles from '@/assets/styles/pages/home/home.module.scss'
import mediaStyles from '@/assets/styles/pages/home/homeMedia.module.scss'

const startSteps = [
  {
    number: '01',
    title: '종목 검색해요',
    description: '기업명이나 티커를 입력해 원하는 미국 주식을 찾습니다.',
  },
  {
    number: '02',
    title: '관심 목록에 담아요',
    description: '계속 확인하고 싶은 종목만 골라 목록에 추가합니다.',
  },
  {
    number: '03',
    title: '이메일로 받아봐요',
    description: '설정한 시간에 종목별 최신 뉴스를 정리해서 받아봅니다.',
  },
]

export default function StartGuide() {
  return (
    <section
      className={`${styles['home-page__guide']} ${mediaStyles['home-page__guide']}`}
      aria-labelledby="guideTitle"
    >
      <h2 id="guideTitle">3단계로 간단하게 시작해요.</h2>
      <p className={styles['home-page__guide-description']}>
        복잡한 설정 없이 관심 종목을 고르고 이메일로 받아보세요.
      </p>
      <ol
        className={`${styles['home-page__step-list']} ${mediaStyles['home-page__step-list']}`}
      >
        {startSteps.map((step) => (
          <li
            key={step.number}
            className={`${styles['home-page__step']} ${mediaStyles['home-page__step']}`}
          >
            <strong>{step.number}</strong>
            <h3>{step.title}</h3>
            <p>{step.description}</p>
          </li>
        ))}
      </ol>
      <aside
        className={`${styles['home-page__guide-callout']} ${mediaStyles['home-page__guide-callout']}`}
      >
        <p>
          <strong>검색하고 담아두면 끝.</strong> 이후에는 StockNews가 새로운 소식을 정리해 전달합니다.
        </p>
        <Link to="/stock-search">관심 종목 등록하기 →</Link>
      </aside>
    </section>
  )
}
