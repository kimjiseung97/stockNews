import { useEffect, useRef, type RefObject } from 'react'
import { useLocation } from 'react-router-dom'

// 화면이 바뀌면 제목과 본문 초점을 갱신
export default function usePageAccessibility(containerRef: RefObject<HTMLElement | null>) {
  const { pathname } = useLocation()
  const previousPathname = useRef(pathname)

  useEffect(() => {
    const container = containerRef.current
    if (!container) return

    let shouldFocus = previousPathname.current !== pathname
    previousPathname.current = pathname

    const updatePage = () => {
      const main = container.querySelector('main')
      const heading = main?.querySelector('h1')
      if (!main || !heading || main.getClientRects().length === 0) return

      document.title = `${heading.textContent?.replace(/\s+/g, ' ').trim()} | StockNews`
      main.tabIndex = -1

      if (shouldFocus) {
        main.focus({ preventScroll: true })
        shouldFocus = false
      }
    }

    updatePage()
    const observer = new MutationObserver(updatePage)
    observer.observe(container, { childList: true, subtree: true, characterData: true })

    return () => observer.disconnect()
  }, [pathname, containerRef])
}
