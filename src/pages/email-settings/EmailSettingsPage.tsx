import { useEffect, useState, type ChangeEvent } from 'react'
import { ChevronDown } from 'lucide-react'
import { ApiError } from '@/api/common/commonApi'
import { getMailSendSetting, registMailSendSetting } from '@/api/emailSettings/mailSendSetting'
import {
  getMailDispatchSetting,
  registMailDispatchSetting,
} from '@/api/emailSettings/mailDispatchSetting'
import styles from '@/assets/styles/pages/email-settings/emailSettings.module.scss'
import mediaStyles from '@/assets/styles/pages/email-settings/emailSettingsMedia.module.scss'
import completeIcon from '@/assets/images/icons/complete.png'
import warningIcon from '@/assets/images/icons/x.png'
import LoadingSpinner from '@/components/common/LoadingSpinner'

// 30분 단위 발송시간 선택지 (00:00 ~ 23:30, 총 48개)
const DISPATCH_TIME_OPTIONS = Array.from({ length: 48 }, (_, index) => {
  const hour = String(Math.floor(index / 2)).padStart(2, '0')
  const minute = index % 2 === 0 ? '00' : '30'
  const time = `${hour}:${minute}`
  return { value: time, label: time }
})

// 이메일 설정 목록 스켈레톤
function EmailSettingsSkeleton() {
  return (
    <ul
      className={`${styles['email-settings-page__list']} ${styles['email-settings-page__skeleton']}`}
      aria-label="이메일 설정을 불러오는 중"
      aria-busy="true"
    >
      <li
        className={`${styles['email-settings-page__item']} ${mediaStyles['email-settings-page__item']}`}
      >
        <span className={styles['email-settings-page__skeleton-text']}>
          <span></span>
          <span></span>
        </span>
        <span className={styles['email-settings-page__skeleton-toggle']}></span>
      </li>
      <li
        className={`${styles['email-settings-page__item']} ${mediaStyles['email-settings-page__item']}`}
      >
        <span className={styles['email-settings-page__skeleton-text']}>
          <span></span>
          <span></span>
        </span>
        <span className={styles['email-settings-page__skeleton-select']}></span>
      </li>
    </ul>
  )
}

function EmailSettingsPage() {
  const [mailEnabled, setMailEnabled] = useState(true)
  const [dispatchTime, setDispatchTime] = useState('')
  const [isSettingsLoaded, setIsSettingsLoaded] = useState(false)
  const [isTimeSelectOpen, setIsTimeSelectOpen] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [saveStatus, setSaveStatus] = useState<'success' | 'error' | ''>('')
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    const loadSettings = async () => {
      try {
        const [sendSetting, dispatchSetting] = await Promise.all([
          getMailSendSetting(),
          getMailDispatchSetting(),
        ])

        setMailEnabled(sendSetting.mailEnabled)
        setDispatchTime(dispatchSetting.dispatchTime.slice(0, 5))
      } catch (error) {
        setDispatchTime('09:00')
        setErrorMessage(
          error instanceof ApiError ? error.message : '이메일 설정을 불러오지 못했습니다.',
        )
      } finally {
        setIsSettingsLoaded(true)
      }
    }

    void loadSettings()
  }, [])

  // 뉴스 메일 받기 토글
  const handleToggleMailEnabled = (event: ChangeEvent<HTMLInputElement>) => {
    const nextMailEnabled = event.target.checked
    setMailEnabled(nextMailEnabled)
    setSaveStatus('')
  }

  // 메일 받을 시간 변경 (30분 단위만 선택 가능)
  const handleChangeDispatchTime = (event: ChangeEvent<HTMLSelectElement>) => {
    const nextDispatchTime = event.target.value
    setDispatchTime(nextDispatchTime)
    setIsTimeSelectOpen(false)
    setSaveStatus('')
  }

  // 이메일 설정 저장
  const handleSaveSettings = async () => {
    if (!isSettingsLoaded || !dispatchTime) {
      return
    }

    setIsSaving(true)
    setSaveStatus('')
    setErrorMessage('')

    try {
      const [sendSetting, dispatchSetting] = await Promise.all([
        registMailSendSetting(mailEnabled),
        registMailDispatchSetting(`${dispatchTime}:00`),
      ])

      setMailEnabled(sendSetting.mailEnabled)
      setDispatchTime(dispatchSetting.dispatchTime.slice(0, 5))
      setSaveStatus('success')
    } catch {
      setSaveStatus('error')
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <main
      id="emailSettingsPage"
      className={`${styles['email-settings-page']} ${mediaStyles['email-settings-page']}`}
    >
      <section className={styles['email-settings-page__heading']}>
        <p className={styles['email-settings-page__eyebrow']}>EMAIL SETTINGS</p>
        <h1>이메일 설정</h1>
        <p>뉴스 메일 수신 여부와 받을 시간을 설정하세요.</p>
      </section>

      {errorMessage && (
        <p className={styles['email-settings-page__notice']} role="alert">
          {errorMessage}
        </p>
      )}

      {!isSettingsLoaded ? (
        <EmailSettingsSkeleton></EmailSettingsSkeleton>
      ) : (
        <>
          <ul className={styles['email-settings-page__list']}>
            <li
              className={`${styles['email-settings-page__item']} ${mediaStyles['email-settings-page__item']}`}
            >
              <div className={styles['email-settings-page__item-text']}>
                <strong>뉴스 메일 받기</strong>
                <span>매일 관심종목 뉴스 를 이메일로 받습니다.</span>
              </div>
              <label className={styles['email-settings-page__toggle']}>
                <input
                  type="checkbox"
                  checked={mailEnabled}
                  disabled={!isSettingsLoaded || isSaving}
                  onChange={handleToggleMailEnabled}
                ></input>
                <span aria-hidden="true"></span>
                <em className={styles['email-settings-page__sr-only']}>
                  뉴스 메일 받기 {mailEnabled ? '켜짐' : '꺼짐'}
                </em>
              </label>
            </li>

            <li
              className={`${styles['email-settings-page__item']} ${mediaStyles['email-settings-page__item']}`}
            >
              <div className={styles['email-settings-page__item-text']}>
                <strong>메일 받을 시간</strong>
                <span>30분 단위로 받고 싶은 시간을 선택하세요.</span>
              </div>
              <label
                className={`${styles['email-settings-page__time-select']} ${mediaStyles['email-settings-page__time-select']}`}
              >
                <span className={styles['email-settings-page__sr-only']}>메일 받을 시간 선택</span>
                <select
                  value={dispatchTime}
                  disabled={!isSettingsLoaded || !mailEnabled || isSaving}
                  aria-busy={!isSettingsLoaded}
                  onPointerDown={() => setIsTimeSelectOpen((current) => !current)}
                  onKeyDown={(event) => {
                    if (['Enter', ' ', 'ArrowDown', 'ArrowUp'].includes(event.key)) {
                      setIsTimeSelectOpen(true)
                    }

                    if (event.key === 'Escape') {
                      setIsTimeSelectOpen(false)
                    }
                  }}
                  onBlur={() => setIsTimeSelectOpen(false)}
                  onChange={handleChangeDispatchTime}
                >
                  <option value="" disabled hidden>
                    --:--
                  </option>
                  {DISPATCH_TIME_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
                <ChevronDown
                  className={`${styles['email-settings-page__time-select-icon']} ${
                    isTimeSelectOpen ? styles['email-settings-page__time-select-icon-open'] : ''
                  }`}
                  aria-hidden="true"
                ></ChevronDown>
              </label>
            </li>
          </ul>

          <section className={styles['email-settings-page__save-area']}>
            {saveStatus && (
              <p
                className={`${styles['email-settings-page__save-notice']} ${
                  saveStatus === 'error' ? styles['email-settings-page__save-notice-error'] : ''
                }`}
                role={saveStatus === 'error' ? 'alert' : 'status'}
              >
                <img src={saveStatus === 'success' ? completeIcon : warningIcon} alt=""></img>
                {saveStatus === 'success'
                  ? '이메일 설정이 완료되었습니다.'
                  : '이메일 설정에 실패했습니다.'}
              </p>
            )}
            <button
              type="button"
              className={`${styles['email-settings-page__save-button']} ${mediaStyles['email-settings-page__save-button']}`}
              disabled={isSaving}
              onClick={handleSaveSettings}
            >
              {isSaving ? <LoadingSpinner label="저장 중"></LoadingSpinner> : '저장'}
            </button>
          </section>
        </>
      )}
    </main>
  )
}

export default EmailSettingsPage
