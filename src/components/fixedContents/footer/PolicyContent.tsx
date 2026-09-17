import styles from '@/assets/styles/common/modal.module.scss'

const SUPPORT_EMAIL = 'febedeveloper9697@gmail.com'

export function TermsContent() {
  return (
    <>
      <h3 className={styles['modal__content-title']}>제1조 목적</h3>
      <p className={styles['modal__content-text']}>
        본 약관은 StockNews(스톡뉴스)(이하 "서비스")가 제공하는 미국 주식 관련 기사 및 이메일 기사
        제공 서비스의 이용에 필요한 사항을 정하는 것을 목적으로 합니다.
      </p>

      <h3 className={styles['modal__content-title']}>제2조 서비스 제공</h3>
      <p className={styles['modal__content-text']}>StockNews(스톡뉴스)는 다음과 같은 서비스를 제공합니다.</p>
      <ul className={styles['modal__content-list']}>
        <li className={styles['modal__content-item']}>미국 주식 종목 검색</li>
        <li className={styles['modal__content-item']}>미국 주식 종목 관련 기사 제공</li>
        <li className={styles['modal__content-item']}>관심 종목 관련 기사 이메일 제공</li>
        <li className={styles['modal__content-item']}>회원 계정 관리</li>
        <li className={styles['modal__content-item']}>기타 서비스 운영에 필요한 기능</li>
      </ul>
      <p className={styles['modal__content-text']}>서비스의 일부 기능은 운영 및 기술상의 필요에 따라 변경되거나 추가될 수 있습니다.</p>

      <h3 className={styles['modal__content-title']}>제3조 회원가입</h3>
      <ul className={styles['modal__content-list']}>
        <li className={styles['modal__content-item']}>이용자는 서비스에서 정한 방법에 따라 회원가입을 할 수 있습니다.</li>
        <li className={styles['modal__content-item']}>회원가입 시 정확한 이메일 정보를 입력해야 합니다.</li>
        <li className={styles['modal__content-item']}>타인의 이메일 또는 정보를 무단으로 이용해서는 안 됩니다.</li>
        <li className={styles['modal__content-item']}>부정한 방법으로 서비스를 이용하는 경우 서비스 이용이 제한될 수 있습니다.</li>
      </ul>

      <h3 className={styles['modal__content-title']}>제4조 계정 관리</h3>
      <p className={styles['modal__content-text']}>회원은 자신의 계정 정보를 안전하게 관리해야 합니다.</p>
      <p className={styles['modal__content-text']}>
        계정이 도용되거나 부정하게 사용된 사실을 확인한 경우 서비스 관련 문의 이메일을 통해
        운영자에게 알릴 수 있습니다.
      </p>

      <h3 className={styles['modal__content-title']}>제5조 이메일 기사 서비스</h3>
      <ul className={styles['modal__content-list']}>
        <li className={styles['modal__content-item']}>회원은 원하는 미국 주식 종목을 선택하여 관련 기사를 이메일로 받아볼 수 있습니다.</li>
        <li className={styles['modal__content-item']}>이메일은 회원이 서비스에 등록한 이메일 주소로 발송됩니다.</li>
        <li className={styles['modal__content-item']}>이메일 발송 시간 및 방식은 서비스에서 제공하는 기능에 따라 적용됩니다.</li>
        <li className={styles['modal__content-item']}>
          네트워크 장애, 이메일 서비스 제공업체의 문제, 스팸 필터 등의 사유로 이메일 발송 또는
          수신이 지연될 수 있습니다.
        </li>
      </ul>

      <h3 className={styles['modal__content-title']}>제6조 서비스 이용 시 금지사항</h3>
      <p className={styles['modal__content-text']}>이용자는 다음과 같은 행위를 해서는 안 됩니다.</p>
      <ul className={styles['modal__content-list']}>
        <li className={styles['modal__content-item']}>타인의 계정을 무단으로 사용하는 행위</li>
        <li className={styles['modal__content-item']}>허위 정보를 이용하여 가입하는 행위</li>
        <li className={styles['modal__content-item']}>서비스의 정상적인 운영을 방해하는 행위</li>
        <li className={styles['modal__content-item']}>자동화된 프로그램 등을 이용하여 서비스 데이터를 비정상적인 방법으로 수집하는 행위</li>
        <li className={styles['modal__content-item']}>서비스 또는 제3자의 권리를 침해하는 행위</li>
        <li className={styles['modal__content-item']}>관련 법령을 위반하는 행위</li>
      </ul>
      <p className={styles['modal__content-text']}>위와 같은 행위가 확인되는 경우 서비스 이용이 제한될 수 있습니다.</p>

      <h3 className={styles['modal__content-title']}>제7조 뉴스 및 투자 정보</h3>
      <ul className={styles['modal__content-list']}>
        <li className={styles['modal__content-item']}>StockNews(스톡뉴스)에서 제공하는 기사 및 관련 정보는 정보 제공을 목적으로 합니다.</li>
        <li className={styles['modal__content-item']}>
          서비스에서 제공하는 정보는 특정 주식 또는 금융상품의 매수 또는 매도를 권유하기 위한
          것이 아닙니다.
        </li>
        <li className={styles['modal__content-item']}>투자에 관한 최종 판단과 결정은 이용자 본인에게 있습니다.</li>
        <li className={styles['modal__content-item']}>
          기사 및 관련 정보는 제공 시점이나 외부 정보 제공처의 사정에 따라 실제 정보와 차이가
          발생할 수 있습니다.
        </li>
        <li className={styles['modal__content-item']}>StockNews(스톡뉴스)는 특정 금융상품의 투자 수익이나 투자 결과를 보장하지 않습니다.</li>
      </ul>

      <h3 className={styles['modal__content-title']}>제8조 서비스의 변경 및 중단</h3>
      <p className={styles['modal__content-text']}>StockNews(스톡뉴스)는 서비스 운영 또는 기술상의 필요에 따라 일부 기능을 변경하거나 중단할 수 있습니다.</p>
      <p className={styles['modal__content-text']}>
        서버 점검, 시스템 장애, 네트워크 장애 등 불가피한 사유가 발생하는 경우 서비스 이용이
        일시적으로 제한될 수 있습니다.
      </p>
      <p className={styles['modal__content-text']}>이용자에게 중요한 영향을 미치는 변경사항이 있는 경우 서비스 내에서 안내합니다.</p>

      <h3 className={styles['modal__content-title']}>제9조 회원 탈퇴</h3>
      <p className={styles['modal__content-text']}>회원은 언제든지 회원 탈퇴를 요청할 수 있습니다.</p>
      <p className={styles['modal__content-text']}>
        회원 탈퇴를 원하는 경우 아래 서비스 관련 문의 이메일로{' '}
        <strong className={styles['modal__content-emphasis']}>탈퇴할 계정의 이메일 주소를 작성하여 보내주시기 바랍니다.</strong>
      </p>
      <p className={styles['modal__content-text']}>
        서비스 관련 문의
        <br></br>
        <a className={styles['modal__content-link']} href={`mailto:${SUPPORT_EMAIL}`}>{SUPPORT_EMAIL}</a>
      </p>
      <p className={styles['modal__content-text']}>탈퇴 요청이 확인되면 관련 법령에 따라 보관할 필요가 있는 경우를 제외하고 해당 계정 및 개인정보를 삭제합니다.</p>

      <h3 className={styles['modal__content-title']}>제10조 약관의 변경</h3>
      <p className={styles['modal__content-text']}>StockNews(스톡뉴스)는 관련 법령 또는 서비스 운영상의 필요에 따라 본 약관을 변경할 수 있습니다.</p>
      <p className={styles['modal__content-text']}>중요한 내용이 변경되는 경우 적용일 및 변경 내용을 서비스 내에서 안내합니다.</p>

      <h3 className={styles['modal__content-title']}>제11조 문의</h3>
      <p className={styles['modal__content-text']}>서비스 이용과 관련된 문의는 아래 이메일을 통해 접수할 수 있습니다.</p>
      <p className={styles['modal__content-text']}>
        서비스 관련 문의
        <br></br>
        <a className={styles['modal__content-link']} href={`mailto:${SUPPORT_EMAIL}`}>{SUPPORT_EMAIL}</a>
      </p>

      <h3 className={styles['modal__content-title']}>시행일</h3>
      <p className={styles['modal__content-text']}>
        본 이용약관은 <strong className={styles['modal__content-emphasis']}>2026년 9월 17일부터 시행합니다.</strong>
      </p>
    </>
  )
}

export function PrivacyContent() {
  return (
    <>
      <p className={styles['modal__content-text']}>
        StockNews(스톡뉴스)(이하 "서비스")는 서비스 제공을 위해 필요한 최소한의 개인정보를
        수집하고 있으며, 이용자의 개인정보를 안전하게 관리하기 위해 노력하고 있습니다.
      </p>

      <h3 className={styles['modal__content-title']}>1. 수집하는 개인정보</h3>
      <p className={styles['modal__content-text']}>서비스는 회원가입 및 서비스 이용을 위해 다음 정보를 수집합니다.</p>
      <ul className={styles['modal__content-list']}>
        <li className={styles['modal__content-item']}>로그인 이메일</li>
        <li className={styles['modal__content-item']}>계정 복구용 이메일</li>
        <li className={styles['modal__content-item']}>비밀번호</li>
      </ul>
      <p className={styles['modal__content-text']}>
        비밀번호는 안전하게 암호화되어 저장되며, StockNews(스톡뉴스) 운영자도 비밀번호 원문을
        확인할 수 없습니다.
      </p>

      <h3 className={styles['modal__content-title']}>2. 개인정보 이용 목적</h3>
      <p className={styles['modal__content-text']}>수집한 개인정보는 다음 목적으로 이용합니다.</p>
      <ul className={styles['modal__content-list']}>
        <li className={styles['modal__content-item']}>회원가입 및 로그인</li>
        <li className={styles['modal__content-item']}>회원 계정 확인</li>
        <li className={styles['modal__content-item']}>계정 및 비밀번호 복구</li>
        <li className={styles['modal__content-item']}>관심 종목 관련 기사 이메일 발송</li>
        <li className={styles['modal__content-item']}>서비스 이용에 필요한 안내</li>
      </ul>
      <p className={styles['modal__content-text']}>수집한 개인정보는 위 목적 이외의 용도로 이용하지 않습니다.</p>

      <h3 className={styles['modal__content-title']}>3. 개인정보 수집 방법</h3>
      <p className={styles['modal__content-text']}>개인정보는 회원가입 및 서비스 이용 과정에서 이용자가 직접 입력하는 방법으로 수집합니다.</p>

      <h3 className={styles['modal__content-title']}>4. 개인정보 보유 및 이용 기간</h3>
      <p className={styles['modal__content-text']}>개인정보는 회원이 StockNews(스톡뉴스)를 이용하는 동안 보관합니다.</p>
      <p className={styles['modal__content-text']}>
        회원 탈퇴 요청이 처리된 경우 관련 법령에 따라 별도로 보관할 필요가 있는 경우를 제외하고
        해당 계정 및 개인정보를 삭제합니다.
      </p>

      <h3 className={styles['modal__content-title']}>5. 개인정보의 제3자 제공</h3>
      <p className={styles['modal__content-text']}>StockNews(스톡뉴스)는 이용자의 개인정보를 원칙적으로 제3자에게 제공하지 않습니다.</p>
      <p className={styles['modal__content-text']}>다만 다음의 경우에는 예외로 합니다.</p>
      <ul className={styles['modal__content-list']}>
        <li className={styles['modal__content-item']}>이용자가 사전에 동의한 경우</li>
        <li className={styles['modal__content-item']}>관련 법령에 따라 개인정보 제공이 필요한 경우</li>
      </ul>

      <h3 className={styles['modal__content-title']}>6. 이메일 기사 제공</h3>
      <p className={styles['modal__content-text']}>
        이용자가 관심 종목의 기사 이메일 수신을 신청한 경우 등록된 이메일 주소를 이용하여 관련
        기사 및 정보를 발송할 수 있습니다.
      </p>
      <p className={styles['modal__content-text']}>이메일 주소는 이용자가 신청한 기사 및 서비스 관련 정보를 제공하기 위한 목적으로 사용합니다.</p>

      <h3 className={styles['modal__content-title']}>7. 개인정보 보호</h3>
      <p className={styles['modal__content-text']}>StockNews(스톡뉴스)는 이용자의 개인정보를 안전하게 관리하기 위해 필요한 보호 조치를 시행합니다.</p>
      <p className={styles['modal__content-text']}>특히 비밀번호는 암호화되어 저장되며 운영자가 비밀번호 원문을 확인할 수 없습니다.</p>

      <h3 className={styles['modal__content-title']}>8. 회원 탈퇴 및 개인정보 관련 요청</h3>
      <p className={styles['modal__content-text']}>회원 탈퇴를 원하는 경우 아래 서비스 관련 문의 이메일로 요청할 수 있습니다.</p>
      <p className={styles['modal__content-text']}>
        회원 탈퇴 요청 시 본인의 계정을 확인할 수 있도록 메일 내용에{' '}
        <strong className={styles['modal__content-emphasis']}>탈퇴할 계정의 이메일 주소를 작성하여 보내주시기 바랍니다.</strong>
      </p>
      <p className={styles['modal__content-text']}>개인정보와 관련된 문의 또는 법령에 따른 개인정보 관련 요청도 아래 이메일을 통해 접수할 수 있습니다.</p>
      <p className={styles['modal__content-text']}>
        서비스 관련 문의
        <br></br>
        <a className={styles['modal__content-link']} href={`mailto:${SUPPORT_EMAIL}`}>{SUPPORT_EMAIL}</a>
      </p>
      <p className={styles['modal__content-text']}>
        회원 탈퇴 요청이 확인되면 관련 법령에 따라 별도로 보관할 필요가 있는 경우를 제외하고 해당
        계정 및 개인정보를 삭제합니다.
      </p>

      <h3 className={styles['modal__content-title']}>9. 개인정보처리방침 변경</h3>
      <p className={styles['modal__content-text']}>본 개인정보처리방침의 내용이 변경되는 경우 서비스 내 공지 등을 통해 변경 내용을 안내합니다.</p>

      <h3 className={styles['modal__content-title']}>시행일</h3>
      <p className={styles['modal__content-text']}>
        본 개인정보처리방침은 <strong className={styles['modal__content-emphasis']}>2026년 9월 17일부터 시행합니다.</strong>
      </p>
    </>
  )
}

export function ContactContent() {
  return (
    <p className={styles['modal__content-text']}>협업 및 광고 제안은 준비 중인 게시판입니다.</p>
  )
}
