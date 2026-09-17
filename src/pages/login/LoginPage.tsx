import { useState, type FormEvent } from 'react'
import { Eye, EyeOff, Mail } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError } from '@/api/common/commonApi'
import { login } from '@/api/login/login'
import styles from '@/assets/styles/pages/login/login.module.scss'
import warningIcon from '@/assets/images/icons/x.webp'
import { useAuth } from '@/contexts/AuthContext'
import { useStableLoading } from '@/hooks/useStableLoading'
import LoadingSpinner from '@/components/common/LoadingSpinner'

function LoginPage() {
  const navigate = useNavigate()
  const { setLoggedInUser } = useAuth()
  const [isPasswordVisible, setIsPasswordVisible] = useState(false)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [warningMessage, setWarningMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const showSubmitting = useStableLoading(isSubmitting)

  // 로그인 폼 제출
  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setWarningMessage('')
    setIsSubmitting(true)

    try {
      const user = await login({ email, password })
      setLoggedInUser(user.email)
      navigate('/')
    } catch (error) {
      setWarningMessage(error instanceof ApiError ? error.message : '로그인에 실패했습니다.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main id="loginPage" className={styles['login-page']}>
      <div className={styles['login-page__card']}>
        <hgroup className={styles['login-page__heading']}>
          <h1>로그인</h1>
        </hgroup>

        {warningMessage && (
          <p id="loginError" className={styles['login-page__notice']} role="alert">
            <img src={warningIcon} alt=""></img>
            {warningMessage}
          </p>
        )}

        <form className={styles['login-page__form']} onSubmit={handleSubmit}>
          <p className={styles['login-page__field']}>
            <label htmlFor="login-email">이메일</label>
            <span className={styles['login-page__input-box']}>
              <input
                id="login-email"
                aria-describedby={warningMessage ? 'loginError' : undefined}
                type="email"
                name="email"
                value={email}
                placeholder="example@email.com"
                autoComplete="email"
                maxLength={50}
                onChange={(event) => setEmail(event.target.value)}
                required
              />
              <Mail aria-hidden="true"></Mail>
            </span>
          </p>

          <p className={styles['login-page__field']}>
            <label htmlFor="login-password">비밀번호</label>
            <span className={styles['login-page__input-box']}>
              <input
                id="login-password"
                aria-describedby={warningMessage ? 'loginError' : undefined}
                type={isPasswordVisible ? 'text' : 'password'}
                name="password"
                value={password}
                placeholder="비밀번호를 입력하세요"
                autoComplete="current-password"
                minLength={8}
                maxLength={20}
                onChange={(event) => setPassword(event.target.value)}
                required
              />
              <button
                type="button"
                className={styles['login-page__password-toggle']}
                onClick={() => setIsPasswordVisible(!isPasswordVisible)}
                aria-label={isPasswordVisible ? '비밀번호 숨기기' : '비밀번호 보기'}
                aria-controls="login-password"
              >
                {isPasswordVisible ? (
                  <EyeOff aria-hidden="true"></EyeOff>
                ) : (
                  <Eye aria-hidden="true"></Eye>
                )}
              </button>
            </span>
          </p>

          <button
            type="button"
            className={styles['login-page__forgot-password']}
            onClick={() => navigate('/find-password')}
          >
            비밀번호를 잊으셨나요?
          </button>

          <button type="submit" className={styles['login-page__submit']} disabled={isSubmitting}>
            {showSubmitting ? <LoadingSpinner label="로그인 중"></LoadingSpinner> : '로그인'}
          </button>
        </form>

        <p className={styles['login-page__divider']} aria-hidden="true">
          <span></span>
          <em>또는</em>
          <span></span>
        </p>

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
      </div>
    </main>
  )
}

export default LoginPage
