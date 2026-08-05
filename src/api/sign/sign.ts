// 회원가입 API
import { apiFetch } from '@/api/common/commonApi'

interface signUp {
  email: string
  password: string
  recoveryEmail: string
}

interface SignUpResponse {
  isMailSendSuccess: boolean
}

export async function signUp(data: signUp): Promise<void> {
  try {
    const response = await apiFetch<SignUpResponse>('/auth/signup', {
      method: 'POST',
      body: JSON.stringify(data),
    })
    console.log('보낸데이터', response)
  } catch (e) {
    console.log('에러', e)
    throw e
  }
}
