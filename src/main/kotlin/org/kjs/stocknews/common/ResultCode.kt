package org.kjs.stocknews.common

enum class ResultCode(val code: String, val message: String) {
    OK("OK", "SUCCESS"),
    BAD_REQUEST("BAD_REQUEST", "잘못된 요청입니다"),
    NOT_FOUND("NOT_FOUND", "요청한 리소스를 찾을 수 없습니다"),
    INTERNAL_ERROR("INTERNAL_ERROR", "서버 오류가 발생했습니다"),
    EMAIL_ALREADY_REGISTERED("EMAIL_ALREADY_REGISTERED", "이미 가입된 이메일입니다"),
    VERIFICATION_NOT_FOUND("VERIFICATION_NOT_FOUND", "인증 요청 내역을 찾을 수 없습니다"),
    VERIFICATION_EXPIRED("VERIFICATION_EXPIRED", "인증코드가 만료되었습니다"),
    VERIFICATION_CODE_MISMATCH("VERIFICATION_CODE_MISMATCH", "인증코드가 일치하지 않습니다"),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "이메일 또는 비밀번호가 일치하지 않습니다"),
    UNAUTHORIZED("UNAUTHORIZED", "로그인이 필요합니다"),
    STOCK_NOT_FOUND("STOCK_NOT_FOUND", "존재하지 않는 종목입니다"),
    USER_STOCK_NOT_FOUND("USER_STOCK_NOT_FOUND", "등록되지 않은 관심종목입니다"),
}
