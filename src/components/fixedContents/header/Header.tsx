import styles from '@/assets/styles/fixedContents/header/header.module.scss'
import { Link, useNavigate } from 'react-router-dom'
import Menu from './menu/Menu'
import { logout } from '@/api/login/login'
import { useAuth } from '@/contexts/AuthContext'
import logo from '@/assets/images/logo.svg'

export default function Header() {
  const { email, clearLoggedInUser } = useAuth()
  const navigate = useNavigate()

  const handleLogout = async () => {
    // 화면은 즉시 한 번만 갱신하고 세션 종료는 별도로 처리
    clearLoggedInUser()

    try {
      await logout()
      navigate('/', { replace: true })
    } catch (error) {
      console.error('로그아웃 요청 실패', error)
    }
  }

  return (
    <header id="headerContainer" className={styles['header-container']}>
      <div className={styles['header-container__top']}>
        <Link
          className={styles['header-container__brand']}
          to="/"
          aria-label="StockNews 홈으로 이동"
        >
          <img src={logo} alt="로고" />
        </Link>

        <ul className={styles['header-container__account']}>
          {email ? (
            <>
              <li className={styles['header-container__email']}>{email}</li>
              <li>
                <button
                  className={styles['header-container__logout']}
                  type="button"
                  onClick={handleLogout}
                >
                  로그아웃
                </button>
              </li>
            </>
          ) : (
            <>
              <li>
                <Link className={styles['header-container__login']} to="/login">
                  로그인
                </Link>
              </li>
              <li>
                <Link className={styles['header-container__sign-up']} to="/sign-up">
                  시작하기
                </Link>
              </li>
            </>
          )}
        </ul>
      </div>

      <Menu></Menu>
    </header>
  )
}
