package org.kjs.stocknews.model.table

// 다이제스트 메일 1건의 발송 결과. 어드민(stockNewsAdmin)의 같은 이름 enum과 값이 일치해야 한다 - DB에 문자열로 저장된다.
enum class MailDispatchStatus {
    SUCCESS,
    FAILED,

    // 관심종목이 없거나 보낼 뉴스가 하나도 없어 발송 자체를 건너뛴 경우.
    SKIPPED,
}
