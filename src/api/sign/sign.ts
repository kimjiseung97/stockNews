// 회원가입 API 요청
import { apiFetch } from '@/api/common/commonApi'

interface signUp {
  email: string
  password: string
  recoveryEmail: string
}

export async function signUp(data: signUp): Promise<void> {
  try {
    console.log('보낸 데이터', data)

    const response = await apiFetch('/auth/signup/complete', {
      method: 'POST',
      body: JSON.stringify(data),
    })

    console.log('회원가입 성공 응답', response)
  } catch (e) {
    console.log('에러', e)
    throw e
  }
}
