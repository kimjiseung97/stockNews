import emailIcon from '@/assets/images/icons/email.png'
import listIcon from '@/assets/images/icons/list.png'
import newsIcon from '@/assets/images/icons/news.png'
import searchIcon from '@/assets/images/icons/search.png'
import styles from '@/assets/styles/pages/home/home.module.scss'
import mediaStyles from '@/assets/styles/pages/home/homeMedia.module.scss'

const serviceFeatures = [
  {
    icon: searchIcon,
    title: '관심 종목 검색',
    description: '기업명 또는 티커를 입력해 원하는 미국 주식 종목을 빠르게 찾아보세요.',
  },
  {
    icon: emailIcon,
    title: '이메일 뉴스 알림',
    description: '종목별로 정리된 최신 뉴스를 원하는 시간에 이메일로 편리하게 받아보세요.',
  },
  {
    icon: newsIcon,
    title: '종목별 뉴스 확인',
    description: '관심 종목마다 수집된 뉴스를 구분해서 확인하고 중요한 흐름을 놓치지 마세요.',
  },
  {
    icon: listIcon,
    title: '관심 목록 관리',
    description: '자주 확인하는 종목만 따로 등록하고 필요하지 않은 종목은 언제든지 정리할 수 있습니다.',
  },
]

export default function ServiceFeatures() {
  return (
    <section
      className={`${styles['home-page__features']} ${mediaStyles['home-page__features']}`}
      aria-labelledby="featureTitle"
    >
      <h2 id="featureTitle" className={styles['home-page__section-guide']}>
        반복해서 뉴스를 찾는 과정을 더 간단하게 만들었습니다.
      </h2>
      <ul
        className={`${styles['home-page__feature-list']} ${mediaStyles['home-page__feature-list']}`}
      >
        {serviceFeatures.map((feature) => (
          <li
            key={feature.title}
            className={`${styles['home-page__feature-card']} ${mediaStyles['home-page__feature-card']}`}
          >
            <img src={feature.icon} alt="" aria-hidden="true" />
            <h3>{feature.title}</h3>
            <p>{feature.description}</p>
          </li>
        ))}
      </ul>
    </section>
  )
}
