import { Suspense, useRef } from 'react'
import { Outlet } from 'react-router-dom'
import Header from '@/components/fixedContents/header/Header'
import LeftContents from '@/components/fixedContents/leftContents/LeftContents'
import useScrollToTop from '@/hooks/useScrollToTop'
import usePageAccessibility from '@/hooks/usePageAccessibility'
import ListSkeleton from '@/components/common/ListSkeleton'
import StockChat from '@/components/fixedContents/stockChat/StockChat'
import styles from '@/assets/styles/layout/main/mainLayout.module.scss'
import mediaStyles from '@/assets/styles/layout/main/mainLayoutMedia.module.scss'

export default function MainLayout() {
  const scrollContainerRef = useRef<HTMLDivElement>(null)

  useScrollToTop(scrollContainerRef)
  usePageAccessibility(scrollContainerRef)

  return (
    <div id="mainLayout" className={`${styles['main-layout']} ${mediaStyles['main-layout']}`}>
      <a className={styles['main-layout__skip-link']} href="#mainContent">
        본문 바로가기
      </a>
      <aside
        className={`${styles['main-layout__left']} ${mediaStyles['main-layout__left']}`}
        aria-label="서비스 소개 및 종목 검색"
      >
        <LeftContents
          eyebrow="미국 주식 뉴스 이메일 서비스"
          headline={
            <>
              관심 종목의 <br /> 중요한 뉴스만 확인하세요.
            </>
          }
          description={
            <>
              기업명이나 티커를 검색해 관심 종목을 등록하면 <br /> 최신 뉴스를 정리해 이메일로
              보내드립니다.
            </>
          }
        ></LeftContents>
      </aside>

      <div ref={scrollContainerRef} className={styles['main-layout__right']}>
        <Header></Header>

        <div id="mainContent" className={styles['main-layout__content']} tabIndex={-1}>
          <Suspense
            fallback={
              <main aria-label="본문 불러오는 중">
                <ListSkeleton count={6}></ListSkeleton>
              </main>
            }
          >
            <Outlet></Outlet>
          </Suspense>
        </div>

        <StockChat></StockChat>
      </div>
    </div>
  )
}
