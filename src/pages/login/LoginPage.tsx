import { useState, type FormEvent } from 'react'
import { Eye, EyeOff, Mail } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import styles from '@/assets/styles/pages/login/login.module.scss'
import mediaStyles from '@/assets/styles/pages/login/loginMedia.module.scss'

function LoginPage() {
  const navigate = useNavigate()
  const [isPasswordVisible, setIsPasswordVisible] = useState(false)

  // 로그인 폼 제출
  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
  }

  return (
    <main id="loginPage" className={`${styles['login-page']} ${mediaStyles['login-page']}`}>
      <article className={`${styles['login-page__card']} ${mediaStyles['login-page__card']}`}>
        <section className={styles['login-page__heading']}>
          <h1>로그인</h1>
          <p>관심 종목의 뉴스를 지금 바로 확인하세요.</p>
        </section>

        <form className={styles['login-page__form']} onSubmit={handleSubmit}>
          <label className={styles['login-page__field']}>
            <span>이메일</span>
            <span className={styles['login-page__input-box']}>
              <input
                type="email"
                name="email"
                placeholder="example@email.com"
                autoComplete="email"
                maxLength={50}
                required
              />
              <Mail aria-hidden="true"></Mail>
            </span>
          </label>

          <label className={styles['login-page__field']}>
            <span>비밀번호</span>
            <span className={styles['login-page__input-box']}>
              <input
                type={isPasswordVisible ? 'text' : 'password'}
                name="password"
                placeholder="비밀번호를 입력하세요"
                autoComplete="current-password"
                minLength={8}
                maxLength={20}
                required
              />
              <button
                type="button"
                className={styles['login-page__password-toggle']}
                onClick={() => setIsPasswordVisible(!isPasswordVisible)}
                aria-label={isPasswordVisible ? '비밀번호 숨기기' : '비밀번호 보기'}
              >
                {isPasswordVisible ? (
                  <EyeOff aria-hidden="true"></EyeOff>
                ) : (
                  <Eye aria-hidden="true"></Eye>
                )}
              </button>
            </span>
          </label>

          <button
            type="button"
            className={styles['login-page__forgot-password']}
            onClick={() => navigate('/forgot-password')}
          >
            비밀번호를 잊으셨나요?
          </button>

          <button type="submit" className={styles['login-page__submit']}>
            로그인
          </button>
        </form>

        <section className={styles['login-page__divider']} aria-hidden="true">
          <span></span>
          <em>또는</em>
          <span></span>
        </section>

        <Link className={styles['login-page__sign-up']} to="/sign-up">
          회원가입
        </Link>

        <button
          type="button"
          className={styles['login-page__find-email']}
          onClick={() => navigate('/find-email')}
        >
          아이디(이메일) 찾기
          <span>여기</span>
        </button>
      </article>
    </main>
  )
}

export default LoginPage
