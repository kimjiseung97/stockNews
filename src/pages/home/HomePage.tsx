import EmailPreview from '@/components/home/EmailPreview'
import HomeCta from '@/components/home/HomeCta'
import HomeIntro from '@/components/home/HomeIntro'
import ServiceFeatures from '@/components/home/ServiceFeatures'
import StartGuide from '@/components/home/StartGuide'
import styles from '@/assets/styles/pages/home/home.module.scss'
import mediaStyles from '@/assets/styles/pages/home/homeMedia.module.scss'

function HomePage() {
  return (
    <main id="homePage" className={`${styles['home-page']} ${mediaStyles['home-page']}`}>
      <HomeIntro></HomeIntro>
      <ServiceFeatures></ServiceFeatures>
      <StartGuide></StartGuide>
      <EmailPreview></EmailPreview>
      <HomeCta></HomeCta>
    </main>
  )
}

export default HomePage
