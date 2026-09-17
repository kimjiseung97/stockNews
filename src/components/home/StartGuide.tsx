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
      <div className={styles['home-page__guide-heading']}>
        <h2
          id="guideTitle"
          className={`${styles['home-page__guide-title']} ${mediaStyles['home-page__guide-title']}`}
        >
          3단계로 간단하게 시작해요.
        </h2>
        <p className={styles['home-page__guide-description']}>
          복잡한 설정 없이 관심 종목을 고르고 이메일로 받아보세요.
        </p>
      </div>
      <ol
        className={`${styles['home-page__step-list']} ${mediaStyles['home-page__step-list']}`}
      >
        {startSteps.map((step) => (
          <li
            key={step.number}
            className={`${styles['home-page__step']} ${mediaStyles['home-page__step']}`}
          >
            <article
              className={`${styles['home-page__step-article']} ${mediaStyles['home-page__step-article']}`}
            >
              <div
                className={`${styles['home-page__step-title-group']} ${mediaStyles['home-page__step-title-group']}`}
              >
                <h3
                  className={`${styles['home-page__step-title']} ${mediaStyles['home-page__step-title']}`}
                >
                  {step.title}
                </h3>
                <strong
                  className={`${styles['home-page__step-number']} ${mediaStyles['home-page__step-number']}`}
                >
                  {step.number}
                </strong>
              </div>
              <p
                className={`${styles['home-page__step-text']} ${mediaStyles['home-page__step-text']}`}
              >
                {step.description}
              </p>
            </article>
          </li>
        ))}
      </ol>
    </section>
  )
}
