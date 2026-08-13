import { useLayoutEffect, type RefObject } from 'react'
import { useLocation } from 'react-router-dom'

export default function useScrollToTop(scrollContainerRef?: RefObject<HTMLElement | null>) {
  const { pathname } = useLocation()

  useLayoutEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: 'auto' })
    scrollContainerRef?.current?.scrollTo({ top: 0, left: 0, behavior: 'auto' })
  }, [pathname, scrollContainerRef])
}
