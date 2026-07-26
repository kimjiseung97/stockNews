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
    EMAIL_REQUIRED("EMAIL_REQUIRED", "이메일을 입력해주세요"),
    INVALID_EMAIL_FORMAT("INVALID_EMAIL_FORMAT", "올바른 이메일 형식이 아닙니다"),
    EMAIL_TOO_LONG("EMAIL_TOO_LONG", "이메일은 50자 이하로 입력해주세요"),
    PASSWORD_REQUIRED("PASSWORD_REQUIRED", "비밀번호를 입력해주세요"),
    INVALID_PASSWORD_LENGTH("INVALID_PASSWORD_LENGTH", "비밀번호는 8~20자로 입력해주세요"),
    VERIFICATION_CODE_REQUIRED("VERIFICATION_CODE_REQUIRED", "인증코드를 입력해주세요"),
    INVALID_VERIFICATION_CODE_LENGTH("INVALID_VERIFICATION_CODE_LENGTH", "인증코드 자릿수가 올바르지 않습니다"),
    UNAUTHORIZED("UNAUTHORIZED", "로그인이 필요합니다"),
    STOCK_NOT_FOUND("STOCK_NOT_FOUND", "존재하지 않는 종목입니다"),
    USER_STOCK_NOT_FOUND("USER_STOCK_NOT_FOUND", "등록되지 않은 관심종목입니다"),
}
