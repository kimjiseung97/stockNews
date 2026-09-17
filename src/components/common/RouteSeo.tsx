import { useLocation, useSearchParams } from 'react-router-dom'
import Seo from '@/components/common/Seo'
import { SITE_URL } from '@/constants/seo'

const mainKeywords = [
  '스톡뉴스',
  'StockNews',
  'stocknews',
  '미국주식 뉴스레터',
  '미국주식 뉴스 메일',
  '서학개미 뉴스',
  '미국주식 뉴스 요약',
  '미국증시 마감 시황',
  '미국장 프리마켓 시황',
  '미국주식 실적발표 일정',
  'FOMC 일정',
  '미국주식 배당일정',
  '엔비디아 실적발표일',
  '테슬라 주가 뉴스',
  '미국주식 티커 검색',
]

const privatePaths = [
  '/watchlist',
  '/watchlist/register',
  '/email-settings',
  '/login',
  '/sign-up',
  '/find-password',
  '/find-email',
]

function RouteSeo() {
  const location = useLocation()
  const [searchParams] = useSearchParams()
  const stockName = searchParams.get('koreaName')?.trim()

  if (location.pathname === '/') {
    return (
      <Seo
        title="스톡뉴스"
        description="스톡뉴스(StockNews)에서 미국주식 뉴스를 종목별로 모아 보고, 관심 종목의 주요 소식을 이메일로 받아보세요."
        path="/"
        keywords={mainKeywords}
        structuredData={{
          '@context': 'https://schema.org',
          '@type': 'WebSite',
          name: '스톡뉴스',
          alternateName: 'StockNews',
          url: `${SITE_URL}/`,
          description: '관심 종목의 미국주식 뉴스를 모아 이메일로 보내주는 뉴스 서비스',
          inLanguage: 'ko-KR',
          potentialAction: {
            '@type': 'SearchAction',
            target: `${SITE_URL}/stock-search?koreaName={search_term_string}`,
            'query-input': 'required name=search_term_string',
          },
        }}
      ></Seo>
    )
  }

  if (location.pathname === '/stock-search') {
    return (
      <Seo
        title="미국주식 티커 검색"
        description="기업명이나 티커로 미국주식 종목을 검색하고 기업 정보와 관련 뉴스를 확인하세요. 엔비디아, 테슬라 등 관심 종목을 빠르게 찾을 수 있습니다."
        path="/stock-search"
        keywords={['미국주식 티커 검색', '미국주식 종목 검색', '엔비디아 주식', '테슬라 주가 뉴스']}
      ></Seo>
    )
  }

  if (location.pathname === '/stock-news') {
    return (
      <Seo
        title={stockName ? `${stockName} 미국주식 뉴스` : '미국주식 뉴스 요약'}
        description={
          stockName
            ? `${stockName} 관련 최신 미국주식 뉴스를 한곳에서 확인하세요. 주요 기사와 종목 소식을 빠르게 모아봅니다.`
            : '엔비디아, 테슬라 등 관심 종목의 최신 미국주식 뉴스를 검색하고 주요 기사를 한곳에서 확인하세요.'
        }
        path="/stock-news"
        keywords={['미국주식 뉴스 요약', '서학개미 뉴스', '엔비디아 실적발표일', '테슬라 주가 뉴스']}
      ></Seo>
    )
  }

  if (location.pathname === '/stocks/detail') {
    return (
      <Seo
        title="미국주식 기업 정보"
        description="미국주식 종목의 기업 개요, 업종, 상장일과 공식 홈페이지 정보를 확인하세요."
        path="/stocks/detail"
        keywords={['미국주식 기업 정보', '미국주식 종목 정보']}
        noindex={true}
      ></Seo>
    )
  }

  const isPrivatePage = privatePaths.includes(location.pathname)

  return (
    <Seo
      title="스톡뉴스"
      description="관심 종목의 미국주식 뉴스를 모아 보고 이메일로 받아보세요."
      path={location.pathname}
      noindex={isPrivatePage}
    ></Seo>
  )
}

export default RouteSeo
