import type { PrerenderArguments, PrerenderResult } from 'vite-prerender-plugin'
import { SITE_URL } from '@/constants/seo'

interface PrerenderPage {
  title: string
  description: string
  keywords: string
  html: string
}

const SERVICE_NAME = '스톡뉴스 StockNews'
const OPEN_GRAPH_IMAGE_URL = `${SITE_URL}/op.jpg`

const pages: Record<string, PrerenderPage> = {
  '/': {
    title: '스톡뉴스 StockNews | 미국주식 뉴스 요약과 이메일 알림',
    description:
      '스톡뉴스(StockNews)에서 미국주식 뉴스를 종목별로 모아 보고, 관심 종목의 주요 소식을 이메일로 받아보세요.',
    keywords:
      '스톡뉴스, StockNews, stocknews, 미국주식 뉴스레터, 미국주식 뉴스 메일, 서학개미 뉴스, 미국주식 뉴스 요약, 미국주식 티커 검색',
    html: `
      <main>
        <section>
          <h1>스톡뉴스 StockNews</h1>
          <p>미국주식 뉴스를 종목별로 모아 보고, 관심 종목의 주요 소식을 이메일로 받아보세요.</p>
          <article>
            <h2>미국주식 종목 검색</h2>
            <p>기업명이나 티커로 원하는 미국주식 종목을 빠르게 찾을 수 있습니다.</p>
            <a href="/stock-search">미국주식 종목 검색하기</a>
          </article>
          <article>
            <h2>종목별 미국주식 뉴스</h2>
            <p>엔비디아, 테슬라 등 관심 종목의 최신 뉴스와 주요 기사를 한곳에서 확인하세요.</p>
            <a href="/stock-news">미국주식 뉴스 확인하기</a>
          </article>
          <article>
            <h2>관심 종목 이메일 알림</h2>
            <p>관심 종목을 등록하면 설정한 시간에 최신 뉴스를 이메일로 받아볼 수 있습니다.</p>
          </article>
        </section>
      </main>
    `,
  },
  '/stock-search': {
    title: '미국주식 티커 검색 | 스톡뉴스 StockNews',
    description:
      '기업명이나 티커로 미국주식 종목을 검색하고 기업 정보와 관련 뉴스를 확인하세요. 엔비디아, 테슬라 등 관심 종목을 빠르게 찾을 수 있습니다.',
    keywords: '미국주식 티커 검색, 미국주식 종목 검색, 엔비디아 주식, 테슬라 주가 뉴스',
    html: `
      <main>
        <section>
          <h1>미국주식 티커 검색</h1>
          <p>기업명이나 티커를 검색해 원하는 미국주식 종목과 기업 정보를 확인하세요.</p>
          <article>
            <h2>관심 종목의 최신 정보 확인</h2>
            <p>검색한 종목의 기업 정보와 관련 뉴스를 확인하고 관심 종목으로 등록할 수 있습니다.</p>
            <a href="/stock-news">종목별 미국주식 뉴스 보기</a>
          </article>
        </section>
      </main>
    `,
  },
  '/stock-news': {
    title: '미국주식 뉴스 요약 | 스톡뉴스 StockNews',
    description:
      '엔비디아, 테슬라 등 관심 종목의 최신 미국주식 뉴스를 검색하고 주요 기사를 한곳에서 확인하세요.',
    keywords: '미국주식 뉴스 요약, 서학개미 뉴스, 엔비디아 실적발표일, 테슬라 주가 뉴스',
    html: `
      <main>
        <section>
          <h1>종목별 미국주식 뉴스</h1>
          <p>관심 있는 미국주식 종목을 검색하고 최신 뉴스와 주요 기사를 한곳에서 확인하세요.</p>
          <article>
            <h2>관심 종목 뉴스 모아보기</h2>
            <p>엔비디아, 테슬라 등 원하는 종목을 선택해 관련 뉴스를 최신순으로 확인할 수 있습니다.</p>
            <a href="/stock-search">미국주식 종목 검색하기</a>
          </article>
        </section>
      </main>
    `,
  },
}

// 공개 페이지마다 검색로봇이 읽을 수 있는 HTML과 SEO 정보를 생성
export function prerender({ url }: PrerenderArguments): PrerenderResult {
  const pathname = new URL(url, SITE_URL).pathname
  const page = pages[pathname] ?? pages['/']
  const canonicalUrl = `${SITE_URL}${pathname}`
  const headElements: Array<{ type: string; props: Record<string, string> }> = [
    { type: 'meta', props: { name: 'description', content: page.description } },
    { type: 'meta', props: { name: 'keywords', content: page.keywords } },
    {
      type: 'meta',
      props: { name: 'robots', content: 'index, follow, max-image-preview:large' },
    },
    { type: 'link', props: { rel: 'canonical', href: canonicalUrl } },
    { type: 'meta', props: { property: 'og:locale', content: 'ko_KR' } },
    { type: 'meta', props: { property: 'og:type', content: 'website' } },
    { type: 'meta', props: { property: 'og:site_name', content: SERVICE_NAME } },
    { type: 'meta', props: { property: 'og:title', content: page.title } },
    { type: 'meta', props: { property: 'og:description', content: page.description } },
    { type: 'meta', props: { property: 'og:url', content: canonicalUrl } },
    { type: 'meta', props: { property: 'og:image', content: OPEN_GRAPH_IMAGE_URL } },
    { type: 'meta', props: { name: 'twitter:card', content: 'summary_large_image' } },
    { type: 'meta', props: { name: 'twitter:title', content: page.title } },
    { type: 'meta', props: { name: 'twitter:description', content: page.description } },
    { type: 'meta', props: { name: 'twitter:image', content: OPEN_GRAPH_IMAGE_URL } },
  ]

  return {
    html: page.html,
    links: new Set(Object.keys(pages)),
    head: {
      lang: 'ko',
      title: page.title,
      elements: new Set(headElements),
    },
  }
}
