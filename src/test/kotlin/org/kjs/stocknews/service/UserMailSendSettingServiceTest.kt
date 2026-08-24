package org.kjs.stocknews.service

import org.junit.jupiter.api.Test
import org.kjs.stocknews.model.table.UserMailSendSetting
import org.kjs.stocknews.repository.UserMailSendSettingRepository
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class UserMailSendSettingServiceTest {
    private val userMailSendSettingRepository = mock(UserMailSendSettingRepository::class.java)
    private val userMailSendSettingService = UserMailSendSettingService(userMailSendSettingRepository)

    @Test
    fun `설정 row가 없으면 조회 시 기본값 true를 반환한다`() {
        `when`(userMailSendSettingRepository.findByUserId(1L)).thenReturn(null)

        val response = userMailSendSettingService.get(1L)

        assert(response.mailEnabled)
    }

    @Test
    fun `설정 row가 있으면 조회 시 저장된 값을 반환한다`() {
        `when`(userMailSendSettingRepository.findByUserId(1L)).thenReturn(UserMailSendSetting(userId = 1L, mailEnabled = false))

        val response = userMailSendSettingService.get(1L)

        assert(!response.mailEnabled)
    }

    @Test
    fun `설정 row가 없는 유저가 등록하면 새 row를 저장한다`() {
        `when`(userMailSendSettingRepository.findByUserId(1L)).thenReturn(null)
        `when`(userMailSendSettingRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer { it.arguments[0] }

        val response = userMailSendSettingService.register(1L, false)

        assert(!response.mailEnabled)
        org.mockito.Mockito.verify(userMailSendSettingRepository).save(org.mockito.ArgumentMatchers.any())
    }

    @Test
    fun `설정 row가 있는 유저가 다시 등록하면 기존 row 값을 변경한다`() {
        val existing = UserMailSendSetting(userId = 1L, mailEnabled = true)
        `when`(userMailSendSettingRepository.findByUserId(1L)).thenReturn(existing)

        val response = userMailSendSettingService.register(1L, false)

        assert(!response.mailEnabled)
        assert(!existing.mailEnabled)
        org.mockito.Mockito.verify(userMailSendSettingRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any())
    }
}
