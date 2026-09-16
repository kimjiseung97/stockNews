import { useLayoutEffect, useRef } from 'react'
import { Link } from 'react-router-dom'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import { useScrollContainer } from '@/contexts/ScrollContainerContext'
import styles from '@/assets/styles/pages/home/home.module.scss'

gsap.registerPlugin(ScrollTrigger)

export default function HomeCta() {
  const ctaRef = useRef<HTMLElement>(null)
  const scrollContainerRef = useScrollContainer()

  useLayoutEffect(() => {
    const motion = gsap.matchMedia()

    motion.add('(prefers-reduced-motion: no-preference)', () => {
      gsap.fromTo(
        ctaRef.current,
        { y: 48, opacity: 0 },
        {
          y: 0,
          opacity: 1,
          duration: 0.9,
          ease: 'power2.out',
          scrollTrigger: {
            trigger: ctaRef.current,
            scroller: scrollContainerRef?.current ?? undefined,
            start: 'top 92%',
            toggleActions: 'play none none reverse',
          },
        },
      )
    })

    return () => motion.revert()
  }, [scrollContainerRef])

  return (
    <section className={styles['home-page__cta']} ref={ctaRef}>
      <h2>관심 종목의 중요한 변화를 놓치지 마세요.</h2>
      <p>
        지금 종목을 등록하고 필요한 뉴스만 편리하게 받아보세요. 언제든지 관심 종목과 이메일 설정을
        변경할 수 있습니다.
      </p>
      <Link to="/stock-search">무료로 시작하기</Link>
    </section>
  )
}
