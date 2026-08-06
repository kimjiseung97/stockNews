// 로그인
import { apiFetch } from '@/api/common/commonApi'

interface login {
  email: string
  password: string
}

export async function login(data: login): Promise<void> {
  try {
    console.log('보낸 데이터', data)

    const response = await apiFetch('/auth/login', {
      method: 'POST',
      body: JSON.stringify(data),
    })

    console.log('로그인 성공 응답', response)
  } catch (e) {
    console.log('로그인 에러', e)
    throw e
  }
}
