import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'

const USER_EMAIL_KEY = 'stockNews.userEmail'

interface AuthContextValue {
  email: string | null
  setLoggedInUser: (email: string) => void
  clearLoggedInUser: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [email, setEmail] = useState<string | null>(() => localStorage.getItem(USER_EMAIL_KEY))

  const value = useMemo<AuthContextValue>(
    () => ({
      email,
      setLoggedInUser: (userEmail) => {
        localStorage.setItem(USER_EMAIL_KEY, userEmail)
        setEmail(userEmail)
      },
      clearLoggedInUser: () => {
        localStorage.removeItem(USER_EMAIL_KEY)
        setEmail(null)
      },
    }),
    [email],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)

  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }

  return context
}
