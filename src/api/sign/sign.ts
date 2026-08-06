// 회원가입 API
import { apiFetch } from '@/api/common/commonApi'

interface signUp {
  email: string
  password: string
  recoveryEmail: string
}

export async function signUp(data: signUp): Promise<void> {
  try {
    await apiFetch('/auth/signup/complete', {
      method: 'POST',
      body: JSON.stringify(data),
    })
  } catch (e) {
    console.log('에러', e)
    throw e
  }
}
