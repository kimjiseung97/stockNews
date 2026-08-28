// 뉴스 메일 발송 여부 설정 API
import { apiFetch } from '@/api/common/commonApi'

export interface MailSendSetting {
  mailEnabled: boolean
}

// 메일 발송 여부 조회
export async function getMailSendSetting(): Promise<MailSendSetting> {
  try {
    const response = await apiFetch<MailSendSetting>('/users/me/mail-send-setting')

    if (!response) {
      throw new Error('메일 발송 설정 응답이 없습니다.')
    }

    console.log('메일 발송 설정 조회 응답', response)
    return response
  } catch (error) {
    console.log('메일 발송 설정 조회 에러', error)
    throw error
  }
}

// 메일 발송 여부 등록/변경
export async function registMailSendSetting(mailEnabled: boolean): Promise<MailSendSetting> {
  try {
    const response = await apiFetch<MailSendSetting>('/users/me/mail-send-setting', {
      method: 'POST',
      body: JSON.stringify({ mailEnabled }),
    })

    if (!response) {
      throw new Error('메일 발송 설정 응답이 없습니다.')
    }

    console.log('메일 발송 설정 변경 응답', response)
    return response
  } catch (error) {
    console.log('메일 발송 설정 변경 에러', error)
    throw error
  }
}
