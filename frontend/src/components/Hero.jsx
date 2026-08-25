import heroImage from '../assets/stuttgart-hero.jpg'
import { useI18n } from '../i18n/I18nContext'

export default function Hero({ onStart }) {
  const { t } = useI18n()
  return (
    <div
      className="relative min-h-screen flex items-center justify-center bg-cover bg-center px-4"
      style={{
        backgroundImage: `linear-gradient(180deg, rgba(17,17,17,0.55), rgba(17,17,17,0.88)), url(${heroImage})`,
      }}
    >
      <div className="text-center max-w-2xl">
        <div className="text-th-yellow font-extrabold tracking-widest text-sm mb-4">{t('hero.brand')}</div>
        <h1 className="text-4xl sm:text-6xl font-extrabold text-white leading-tight mb-4">{t('hero.title')}</h1>
        <p className="text-gray-200 text-lg mb-10">{t('hero.subtitle')}</p>
        <button
          type="button"
          onClick={onStart}
          className="bg-th-yellow hover:bg-th-yellow-dark text-th-black font-bold text-lg px-10 py-4 rounded-full shadow-lg transition"
        >
          {t('hero.cta')}
        </button>
      </div>

      <div className="absolute bottom-2 right-3 text-[10px] text-gray-400">{t('hero.photoCredit')}</div>
    </div>
  )
}
