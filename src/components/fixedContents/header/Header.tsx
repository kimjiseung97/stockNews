import styles from '@/assets/styles/fixedContents/header/header.module.scss'
import { Link } from 'react-router-dom'
import Menu from './menu/Menu'

export default function Header() {
  return (
    <header id="headerContainer" className={styles['header-container']}>
      <div className={styles['header-container__top']}>
        <Link
          className={styles['header-container__brand']}
          to="/"
          aria-label="StockNews 홈으로 이동"
        >
          로고
        </Link>

        <ul className={styles['header-container__account']}>
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
        </ul>
      </div>

      <Menu></Menu>
    </header>
  )
}
