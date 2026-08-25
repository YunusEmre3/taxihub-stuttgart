import { useI18n } from '../i18n/I18nContext'

const LOCALES = ['tr', 'de', 'en']

export default function LanguageSwitcher({ dark = false }) {
  const { locale, setLocale } = useI18n()

  return (
    <div
      className={`flex gap-1 rounded-full p-1 text-xs font-bold ${
        dark ? 'bg-th-black/60 backdrop-blur' : 'bg-white/80 backdrop-blur border border-gray-200'
      }`}
    >
      {LOCALES.map((code) => (
        <button
          key={code}
          type="button"
          onClick={() => setLocale(code)}
          className={`px-2.5 py-1 rounded-full uppercase transition ${
            locale === code
              ? 'bg-th-yellow text-th-black'
              : dark
                ? 'text-gray-200 hover:text-white'
                : 'text-gray-500 hover:text-th-black'
          }`}
        >
          {code}
        </button>
      ))}
    </div>
  )
}
