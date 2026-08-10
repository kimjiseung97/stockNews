import { useEffect, useState, type FormEvent } from 'react'
import { ArrowLeft, Mail } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError } from '@/api/common/commonApi'
import { requestResetPassword } from '@/api/infoFind/findId'
import styles from '@/assets/styles/pages/find-email/findEmail.module.scss'
import mediaStyles from '@/assets/styles/pages/find-email/findEmailMedia.module.scss'
import completeIcon from '@/assets/images/icons/complete.png'
import warningIcon from '@/assets/images/icons/x.png'
import { useStableLoading } from '@/hooks/useStableLoading'
import LoadingSpinner from '@/components/common/LoadingSpinner'

function FindEmailPage() {
  const navigate = useNavigate()
  const [step, setStep] = useState(1)
  const [recoveryEmail, setRecoveryEmail] = useState('')
  const [verificationCode, setVerificationCode] = useState('')
  const [warningMessage, setWarningMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const showSubmitting = useStableLoading(isSubmitting)

  const focusInput = (form: HTMLFormElement, name: string) => {
    const input = form.elements.namedItem(name)

    if (input instanceof HTMLInputElement) {
      input.focus()
    }
  }

  // 다른 탭에서 돌아오면 입력 정보 초기화
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.visibilityState !== 'visible') {
        return
      }

      setStep(1)
      setRecoveryEmail('')
      setVerificationCode('')
      setWarningMessage('')
    }

    document.addEventListener('visibilitychange', handleVisibilityChange)

    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange)
    }
  }, [])

  // 복구 이메일 인증 단계로 이동
  const handleEmailSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    const trimmedEmail = recoveryEmail.trim()

    if (!trimmedEmail) {
      alert('복구 이메일을 입력해 주세요.')
      focusInput(event.currentTarget, 'recoveryEmail')
      setWarningMessage('복구용 이메일을 입력해 주세요.')
      return
    }

    if (!emailPattern.test(trimmedEmail)) {
      setWarningMessage('올바른 이메일 형식으로 입력해 주세요.')
      return
    }

    setIsSubmitting(true)

    try {
      await requestResetPassword({ recoveryEmail: trimmedEmail })
      setRecoveryEmail(trimmedEmail)
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

  // 아이디 확인 단계로 이동
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

  // 아이디 일부를 가려서 표시
  const getMaskedEmail = () => {
    const [emailId, emailDomain] = recoveryEmail.split('@')

    if (!emailId || !emailDomain) {
      return recoveryEmail
    }

    return `${emailId.slice(0, 4)}***@${emailDomain}`
  }

  return (
    <main
      id="findEmailPage"
      className={`${styles['find-email-page']} ${mediaStyles['find-email-page']}`}
    >
      <article className={styles['find-email-page__card']}>
        <Link className={styles['find-email-page__back']} to="/login">
          <ArrowLeft aria-hidden="true"></ArrowLeft>
          로그인으로 돌아가기
        </Link>

        <section className={styles['find-email-page__heading']}>
          <h1>{step === 3 ? '아이디 찾기 완료' : '아이디 찾기'}</h1>
          <p>
            {step === 3
              ? '가입 시 등록한 복구용 이메일로 인증이 완료되었습니다.'
              : '가입 시 등록한 복구용 이메일로 인증합니다.'}
          </p>
        </section>

        {warningMessage ? (
          <p
            className={`${styles['find-email-page__notice']} ${styles['find-email-page__notice-warning']}`}
            role="alert"
          >
            <img src={warningIcon} alt=""></img>
            {warningMessage}
          </p>
        ) : (
          step > 1 && (
            <p className={styles['find-email-page__notice']} role="status">
              <img src={completeIcon} alt=""></img>
              {step === 2 ? (
                <>
                  <strong>{recoveryEmail}</strong>로 인증 코드를 발송했습니다.
                </>
              ) : (
                '복구용 이메일 인증이 완료되었습니다.'
              )}
            </p>
          )
        )}

        {step === 1 && (
          <form className={styles['find-email-page__form']} onSubmit={handleEmailSubmit} noValidate>
            <label className={styles['find-email-page__field']}>
              <span>복구용 이메일</span>
              <span className={styles['find-email-page__input-box']}>
                <input
                  id="find-email-recovery-email"
                  type="email"
                  name="recoveryEmail"
                  value={recoveryEmail}
                  placeholder="복구용 이메일 주소"
                  autoComplete="email"
                  maxLength={50}
                  onChange={(event) => {
                    setRecoveryEmail(event.target.value)
                    setWarningMessage('')
                  }}
                  required
                />
                <Mail aria-hidden="true"></Mail>
              </span>
              <small>회원가입 시 입력한 복구용 이메일을 입력하세요.</small>
            </label>

            <button
              type="submit"
              className={styles['find-email-page__submit']}
              disabled={isSubmitting}
            >
              {showSubmitting ? <LoadingSpinner label="발송 중" /> : '인증 코드 발송'}
            </button>
          </form>
        )}

        {step === 2 && (
          <>
            <form
              className={styles['find-email-page__form']}
              onSubmit={handleCodeSubmit}
              noValidate
            >
              <label className={styles['find-email-page__field']}>
                <span>인증 코드</span>
                <span className={styles['find-email-page__input-box']}>
                  <input
                    id="find-email-verification-code"
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

              <button type="submit" className={styles['find-email-page__submit']}>
                아이디 확인
              </button>
              <button
                type="button"
                className={styles['find-email-page__text-button']}
                onClick={() => setStep(1)}
              >
                이메일 다시 입력하기
              </button>
            </form>
          </>
        )}

        {step === 3 && (
          <>
            <dl className={styles['find-email-page__result']}>
              <dt>가입된 아이디(이메일)</dt>
              <dd>{getMaskedEmail()}</dd>
            </dl>

            <button
              type="button"
              className={styles['find-email-page__submit']}
              onClick={() => navigate('/login')}
            >
              로그인
            </button>
          </>
        )}

        <section className={styles['find-email-page__divider']} aria-hidden="true">
          <span></span>
          <em>또는</em>
          <span></span>
        </section>

        <Link className={styles['find-email-page__forgot-password']} to="/find-password">
          비밀번호 찾기
        </Link>
      </article>
    </main>
  )
}

export default FindEmailPage
