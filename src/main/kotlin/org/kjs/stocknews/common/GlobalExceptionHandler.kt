package org.kjs.stocknews.common

import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.dao.QueryTimeoutException
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.transaction.CannotCreateTransactionException
import org.springframework.transaction.TransactionSystemException
import org.springframework.transaction.UnexpectedRollbackException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingPathVariableException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientException
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.support.MissingServletRequestPartException
import org.springframework.web.servlet.resource.NoResourceFoundException

/**
 * 전역 예외 처리기.
 *
 * 모든 실패 응답을 [ApiResponse]로 통일하되, 원인 계층별로 [ResultCode]를 나눠 내려준다.
 * 예전에는 BusinessException이 아닌 예외가 전부 맨 아래 catch-all로 떨어져 INTERNAL_ERROR로
 * 뭉개졌고, 그 탓에 "요청 body 누락"(클라이언트 실수)과 "DB 연결 끊김"(인프라 장애)을
 * 응답만으로 구분할 수 없었다. 계층별 핸들러를 두어 프론트/운영자가 코드만 보고
 * 원인 계층을 특정할 수 있게 한다.
 *
 * 응답 본문에 예외 메시지를 그대로 싣지는 않는다(스키마·쿼리·내부 호스트 노출 방지).
 * 상세 원인은 서버 로그에만 남기고, 클라이언트에는 계층을 구분하는 코드만 전달한다.
 *
 * 핸들러가 겹치는 경우(예: DataAccessResourceFailureException vs DataAccessException)
 * Spring이 ExceptionDepthComparator로 더 구체적인 쪽을 선택하므로 선언 순서는 영향이 없다.
 *
 * NOTE: 기존 컨벤션대로 실패 응답도 HTTP 200으로 나간다. 프론트가 body의 code만 보고
 * 분기하고 있어 호환을 위해 유지한 것으로, HTTP status 정상화는 별도 과제로 남긴다.
 */
@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ApiResponse<Nothing> {
        // 원인이 있는 경우(하위 계층 예외를 감싼 경우)만 로깅한다 - 입력 검증 실패 같은 일반적인
        // 비즈니스 실패는 원인이 없어 로그 잡음을 만들지 않는다.
        e.cause?.let { log.warn("business exception: {}", e.resultCode, it) }
        return ApiResponse.fail(e.resultCode)
    }

    // ---------------------------------------------------------------------
    // 클라이언트 요청 오류 - 서버 장애가 아니므로 error가 아닌 warn으로 남긴다.
    // ---------------------------------------------------------------------

    /**
     * 요청 본문을 역직렬화하지 못한 경우.
     * body 누락, 깨진 JSON, non-null 필드 누락, enum/타입 변환 실패가 모두 여기로 온다.
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMalformedBody(e: HttpMessageNotReadableException): ApiResponse<Nothing> {
        log.warn("malformed request body: {}", e.message)
        return ApiResponse.fail(ResultCode.MALFORMED_REQUEST_BODY)
    }

    /** POST 전용 엔드포인트에 GET으로 요청하는 등 HTTP 메서드가 맞지 않는 경우. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupported(e: HttpRequestMethodNotSupportedException): ApiResponse<Nothing> {
        log.warn("method not supported: {}", e.message)
        return ApiResponse.fail(ResultCode.METHOD_NOT_ALLOWED)
    }

    /**
     * 파라미터/헤더/파트 누락, 타입 불일치, Bean Validation 실패 등 나머지 요청 오류.
     * 여기 등록하지 않으면 전부 맨 아래 catch-all로 떨어져 INTERNAL_ERROR가 되므로,
     * Spring MVC의 주요 클라이언트 오류 예외를 명시적으로 모아둔다.
     */
    @ExceptionHandler(
        MissingServletRequestParameterException::class,
        MethodArgumentTypeMismatchException::class,
        MissingRequestHeaderException::class,
        MissingPathVariableException::class,
        MissingServletRequestPartException::class,
        HttpMediaTypeNotSupportedException::class,
        MethodArgumentNotValidException::class,
        HandlerMethodValidationException::class,
    )
    fun handleInvalidRequest(e: Exception): ApiResponse<Nothing> {
        log.warn("invalid request: {}", e.message)
        return ApiResponse.fail(ResultCode.BAD_REQUEST)
    }

    /** 매핑되지 않은 경로 요청 - 서버 장애가 아니므로 NOT_FOUND로 내린다. */
    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResource(e: NoResourceFoundException): ApiResponse<Nothing> =
        ApiResponse.fail(ResultCode.NOT_FOUND)

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(e: NoSuchElementException): ApiResponse<Nothing> =
        ApiResponse.fail(ResultCode.NOT_FOUND)

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(e: IllegalArgumentException): ApiResponse<Nothing> =
        ApiResponse.fail(ResultCode.BAD_REQUEST)

    // ---------------------------------------------------------------------
    // 인프라 장애 - DB / 외부 API. 재시도로 풀릴 수 있는 계층이라 코드를 분리한다.
    // ---------------------------------------------------------------------

    /**
     * DB에 아예 붙지 못한 경우(커넥션 획득 실패, 트랜잭션 시작 실패).
     * DB 컨테이너 다운, 커넥션 풀 고갈, 네트워크 단절이 여기로 온다.
     * CannotCreateTransactionException은 TransactionException 계열이라
     * DataAccessException 핸들러에 걸리지 않으므로 함께 등록한다.
     */
    @ExceptionHandler(
        DataAccessResourceFailureException::class,
        CannotCreateTransactionException::class,
    )
    fun handleDatabaseUnavailable(e: Exception): ApiResponse<Nothing> {
        log.error("database unavailable", e)
        return ApiResponse.fail(ResultCode.DATABASE_UNAVAILABLE)
    }

    /**
     * 연결은 됐으나 쿼리가 제한 시간을 넘긴 경우.
     * 접속 불가와 달리 느린 쿼리·락 경합·과부하가 원인이라 코드를 분리한다.
     */
    @ExceptionHandler(QueryTimeoutException::class)
    fun handleDatabaseTimeout(e: QueryTimeoutException): ApiResponse<Nothing> {
        log.error("database query timed out", e)
        return ApiResponse.fail(ResultCode.DATABASE_TIMEOUT)
    }

    /**
     * 쿼리 실행 실패(제약 위반, 스키마 불일치, 문법 오류) 및 트랜잭션 커밋/롤백 실패.
     * TransactionSystemException/UnexpectedRollbackException은 DataAccessException
     * 하위가 아니므로 함께 등록한다.
     */
    @ExceptionHandler(
        DataAccessException::class,
        TransactionSystemException::class,
        UnexpectedRollbackException::class,
    )
    fun handleDatabaseError(e: Exception): ApiResponse<Nothing> {
        log.error("database error", e)
        return ApiResponse.fail(ResultCode.DATABASE_ERROR)
    }

    /**
     * 외부 API에 도달하지 못한 경우 (SEC, Naver, Finnhub, Toss, NVIDIA 등 RestClient 호출).
     * 타임아웃뿐 아니라 DNS 실패, 연결 거부, TLS/소켓 I/O 오류가 모두 포함된다.
     */
    @ExceptionHandler(ResourceAccessException::class)
    fun handleExternalApiUnavailable(e: ResourceAccessException): ApiResponse<Nothing> {
        log.error("external api not reachable", e)
        return ApiResponse.fail(ResultCode.EXTERNAL_API_UNAVAILABLE)
    }

    /** 외부 API가 응답은 했으나 4xx/5xx이거나 본문을 해석하지 못한 경우. */
    @ExceptionHandler(RestClientException::class)
    fun handleExternalApiError(e: RestClientException): ApiResponse<Nothing> {
        log.error("external api call failed", e)
        return ApiResponse.fail(ResultCode.EXTERNAL_API_ERROR)
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(e: Exception): ApiResponse<Nothing> {
        log.error("Unhandled exception", e)
        return ApiResponse.fail(ResultCode.INTERNAL_ERROR)
    }
}
