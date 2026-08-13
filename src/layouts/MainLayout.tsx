import { useRef } from 'react'
import { Outlet } from 'react-router-dom'
import Header from '@/components/fixedContents/header/Header'
import LeftContents from '@/components/fixedContents/leftContents/LeftContents'
import useScrollToTop from '@/hooks/useScrollToTop'
import styles from '@/assets/styles/layout/main/mainLayout.module.scss'

export default function MainLayout() {
  const scrollContainerRef = useRef<HTMLElement>(null)

  useScrollToTop(scrollContainerRef)

  return (
    <section id="mainLayout" className={styles['main-layout']}>
      <aside className={styles['main-layout__left']}>
        <LeftContents
          eyebrow="미국 주식 뉴스 이메일 서비스"
          headline={
            <>
              관심 종목의 <br /> 중요한 뉴스만 확인하세요.
            </>
          }
          description={
            <>
              기업명이나 티커를 검색해 관심 종목을 등록하면 최신 뉴스를 정리해 <br /> 이메일로
              보내드립니다.
            </>
          }
        ></LeftContents>
      </aside>

      <section ref={scrollContainerRef} className={styles['main-layout__right']}>
        <Header></Header>

        <section className={styles['main-layout__content']}>
          <Outlet></Outlet>
        </section>
      </section>
    </section>
  )
}
