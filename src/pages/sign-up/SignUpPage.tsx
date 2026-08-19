import { useState, type FormEvent, type MouseEvent as ReactMouseEvent } from 'react'
import { Eye, EyeOff, Mail } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { signUp } from '@/api/sign/sign'
import { duplicateCheck } from '@/api/sign/duplicateCheck'
import { emailAuth } from '@/api/emailAuth/emailAuth'
import styles from '@/assets/styles/pages/sign-up/signUp.module.scss'
import { useStableLoading } from '@/hooks/useStableLoading'
import LoadingSpinner from '@/components/common/LoadingSpinner'

function SignUpPage() {
  const navigate = useNavigate()
  const [isPasswordVisible, setIsPasswordVisible] = useState(false)
  const [isPasswordConfirmVisible, setIsPasswordConfirmVisible] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const showSubmitting = useStableLoading(isSubmitting)
  const [isEmailDuplicateChecked, setIsEmailDuplicateChecked] = useState(false)
  const [isVerificationCodeVisible, setIsVerificationCodeVisible] = useState(false)
  const [isVerificationCodeSent, setIsVerificationCodeSent] = useState(false)
  const [isEmailVerified, setIsEmailVerified] = useState(false)
  const [verificationCode, setVerificationCode] = useState('')
  const [passwordValue, setPasswordValue] = useState('')
  const [passwordConfirmValue, setPasswordConfirmValue] = useState('')

  const isPasswordMismatch =
    passwordConfirmValue.length > 0 && passwordValue !== passwordConfirmValue

  const focusInput = (form: HTMLFormElement, name: string) => {
    const input = form.elements.namedItem(name)

    if (input instanceof HTMLInputElement) {
      input.focus()
    }
  }

  // 회원가입 폼 제출
  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    const form = event.currentTarget
    const formData = new FormData(form)
    const email = String(formData.get('email') ?? '').trim()
    const password = String(formData.get('password') ?? '').trim()
    const passwordConfirm = String(formData.get('passwordConfirm') ?? '').trim()
    const recoveryEmail = String(formData.get('recoveryEmail') ?? '').trim()

    if (!email) {
      alert('이메일을 입력해 주세요.')
      focusInput(form, 'email')
      return
    }

    if (!isEmailDuplicateChecked) {
      alert('이메일 중복확인을 완료해 주세요.')
      focusInput(form, 'email')
      return
    }

    if (!isEmailVerified) {
      alert('이메일 인증을 완료해 주세요.')
      focusInput(form, 'verificationCode')
      return
    }

    if (!password) {
      alert('비밀번호를 입력해 주세요.')
      focusInput(form, 'password')
      return
    }

    if (!passwordConfirm) {
      alert('비밀번호 확인을 입력해 주세요.')
      focusInput(form, 'passwordConfirm')
      return
    }

    if (password !== passwordConfirm) {
      focusInput(form, 'passwordConfirm')
      return
    }

    if (!recoveryEmail) {
      alert('복구 이메일을 입력해 주세요.')
      focusInput(form, 'recoveryEmail')
      return
    }

    setIsSubmitting(true)

    try {
      await signUp({ email, password, recoveryEmail })
      alert('회원가입이 완료되었습니다.')
      navigate('/login')
    } catch (error) {
      alert('회원가입에 실패했습니다.')
    } finally {
      setIsSubmitting(false)
    }
  }
  const handleDuplicateCheck = async (event: ReactMouseEvent<HTMLButtonElement>) => {
    if (isEmailDuplicateChecked) {
      return
    }

    const form = event.currentTarget.form

    if (!form) {
      return
    }

    const formData = new FormData(form)
    const email = String(formData.get('email') ?? '').trim()

    if (!email) {
      alert('이메일을 입력해주세요.')
      focusInput(form, 'email')
      return
    }

    try {
      const result = await duplicateCheck(email)

      if (result.duplicated) {
        alert('이미 가입된 이메일입니다.')
        return
      }

      if (!result.isMailSendSuccess) {
        alert('인증코드 메일 발송에 실패했습니다.')
        return
      }

      setIsEmailDuplicateChecked(true)
      setIsVerificationCodeVisible(true)
      setIsVerificationCodeSent(true)
      alert('사용 가능한 이메일입니다. 인증코드가 발송되었습니다.')
    } catch (e) {
      alert('이메일 중복 확인에 실패했습니다.')
    }
  }

  // 이메일 인증코드 확인
  const handleEmailAuth = async (event: ReactMouseEvent<HTMLButtonElement>) => {
    const form = event.currentTarget.form

    if (!form) {
      return
    }

    if (!verificationCode) {
      focusInput(form, 'verificationCode')
      return
    }

    if (!/^\d{6}$/.test(verificationCode)) {
      alert('인증코드 6자리를 숫자로 입력해 주세요.')
      focusInput(form, 'verificationCode')
      return
    }

    const formData = new FormData(form)
    const email = String(formData.get('email') ?? '').trim()

    try {
      await emailAuth({ email, code: verificationCode })
      setIsEmailVerified(true)
      alert('이메일 인증이 완료되었습니다.')
    } catch (e) {
      alert('인증코드가 일치하지 않거나 만료되었습니다.')
    }
  }

  const handleVerificationCodeChange = (value: string) => {
    const onlyNumbers = value.replace(/[^0-9]/g, '').slice(0, 6)
    setVerificationCode(onlyNumbers)
  }

  return (
    <main id="signUpPage" className={styles['sign-up-page']}>
      <article className={styles['sign-up-page__card']}>
        <section className={styles['sign-up-page__heading']}>
          <h1>회원가입</h1>
          <p>계정을 만들고 관심 종목의 뉴스를 받아보세요.</p>
        </section>

        <form className={styles['sign-up-page__form']} onSubmit={handleSubmit} noValidate>
          <label className={styles['sign-up-page__field']}>
            <span>이메일</span>
            <span className={styles['sign-up-page__email-row']}>
              <span className={styles['sign-up-page__input-box']}>
                <input
                  id="sign-up-email"
                  type="email"
                  name="email"
                  placeholder="example@email.com"
                  autoComplete="email"
                  maxLength={50}
                  readOnly={isEmailDuplicateChecked}
                  required
                />
                <Mail aria-hidden="true"></Mail>
              </span>
              <button
                type="button"
                tabIndex={-1}
                className={styles['sign-up-page__verify']}
                onClick={handleDuplicateCheck}
                aria-disabled={isEmailDuplicateChecked}
              >
                {isEmailDuplicateChecked ? '확인완료' : '중복확인'}
              </button>
            </span>
          </label>

          {isVerificationCodeVisible && (
            <label className={styles['sign-up-page__field']}>
              <span>인증코드</span>
              <span className={styles['sign-up-page__email-row']}>
                <span className={styles['sign-up-page__input-box']}>
                  <input
                    id="sign-up-verification-code"
                    type="number"
                    inputMode="numeric"
                    pattern="[0-9]*"
                    name="verificationCode"
                    value={verificationCode}
                    onChange={(event) => handleVerificationCodeChange(event.target.value)}
                    placeholder="인증코드 6자리"
                    autoComplete="one-time-code"
                    readOnly={isEmailVerified}
                    required
                  />
                </span>
                <button
                  type="button"
                  className={styles['sign-up-page__verify']}
                  onClick={handleEmailAuth}
                  aria-disabled={!isVerificationCodeSent || isEmailVerified}
                >
                  {isEmailVerified ? '인증완료' : '확인'}
                </button>
              </span>
            </label>
          )}

          <label className={styles['sign-up-page__field']}>
            <span>비밀번호</span>
            <span className={styles['sign-up-page__input-box']}>
              <input
                id="sign-up-password"
                type={isPasswordVisible ? 'text' : 'password'}
                name="password"
                value={passwordValue}
                onChange={(event) => setPasswordValue(event.target.value)}
                placeholder="비밀번호를 입력하세요"
                autoComplete="new-password"
                minLength={8}
                maxLength={20}
                required
              />
              <button
                type="button"
                tabIndex={-1}
                className={styles['sign-up-page__password-toggle']}
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
            <small>영문, 숫자, 특수문자 포함 8~20자</small>
          </label>

          <label className={styles['sign-up-page__field']}>
            <span>비밀번호 확인</span>
            <span className={styles['sign-up-page__input-box']}>
              <input
                id="sign-up-password-confirm"
                type={isPasswordConfirmVisible ? 'text' : 'password'}
                name="passwordConfirm"
                value={passwordConfirmValue}
                onChange={(event) => setPasswordConfirmValue(event.target.value)}
                placeholder="비밀번호를 다시 입력하세요"
                autoComplete="new-password"
                minLength={8}
                maxLength={20}
                required
              />
              <button
                type="button"
                tabIndex={-1}
                className={styles['sign-up-page__password-toggle']}
                onClick={() => setIsPasswordConfirmVisible(!isPasswordConfirmVisible)}
                aria-label={
                  isPasswordConfirmVisible ? '비밀번호 확인 숨기기' : '비밀번호 확인 보기'
                }
              >
                {isPasswordConfirmVisible ? (
                  <EyeOff aria-hidden="true"></EyeOff>
                ) : (
                  <Eye aria-hidden="true"></Eye>
                )}
              </button>
            </span>
            {isPasswordMismatch && (
              <small className={styles['sign-up-page__password-error']} role="alert">
                비밀번호가 일치하지 않습니다.
              </small>
            )}
          </label>

          <label className={styles['sign-up-page__field']}>
            <span>복구 이메일</span>
            <span className={styles['sign-up-page__input-box']}>
              <input
                id="sign-up-recovery-email"
                type="email"
                name="recoveryEmail"
                placeholder="비상 연락용 이메일"
                autoComplete="email"
                maxLength={50}
              />
              <Mail aria-hidden="true"></Mail>
            </span>
            <small>비밀번호 분실 시 복구 용도로 사용됩니다.</small>
          </label>

          <button
            type="submit"
            className={styles['sign-up-page__submit']}
            disabled={isSubmitting || (isVerificationCodeSent && !isEmailVerified)}
          >
            {showSubmitting
              ? <LoadingSpinner label="가입 중" />
              : isVerificationCodeSent && !isEmailVerified
                ? '인증 대기중'
                : '회원가입'}
          </button>
        </form>

        <section className={styles['sign-up-page__divider']} aria-hidden="true">
          <span></span>
          <em>또는</em>
          <span></span>
        </section>

        <Link className={styles['sign-up-page__login']} to="/login">
          로그인
        </Link>

        <p className={styles['sign-up-page__account-guide']}>
          이미 계정이 있으신가요?
          <Link to="/login">로그인하기</Link>
        </p>
      </article>
    </main>
  )
}

export default SignUpPage
