import { useEffect, useState, type FormEvent } from 'react'
import { ArrowLeft, Check, Eye, EyeOff, Mail } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError } from '@/api/common/commonApi'
import { requestResetPassword } from '@/api/infoFind/findPw'
import styles from '@/assets/styles/pages/forgot-password/forgotPassword.module.scss'
import mediaStyles from '@/assets/styles/pages/forgot-password/forgotPasswordMedia.module.scss'
import completeIcon from '@/assets/images/icons/complete.png'
import warningIcon from '@/assets/images/icons/x.png'

function FindPasswordPage() {
  const navigate = useNavigate()
  const [step, setStep] = useState(1)
  const [email, setEmail] = useState('')
  const [verificationCode, setVerificationCode] = useState('')
  const [password, setPassword] = useState('')
  const [passwordConfirm, setPasswordConfirm] = useState('')
  const [isPasswordVisible, setIsPasswordVisible] = useState(false)
  const [warningMessage, setWarningMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const focusInput = (form: HTMLFormElement, name: string) => {
    const input = form.elements.namedItem(name)

    if (input instanceof HTMLInputElement) {
      input.focus()
    }
  }

  const stepLabels = ['이메일 입력', '코드 인증', '비밀번호 재설정']

  // 다른 탭에서 돌아오면 입력 정보 초기화
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.visibilityState !== 'visible') {
        return
      }

      setStep(1)
      setEmail('')
      setVerificationCode('')
      setPassword('')
      setPasswordConfirm('')
      setIsPasswordVisible(false)
      setWarningMessage('')
    }

    document.addEventListener('visibilitychange', handleVisibilityChange)

    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange)
    }
  }, [])

  // 이메일 인증 단계로 이동
  const handleEmailSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    const trimmedEmail = email.trim()

    if (!trimmedEmail) {
      alert('이메일을 입력해 주세요.')
      focusInput(event.currentTarget, 'email')
      setWarningMessage('이메일을 입력해 주세요.')
      return
    }

    if (!emailPattern.test(trimmedEmail)) {
      setWarningMessage('올바른 이메일 형식으로 입력해 주세요.')
      return
    }

    setIsSubmitting(true)

    try {
      await requestResetPassword({ email: trimmedEmail })
      setEmail(trimmedEmail)
      setWarningMessage('')
      setStep(2)
    } catch (error) {
      setWarningMessage(
        error instanceof ApiError ? error.message : '인증 코드 발송에 실패했습니다.',
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  // 새 비밀번호 입력 단계로 이동
  const handleCodeSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    if (!verificationCode) {
      alert('인증 코드를 입력해 주세요.')
      focusInput(event.currentTarget, 'verificationCode')
      return
    }

    if (verificationCode.length !== 6) {
      setWarningMessage('인증 코드는 숫자 6자리로 입력해 주세요.')
      return
    }

    setWarningMessage('')
    setStep(3)
  }

  // 비밀번호 변경 완료
  const handlePasswordSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    if (!password) {
      alert('새 비밀번호를 입력해 주세요.')
      focusInput(event.currentTarget, 'password')
      return
    }

    if (password.length < 8 || password.length > 20) {
      setWarningMessage('비밀번호는 8~20자로 입력해 주세요.')
      return
    }

    if (!passwordConfirm) {
      alert('새 비밀번호 확인을 입력해 주세요.')
      focusInput(event.currentTarget, 'passwordConfirm')
      setWarningMessage('새 비밀번호를 한 번 더 입력해 주세요.')
      return
    }

    if (password !== passwordConfirm) {
      setWarningMessage('비밀번호가 일치하지 않습니다.')
      return
    }

    setWarningMessage('')
    navigate('/login')
  }

  return (
    <main
      id="forgotPasswordPage"
      className={`${styles['forgot-password-page']} ${mediaStyles['forgot-password-page']}`}
    >
      <article className={styles['forgot-password-page__card']}>
        <Link className={styles['forgot-password-page__back']} to="/login">
          <ArrowLeft aria-hidden="true"></ArrowLeft>
          로그인으로 돌아가기
        </Link>

        <section className={styles['forgot-password-page__heading']}>
          <h1>비밀번호 찾기</h1>
          <p>가입한 이메일로 인증 후 비밀번호를 재설정합니다.</p>
        </section>

        <section
          className={styles['forgot-password-page__progress']}
          aria-label="비밀번호 찾기 진행 단계"
        >
          <ol>
            {[1, 2, 3].map((progressStep) => (
              <li
                key={progressStep}
                className={[
                  progressStep <= step ? styles['forgot-password-page__progress-active'] : '',
                  progressStep < step ? styles['forgot-password-page__progress-complete'] : '',
                  progressStep === step ? styles['forgot-password-page__progress-current'] : '',
                ]
                  .filter(Boolean)
                  .join(' ')}
                aria-current={progressStep === step ? 'step' : undefined}
              >
                <span>
                  {progressStep < step ? <Check aria-hidden="true"></Check> : progressStep}
                </span>
                <p>{stepLabels[progressStep - 1]}</p>
              </li>
            ))}
          </ol>
        </section>

        {warningMessage ? (
          <p
            className={`${styles['forgot-password-page__notice']} ${styles['forgot-password-page__notice-warning']}`}
            role="alert"
          >
            <img src={warningIcon} alt=""></img>
            {warningMessage}
          </p>
        ) : (
          step > 1 && (
            <p className={styles['forgot-password-page__notice']} role="status">
              <img src={completeIcon} alt=""></img>
              {step === 2 ? (
                <>
                  <strong>{email}</strong>로 인증 코드를 발송했습니다.
                </>
              ) : (
                '이메일 인증이 완료되었습니다.'
              )}
            </p>
          )
        )}

        {step === 1 && (
          <form
            className={styles['forgot-password-page__form']}
            onSubmit={handleEmailSubmit}
            noValidate
          >
            <label className={styles['forgot-password-page__field']}>
              <span>가입한 이메일</span>
              <span className={styles['forgot-password-page__input-box']}>
                <input
                  type="email"
                  name="email"
                  value={email}
                  placeholder="example@email.com"
                  autoComplete="email"
                  maxLength={50}
                  onChange={(event) => {
                    setEmail(event.target.value)
                    setWarningMessage('')
                  }}
                  required
                />
                <Mail aria-hidden="true"></Mail>
              </span>
            </label>

            <button
              type="submit"
              className={styles['forgot-password-page__submit']}
              disabled={isSubmitting}
            >
              {isSubmitting ? '발송 중...' : '인증 코드 발송'}
            </button>
          </form>
        )}

        {step === 2 && (
          <>
            <form
              className={styles['forgot-password-page__form']}
              onSubmit={handleCodeSubmit}
              noValidate
            >
              <label className={styles['forgot-password-page__field']}>
                <span>인증 코드</span>
                <span className={styles['forgot-password-page__input-box']}>
                  <input
                    type="number"
                    name="verificationCode"
                    value={verificationCode}
                    placeholder="6자리 코드 입력"
                    inputMode="numeric"
                    onChange={(event) => {
                      setVerificationCode(event.target.value.replace(/[^0-9]/g, '').slice(0, 6))
                      setWarningMessage('')
                    }}
                    required
                  />
                </span>
                <small>이메일에서 받은 코드를 입력하세요.</small>
              </label>

              <button type="submit" className={styles['forgot-password-page__submit']}>
                인증 확인
              </button>
              <button
                type="button"
                className={styles['forgot-password-page__text-button']}
                onClick={() => setStep(1)}
              >
                이메일 다시 입력하기
              </button>
            </form>
          </>
        )}

        {step === 3 && (
          <>
            <form
              className={styles['forgot-password-page__form']}
              onSubmit={handlePasswordSubmit}
              noValidate
            >
              <label className={styles['forgot-password-page__field']}>
                <span>새 비밀번호</span>
                <span className={styles['forgot-password-page__input-box']}>
                  <input
                    type={isPasswordVisible ? 'text' : 'password'}
                    name="password"
                    value={password}
                    placeholder="8~20자로 입력하세요"
                    minLength={8}
                    maxLength={20}
                    autoComplete="new-password"
                    onChange={(event) => {
                      setPassword(event.target.value)
                      setWarningMessage('')
                    }}
                    required
                  />
                  <button
                    type="button"
                    className={styles['forgot-password-page__password-toggle']}
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

              <label className={styles['forgot-password-page__field']}>
                <span>새 비밀번호 확인</span>
                <span className={styles['forgot-password-page__input-box']}>
                  <input
                    type="password"
                    name="passwordConfirm"
                    value={passwordConfirm}
                    placeholder="비밀번호를 다시 입력하세요"
                    minLength={8}
                    maxLength={20}
                    autoComplete="new-password"
                    onChange={(event) => {
                      setPasswordConfirm(event.target.value)
                      setWarningMessage('')
                    }}
                    required
                  />
                </span>
              </label>

              <button type="submit" className={styles['forgot-password-page__submit']}>
                비밀번호 변경 완료
              </button>
            </form>
          </>
        )}
      </article>
    </main>
  )
}

export default FindPasswordPage
