import { useLayoutEffect, useRef } from 'react'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import HomeCta from '@/components/home/HomeCta'
import HomeIntro from '@/components/home/HomeIntro'
import NewsPreviewStack from '@/components/home/NewsPreviewStack'
import ServiceFeatures from '@/components/home/ServiceFeatures'
import StartGuide from '@/components/home/StartGuide'
import { useScrollContainer } from '@/contexts/ScrollContainerContext'
import styles from '@/assets/styles/pages/home/home.module.scss'
import mediaStyles from '@/assets/styles/pages/home/homeMedia.module.scss'

gsap.registerPlugin(ScrollTrigger)

function HomePage() {
  const homePageRef = useRef<HTMLElement>(null)
  const scrollContainerRef = useScrollContainer()

  useLayoutEffect(() => {
    const homePage = homePageRef.current

    if (!homePage) return

    const motion = gsap.matchMedia()

    motion.add(
      {
        motionAllowed: '(prefers-reduced-motion: no-preference)',
        isNarrowMobile: '(max-width: 550px)',
      },
      (context) => {
        if (!context.conditions?.motionAllowed) return

        const isNarrowMobile = context.conditions.isNarrowMobile
        const sections = [
          {
            trigger: `.${styles['home-page__features']}`,
            heading: `.${styles['home-page__feature-heading']}`,
            items: `.${styles['home-page__feature-card']}`,
            itemDelay: 0.24,
            groupSize: isNarrowMobile ? 1 : 2,
            firstStart: 'top 78%',
            nextStart: 'top 68%',
            line: null,
          },
          {
            trigger: `.${styles['home-page__guide']}`,
            heading: `.${styles['home-page__guide-heading']}`,
            items: `.${styles['home-page__step']}`,
            itemDelay: 0.1,
            groupSize: 3,
            firstStart: 'top 76%',
            nextStart: 'top 68%',
            line: `.${styles['home-page__step-list']}`,
          },
        ]

        sections.forEach((section) => {
          const trigger = homePage.querySelector<HTMLElement>(section.trigger)
          const heading = homePage.querySelector<HTMLElement>(section.heading)
          const items = gsap.utils.toArray<HTMLElement>(section.items, homePage)
          const line = section.line ? homePage.querySelector<HTMLElement>(section.line) : null

          if (!trigger || !heading || items.length === 0) return

          const itemGroups = Array.from(
            { length: Math.ceil(items.length / section.groupSize) },
            (_, index) => items.slice(index * section.groupSize, (index + 1) * section.groupSize),
          )

          itemGroups.forEach((itemGroup, groupIndex) => {
            const timeline = gsap.timeline({
              scrollTrigger: {
                trigger: groupIndex === 0 ? trigger : itemGroup[0],
                scroller: scrollContainerRef?.current ?? undefined,
                start: groupIndex === 0 ? section.firstStart : section.nextStart,
                toggleActions: 'play none none reverse',
                invalidateOnRefresh: true,
              },
            })

            if (groupIndex === 0) {
              timeline.fromTo(
                heading,
                { y: 28, autoAlpha: 0 },
                { y: 0, autoAlpha: 1, duration: 0.72, ease: 'expo.out' },
              )
            }

            itemGroup.forEach((item, index) => {
              timeline.fromTo(
                item,
                { y: 42, autoAlpha: 0 },
                {
                  y: 0,
                  autoAlpha: 1,
                  duration: 0.82,
                  ease: 'expo.out',
                },
                index === 0 ? (groupIndex === 0 ? '-=0.42' : 0) : `-=${0.82 - section.itemDelay}`,
              )
            })

            if (groupIndex === itemGroups.length - 1 && line) {
              timeline.addLabel('stepLineStart', '-=0.2')

              timeline.fromTo(
                line,
                { '--step-line-progress': 0 },
                {
                  '--step-line-progress': 1,
                    duration: 0.45,
                  ease: 'power2.out',
                },
                'stepLineStart',
              )

              items.slice(0, -1).forEach((item, index) => {
                timeline.fromTo(
                  item,
                  { '--mobile-step-line-progress': 0 },
                  {
                    '--mobile-step-line-progress': 1,
                      duration: 0.225,
                    ease: 'power2.out',
                  },
                    `stepLineStart+=${index * 0.225}`,
                )
              })
            }
          })
        })
      },
    )

    return () => motion.revert()
  }, [scrollContainerRef])

  return (
    <main
      id="homePage"
      ref={homePageRef}
      className={`${styles['home-page']} ${mediaStyles['home-page']}`}
    >
      <HomeIntro></HomeIntro>
      <ServiceFeatures></ServiceFeatures>
      <StartGuide></StartGuide>
      <NewsPreviewStack></NewsPreviewStack>
      <HomeCta></HomeCta>
    </main>
  )
}

export default HomePage
