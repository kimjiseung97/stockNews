import { useState, type FormEvent } from 'react'
import { Eye, EyeOff } from 'lucide-react'
import { apiFetch, ApiError } from '@/lib/api'
import styles from '@/assets/styles/pages/login/changePasswordModal.module.scss'
import warningIcon from '@/assets/images/icons/x.png'

interface ChangePasswordModalProps {
  initialCurrentPassword: string
  onChanged: () => void
}

// 임시 비밀번호로 로그인한 계정에 강제로 띄우는, 닫을 수 없는 비밀번호 변경 팝업
function ChangePasswordModal({ initialCurrentPassword, onChanged }: ChangePasswordModalProps) {
  const [currentPassword, setCurrentPassword] = useState(initialCurrentPassword)
  const [newPassword, setNewPassword] = useState('')
  const [newPasswordConfirm, setNewPasswordConfirm] = useState('')
  const [isPasswordVisible, setIsPasswordVisible] = useState(false)
  const [warningMessage, setWarningMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    if (newPassword.length < 8 || newPassword.length > 20) {
      setWarningMessage('새 비밀번호는 8~20자로 입력해 주세요.')
      return
    }

    if (newPassword !== newPasswordConfirm) {
      setWarningMessage('새 비밀번호가 일치하지 않습니다.')
      return
    }

    setWarningMessage('')
    setIsSubmitting(true)

    try {
      await apiFetch('/auth/password', {
        method: 'PUT',
        body: JSON.stringify({ currentPassword, newPassword }),
      })
      onChanged()
    } catch (error) {
      setWarningMessage(error instanceof ApiError ? error.message : '비밀번호 변경에 실패했습니다.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className={styles['change-password-modal']} role="dialog" aria-modal="true">
      <article className={styles['change-password-modal__card']}>
        <h2>비밀번호 변경이 필요합니다</h2>
        <p>임시 비밀번호로 로그인하셨습니다. 계속하려면 새 비밀번호로 변경해 주세요.</p>

        {warningMessage && (
          <p className={styles['change-password-modal__notice']} role="alert">
            <img src={warningIcon} alt=""></img>
            {warningMessage}
          </p>
        )}

        <form className={styles['change-password-modal__form']} onSubmit={handleSubmit}>
          <label className={styles['change-password-modal__field']}>
            <span>임시 비밀번호</span>
            <span className={styles['change-password-modal__input-box']}>
              <input
                type={isPasswordVisible ? 'text' : 'password'}
                name="currentPassword"
                value={currentPassword}
                autoComplete="current-password"
                minLength={8}
                maxLength={20}
                onChange={(event) => setCurrentPassword(event.target.value)}
                required
              />
              <button
                type="button"
                className={styles['change-password-modal__password-toggle']}
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

          <label className={styles['change-password-modal__field']}>
            <span>새 비밀번호</span>
            <span className={styles['change-password-modal__input-box']}>
              <input
                type={isPasswordVisible ? 'text' : 'password'}
                name="newPassword"
                value={newPassword}
                placeholder="8~20자로 입력하세요"
                autoComplete="new-password"
                minLength={8}
                maxLength={20}
                onChange={(event) => setNewPassword(event.target.value)}
                required
              />
            </span>
          </label>

          <label className={styles['change-password-modal__field']}>
            <span>새 비밀번호 확인</span>
            <span className={styles['change-password-modal__input-box']}>
              <input
                type={isPasswordVisible ? 'text' : 'password'}
                name="newPasswordConfirm"
                value={newPasswordConfirm}
                placeholder="비밀번호를 다시 입력하세요"
                autoComplete="new-password"
                minLength={8}
                maxLength={20}
                onChange={(event) => setNewPasswordConfirm(event.target.value)}
                required
              />
            </span>
          </label>

          <button type="submit" className={styles['change-password-modal__submit']} disabled={isSubmitting}>
            비밀번호 변경
          </button>
        </form>
      </article>
    </div>
  )
}

export default ChangePasswordModal
