package org.kjs.stocknews.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.kjs.stocknews.common.BusinessException
import org.kjs.stocknews.common.ResultCode
import org.kjs.stocknews.model.table.UserMailDispatchSetting
import org.kjs.stocknews.repository.UserMailDispatchSettingRepository
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.LocalTime

class UserMailDispatchSettingServiceTest {
    private val userMailDispatchSettingRepository = mock(UserMailDispatchSettingRepository::class.java)
    private val userMailDispatchSettingService = UserMailDispatchSettingService(userMailDispatchSettingRepository)

    @Test
    fun `설정 row가 없으면 조회 시 기본값 09시를 반환한다`() {
        `when`(userMailDispatchSettingRepository.findByUserId(1L)).thenReturn(null)

        val response = userMailDispatchSettingService.get(1L)

        assert(response.dispatchTime == LocalTime.of(9, 0))
    }

    @Test
    fun `설정 row가 있으면 조회 시 저장된 값을 반환한다`() {
        `when`(userMailDispatchSettingRepository.findByUserId(1L))
            .thenReturn(UserMailDispatchSetting(userId = 1L, dispatchTime = LocalTime.of(14, 30)))

        val response = userMailDispatchSettingService.get(1L)

        assert(response.dispatchTime == LocalTime.of(14, 30))
    }

    @Test
    fun `설정 row가 없는 유저가 등록하면 새 row를 저장한다`() {
        `when`(userMailDispatchSettingRepository.findByUserId(1L)).thenReturn(null)
        `when`(userMailDispatchSettingRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer { it.arguments[0] }

        val response = userMailDispatchSettingService.register(1L, LocalTime.of(10, 30))

        assert(response.dispatchTime == LocalTime.of(10, 30))
        org.mockito.Mockito.verify(userMailDispatchSettingRepository).save(org.mockito.ArgumentMatchers.any())
    }

    @Test
    fun `설정 row가 있는 유저가 다시 등록하면 기존 row 값을 변경한다`() {
        val existing = UserMailDispatchSetting(userId = 1L, dispatchTime = LocalTime.of(9, 0))
        `when`(userMailDispatchSettingRepository.findByUserId(1L)).thenReturn(existing)

        val response = userMailDispatchSettingService.register(1L, LocalTime.of(18, 0))

        assert(response.dispatchTime == LocalTime.of(18, 0))
        assert(existing.dispatchTime == LocalTime.of(18, 0))
        org.mockito.Mockito.verify(userMailDispatchSettingRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any())
    }

    @Test
    fun `30분 단위가 아닌 시각으로 등록하면 INVALID_MAIL_DISPATCH_TIME 예외가 발생한다`() {
        val exception = assertThrows<BusinessException> {
            userMailDispatchSettingService.register(1L, LocalTime.of(9, 15))
        }
        assert(exception.resultCode == ResultCode.INVALID_MAIL_DISPATCH_TIME)
    }
}
