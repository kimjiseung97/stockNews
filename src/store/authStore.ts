import { create } from 'zustand'

const USER_EMAIL_KEY = 'stockNews.userEmail'

interface AuthState {
  email: string | null
  setEmail: (email: string | null) => void
}

export const useAuthStore = create<AuthState>((set) => ({
  email: localStorage.getItem(USER_EMAIL_KEY),
  setEmail: (email) => set({ email }),
}))
