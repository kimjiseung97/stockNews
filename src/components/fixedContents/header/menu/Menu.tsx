import { NavLink } from 'react-router-dom'
import styles from '@/assets/styles/fixedContents/header/menu.module.scss'
import { useAuth } from '@/contexts/AuthContext'

export default function Menu() {
  const { email } = useAuth()

  return (
    <nav className={styles['header-menu']} aria-label="주요 메뉴">
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
                className={({ isActive }) => (isActive ? styles['header-menu__active'] : undefined)}
                to="/watchlist"
                end
              >
                내 관심종목
              </NavLink>
            </li>
            <li>
              <NavLink
                className={({ isActive }) => (isActive ? styles['header-menu__active'] : undefined)}
                to="/watchlist/register"
              >
                관심종목 추가·관리
              </NavLink>
            </li>
          </>
        ) : (
          <li>
            <NavLink
              className={({ isActive }) => (isActive ? styles['header-menu__active'] : undefined)}
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
        {email && (
          <li>
            <NavLink
              className={({ isActive }) => (isActive ? styles['header-menu__active'] : undefined)}
              to="/email-settings"
            >
              이메일 설정
            </NavLink>
          </li>
        )}
      </ul>
    </nav>
  )
}
