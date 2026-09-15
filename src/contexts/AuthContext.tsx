import { useCallback, useEffect, useRef, type ReactNode } from 'react'
import { logout } from '@/api/login/login'
import { useAuthStore } from '@/store/authStore'

const USER_EMAIL_KEY = 'stockNews.userEmail'
const SESSION_EXPIRES_AT_KEY = 'stockNews.sessionExpiresAt'
const SESSION_DURATION = 30 * 60 * 1000

export function AuthProvider({ children }: { children: ReactNode }) {
  const email = useAuthStore((state) => state.email)
  const setEmail = useAuthStore((state) => state.setEmail)
  const isSessionExpiring = useRef(false)

  // 로그인 후 30분이 지나면 세션 종료
  useEffect(() => {
    if (!email || isSessionExpiring.current) {
      return
    }

    let expiresAt = Number(localStorage.getItem(SESSION_EXPIRES_AT_KEY))
    const currentSessionExpiresAt = Date.now() + SESSION_DURATION

    if (!expiresAt || expiresAt > currentSessionExpiresAt) {
      expiresAt = currentSessionExpiresAt
      localStorage.setItem(SESSION_EXPIRES_AT_KEY, String(expiresAt))
    }

    const handleSessionExpired = async () => {
      if (isSessionExpiring.current) {
        return
      }

      isSessionExpiring.current = true
      localStorage.removeItem(USER_EMAIL_KEY)
      localStorage.removeItem(SESSION_EXPIRES_AT_KEY)
      setEmail(null)

      window.alert('로그인 세션이 만료되었습니다. 다시 로그인해 주세요.')

      try {
        await logout()
      } catch (error) {
        console.error('세션 만료 로그아웃 요청 실패', error)
      } finally {
        window.location.replace('/login')
      }
    }

    const checkSessionExpiration = () => {
      if (Date.now() >= expiresAt) {
        void handleSessionExpired()
      }
    }

    checkSessionExpiration()

    const sessionTimer = window.setInterval(checkSessionExpiration, 1000)
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        checkSessionExpiration()
      }
    }

    document.addEventListener('visibilitychange', handleVisibilityChange)

    return () => {
      window.clearInterval(sessionTimer)
      document.removeEventListener('visibilitychange', handleVisibilityChange)
    }
  }, [email, setEmail])

  return <>{children}</>
}

export function useAuth() {
  const email = useAuthStore((state) => state.email)
  const setEmail = useAuthStore((state) => state.setEmail)

  const setLoggedInUser = useCallback(
    (userEmail: string) => {
      localStorage.setItem(USER_EMAIL_KEY, userEmail)
      localStorage.setItem(SESSION_EXPIRES_AT_KEY, String(Date.now() + SESSION_DURATION))
      setEmail(userEmail)
    },
    [setEmail],
  )

  const clearLoggedInUser = useCallback(() => {
    localStorage.removeItem(USER_EMAIL_KEY)
    localStorage.removeItem(SESSION_EXPIRES_AT_KEY)
    setEmail(null)
  }, [setEmail])

  return { email, setLoggedInUser, clearLoggedInUser }
}
