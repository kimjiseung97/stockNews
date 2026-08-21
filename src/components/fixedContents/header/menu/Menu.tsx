import { useRef, type MouseEvent, type PointerEvent } from 'react'
import { NavLink } from 'react-router-dom'
import styles from '@/assets/styles/fixedContents/header/menu.module.scss'
import { useAuth } from '@/contexts/AuthContext'

export default function Menu() {
  const { email } = useAuth()
  const menuRef = useRef<HTMLElement>(null)
  const isDragging = useRef(false)
  const hasMoved = useRef(false)
  const dragStartX = useRef(0)
  const dragStartScrollLeft = useRef(0)

  function handlePointerDown(event: PointerEvent<HTMLElement>) {
    if (event.pointerType === 'mouse' && event.button !== 0) {
      return
    }

    const menu = menuRef.current

    if (!menu) {
      return
    }

    isDragging.current = true
    hasMoved.current = false
    dragStartX.current = event.clientX
    dragStartScrollLeft.current = menu.scrollLeft
  }

  function handlePointerMove(event: PointerEvent<HTMLElement>) {
    const menu = menuRef.current

    if (!menu || !isDragging.current) {
      return
    }

    const movedDistance = event.clientX - dragStartX.current

    if (Math.abs(movedDistance) > 5) {
      hasMoved.current = true

      if (!menu.hasPointerCapture(event.pointerId)) {
        menu.setPointerCapture(event.pointerId)
      }
    }

    menu.scrollLeft = dragStartScrollLeft.current - movedDistance
  }

  function handlePointerEnd(event: PointerEvent<HTMLElement>) {
    const menu = menuRef.current

    isDragging.current = false

    if (menu?.hasPointerCapture(event.pointerId)) {
      menu.releasePointerCapture(event.pointerId)
    }

    window.setTimeout(() => {
      hasMoved.current = false
    }, 0)
  }

  function handleClick(event: MouseEvent<HTMLElement>) {
    if (!hasMoved.current) {
      return
    }

    event.preventDefault()
    event.stopPropagation()
  }

  return (
    <nav
      ref={menuRef}
      className={styles['header-menu']}
      aria-label="주요 메뉴"
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerEnd}
      onPointerCancel={handlePointerEnd}
      onClickCapture={handleClick}
    >
      <ul>
        <li>
          <NavLink
            className={({ isActive }) => (isActive ? styles['header-menu__active'] : undefined)}
            to="/"
            end
          >
            홈
          </NavLink>
        </li>
        {/* <li>
          <NavLink
            className={({ isActive }) => (isActive ? styles['header-menu__active'] : undefined)}
            to="/dashboard"
          >
            대시보드
          </NavLink>
        </li> */}
        {email ? (
          <>
            <li>
              <NavLink
                className={({ isActive }) =>
                  isActive ? styles['header-menu__active'] : undefined
                }
                to="/watchlist"
                end
              >
                내 관심종목
              </NavLink>
            </li>
            <li>
              <NavLink
                className={({ isActive }) =>
                  isActive ? styles['header-menu__active'] : undefined
                }
                to="/watchlist/register"
              >
                관심종목 추가·관리
              </NavLink>
            </li>
          </>
        ) : (
          <li>
            <NavLink
              className={({ isActive }) =>
                isActive ? styles['header-menu__active'] : undefined
              }
              to="/stock-search"
            >
              종목 검색
            </NavLink>
          </li>
        )}
        <li>
          <NavLink
            className={({ isActive }) => (isActive ? styles['header-menu__active'] : undefined)}
            to="/stock-news"
          >
            종목 뉴스
          </NavLink>
        </li>
        <li>
          <NavLink
            className={({ isActive }) => (isActive ? styles['header-menu__active'] : undefined)}
            to="/email-settings"
          >
            이메일 설정
          </NavLink>
        </li>
        {/* <li>
          <NavLink
            className={({ isActive }) => (isActive ? styles['header-menu__active'] : undefined)}
            to="/mail-example"
          >
            메일 예시
          </NavLink>
        </li> */}
        {/* 스와이프 동작 테스트용 */}
        {/* <li>
          <NavLink
            className={({ isActive }) => (isActive ? styles['header-menu__active'] : undefined)}
            to="/stock-search"
          >
            종목 검색
          </NavLink>
        </li>
        <li>
          <NavLink
            className={({ isActive }) => (isActive ? styles['header-menu__active'] : undefined)}
            to="/stock-news"
          >
            종목 뉴스
          </NavLink>
        </li>
        <li>
          <NavLink
            className={({ isActive }) => (isActive ? styles['header-menu__active'] : undefined)}
            to="/email-settings"
          >
            이메일 설정
          </NavLink>
        </li> */}
      </ul>
    </nav>
  )
}
