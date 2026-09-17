import { useEffect } from 'react'
import { SITE_URL } from '@/constants/seo'

interface SeoProps {
  title: string
  description: string
  path: string
  keywords?: string[]
  noindex?: boolean
  structuredData?: Record<string, unknown>
}

const SERVICE_NAME = '스톡뉴스 StockNews'

function setMeta(name: string, content: string, attribute: 'name' | 'property' = 'name') {
  let meta = document.head.querySelector<HTMLMetaElement>(`meta[${attribute}="${name}"]`)

  if (!meta) {
    meta = document.createElement('meta')
    meta.setAttribute(attribute, name)
    document.head.appendChild(meta)
  }

  meta.content = content
}

function Seo({ title, description, path, keywords = [], noindex = false, structuredData }: SeoProps) {
  useEffect(() => {
    const canonicalUrl = `${SITE_URL}${path}`
    const openGraphImageUrl = `${SITE_URL}/op.jpg`
    const fullTitle = title === '스톡뉴스' ? SERVICE_NAME : `${title} | ${SERVICE_NAME}`
    let canonical = document.head.querySelector<HTMLLinkElement>('link[rel="canonical"]')

    document.title = fullTitle
    document.documentElement.lang = 'ko'
    document.documentElement.dataset.seoManaged = 'true'

    setMeta('description', description)
    setMeta('robots', noindex ? 'noindex, nofollow' : 'index, follow, max-image-preview:large')
    setMeta('googlebot', noindex ? 'noindex, nofollow' : 'index, follow, max-image-preview:large')
    setMeta('og:locale', 'ko_KR', 'property')
    setMeta('og:type', 'website', 'property')
    setMeta('og:site_name', SERVICE_NAME, 'property')
    setMeta('og:title', fullTitle, 'property')
    setMeta('og:description', description, 'property')
    setMeta('og:url', canonicalUrl, 'property')
    setMeta('og:image', openGraphImageUrl, 'property')
    setMeta('og:image:type', 'image/jpeg', 'property')
    setMeta('og:image:width', '1200', 'property')
    setMeta('og:image:height', '630', 'property')
    setMeta('og:image:alt', '스톡뉴스 미국주식 뉴스 서비스', 'property')
    setMeta('twitter:card', 'summary_large_image')
    setMeta('twitter:title', fullTitle)
    setMeta('twitter:description', description)
    setMeta('twitter:image', openGraphImageUrl)
    setMeta('twitter:image:alt', '스톡뉴스 미국주식 뉴스 서비스')

    if (keywords.length > 0) {
      setMeta('keywords', keywords.join(', '))
    }

    if (!canonical) {
      canonical = document.createElement('link')
      canonical.rel = 'canonical'
      document.head.appendChild(canonical)
    }

    canonical.href = canonicalUrl

    const previousStructuredData = document.getElementById('pageStructuredData')
    previousStructuredData?.remove()

    if (structuredData) {
      const script = document.createElement('script')
      script.id = 'pageStructuredData'
      script.type = 'application/ld+json'
      script.text = JSON.stringify(structuredData)
      document.head.appendChild(script)
    }

    return () => {
      document.getElementById('pageStructuredData')?.remove()
    }
  }, [description, keywords, noindex, path, structuredData, title])

  return null
}

export default Seo
