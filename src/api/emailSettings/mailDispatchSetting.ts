// 뉴스 메일 발송시간대 설정 API
import { apiFetch } from '@/api/common/commonApi'

export interface MailDispatchSetting {
  dispatchTime: string
}

// 메일 발송시간대 조회
export async function getMailDispatchSetting(): Promise<MailDispatchSetting> {
  try {
    const response = await apiFetch<MailDispatchSetting>('/users/me/mail-dispatch-setting')

    if (!response) {
      throw new Error('메일 발송시간대 응답이 없습니다.')
    }

    console.log('메일 발송시간대 조회 응답', response)
    return response
  } catch (error) {
    console.log('메일 발송시간대 조회 에러', error)
    throw error
  }
}

// 메일 발송시간대 등록/변경 (30분 단위만 허용, 예: '09:00', '09:30')
export async function registMailDispatchSetting(
  dispatchTime: string,
): Promise<MailDispatchSetting> {
  try {
    const response = await apiFetch<MailDispatchSetting>('/users/me/mail-dispatch-setting', {
      method: 'POST',
      body: JSON.stringify({ dispatchTime }),
    })

    if (!response) {
      throw new Error('메일 발송시간대 응답이 없습니다.')
    }

    console.log('메일 발송시간대 변경 응답', response)
    return response
  } catch (error) {
    console.log('메일 발송시간대 변경 에러', error)
    throw error
  }
}
