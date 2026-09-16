import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import phoneMockImage from '@/assets/images/phoneMock.png'
import { useScrollContainer } from '@/contexts/ScrollContainerContext'
import styles from '@/assets/styles/pages/home/home.module.scss'
import mediaStyles from '@/assets/styles/pages/home/homeMedia.module.scss'

gsap.registerPlugin(ScrollTrigger)

interface NewsPreviewItem {
  ticker: string
  headline: string
  dateTime: string
  displayDate: string
}

const newsPreviewItems: NewsPreviewItem[] = [
  {
    ticker: 'NVDA',
    headline: "[속보] 엔비디아·마이크론·하이닉스ADR...'급제동' 걸린 반도체주",
    dateTime: '2026-09-15T09:00',
    displayDate: '2026.09.15. 오전 9:00',
  },
  {
    ticker: 'AAPL',
    headline: '[특징주] 애플 첫 폴더블폰 공개...비에이치 수혜 기대감에 6% 상승',
    dateTime: '2026-09-15T09:00',
    displayDate: '2026.09.15. 오전 9:00',
  },
  {
    ticker: 'NVDA',
    headline: '출시 4년 된 엔비디아 H100 몸값 역주행...한달새 대여료 22% 급등',
    dateTime: '2026-09-15T09:00',
    displayDate: '2026.09.15. 오전 9:00',
  },
]

const CARD_INACTIVE_VARS = { x: 0, scale: 1, opacity: 0.3 }
const CARD_ACTIVE_VARS = { x: -8, scale: 1.06, opacity: 1 }

const ACTIVE_CLASS = styles['home-page__news-card--active']

export default function NewsPreviewStack() {
  const trackRef = useRef<HTMLDivElement>(null)
  const cardRefs = useRef<(HTMLLIElement | null)[]>([])
  const scrollContainerRef = useScrollContainer()
  const [isMobile, setIsMobile] = useState(() =>
    window.matchMedia('(max-width: 1024px)').matches,
  )

  useEffect(() => {
    const mobileQuery = window.matchMedia('(max-width: 1024px)')
    const handleMobileChange = (event: MediaQueryListEvent) => setIsMobile(event.matches)

    mobileQuery.addEventListener('change', handleMobileChange)

    return () => mobileQuery.removeEventListener('change', handleMobileChange)
  }, [])

  useLayoutEffect(() => {
    const cards = cardRefs.current

    if (isMobile) {
      cards.forEach((card) => card?.classList.remove(ACTIVE_CLASS))

      const mobileCtx = gsap.context(() => {
        cards.forEach((card, index) => {
          if (!card) return

          gsap.fromTo(
            card,
            {
              x: index % 2 === 0 ? -64 : 64,
              opacity: 0,
            },
            {
              x: 0,
              opacity: 1,
              duration: 0.8,
              ease: 'power2.out',
              scrollTrigger: {
                trigger: card,
                scroller: scrollContainerRef?.current ?? undefined,
                start: 'top 88%',
                toggleActions: 'play none none reverse',
              },
            },
          )
        })
      })

      return () => mobileCtx.revert()
    }

    const headerHeight = document.querySelector('header')?.getBoundingClientRect().height ?? 0

    const ctx = gsap.context(() => {
      gsap.set(cards, CARD_INACTIVE_VARS)
      gsap.set(cards[0], CARD_ACTIVE_VARS)
      cards[0]?.classList.add(ACTIVE_CLASS)

      const timeline = gsap.timeline({
        scrollTrigger: {
          trigger: trackRef.current,
          scroller: scrollContainerRef?.current ?? undefined,
          start: `top ${headerHeight}`,
          end: () => `+=${(newsPreviewItems.length - 1) * 280 + 220}`,
          pin: true,
          pinType: 'fixed',
          anticipatePin: 1,
          scrub: 0.75,
        },

        onUpdate: function () {
          const activeIndex = Math.min(newsPreviewItems.length - 1, Math.round(this.time()))
          cards.forEach((card, i) => card?.classList.toggle(ACTIVE_CLASS, i === activeIndex))
        },
      })

      newsPreviewItems.forEach((_, index) => {
        if (index === 0) return
        timeline.to(cards[index - 1], { ...CARD_INACTIVE_VARS, duration: 1 }, index - 1)
        timeline.to(cards[index], { ...CARD_ACTIVE_VARS, duration: 1 }, index - 1)
      })

      timeline.to({}, { duration: 0.75 })

      const track = trackRef.current
      const scrollContainer = scrollContainerRef?.current

      const handlePinnedWheel = (event: WheelEvent) => {
        if (!timeline.scrollTrigger?.isActive || !scrollContainer) return

        event.preventDefault()
        scrollContainer.scrollTop += event.deltaY
      }

      track?.addEventListener('wheel', handlePinnedWheel, { passive: false })

      return () => {
        track?.removeEventListener('wheel', handlePinnedWheel)
      }
    })

    return () => ctx.revert()
  }, [isMobile, scrollContainerRef])

  return (
    <section
      className={`${styles['home-page__news-preview']} ${mediaStyles['home-page__news-preview']}`}
      aria-labelledby="newsPreviewTitle"
    >
      <div
        className={`${styles['home-page__news-preview__inner']} ${mediaStyles['home-page__news-preview__inner']}`}
        ref={trackRef}
      >
        <hgroup className={styles['home-page__news-preview-heading']}>
          <h2 id="newsPreviewTitle" className={styles['home-page__news-preview-title']}>
            필요한 뉴스가 보기 좋게 도착합니다.
          </h2>
          <p className={styles['home-page__eyebrow']}>이메일 뉴스 알림</p>
        </hgroup>
        <p
          className={`${styles['home-page__news-preview-description']} ${mediaStyles['home-page__news-preview-description']}`}
        >
          관심 종목에서 새로 나온 주요 소식을 한 번에 확인하고, 원문이 궁금한 뉴스만 골라서
          확인하세요.
        </p>
        <div
          className={`${styles['home-page__phone-mock']} ${mediaStyles['home-page__phone-mock']}`}
        >
          {!isMobile && (
            <img
              src={phoneMockImage}
              alt=""
              aria-hidden="true"
              className={styles['home-page__phone-mock-image']}
            ></img>
          )}
          <div
            className={`${styles['home-page__news-stack']} ${mediaStyles['home-page__news-stack']}`}
          >
            <h3 className={styles['home-page__news-stack-title']}>
              오늘 확인하면 좋은 관심 종목 뉴스
            </h3>
            <ul className={styles['home-page__news-list']}>
              {newsPreviewItems.map((item, index) => (
                <li
                  key={item.headline}
                  ref={(el) => {
                    cardRefs.current[index] = el
                  }}
                  className={`${styles['home-page__news-card']} ${mediaStyles['home-page__news-card']}`}
                >
                  <article className={styles['home-page__news-article']}>
                    <div className={styles['home-page__news-title-group']}>
                      <h4 className={styles['home-page__news-title']}>{item.headline}</h4>
                      <strong className={styles['home-page__news-ticker']}>{item.ticker}</strong>
                    </div>
                    <time className={styles['home-page__news-date']} dateTime={item.dateTime}>
                      {item.displayDate}
                    </time>
                    <span className={styles['home-page__news-link']}>기사 보기 →</span>
                  </article>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </div>
    </section>
  )
}
