import { useI18n } from '../i18n/I18nContext'

export default function StaffLoginLink() {
  const { t } = useI18n()
  return (
    <a
      href="/login"
      className="rounded-full bg-th-black/80 backdrop-blur px-4 py-2 text-xs sm:text-sm font-semibold text-white border border-th-yellow hover:bg-th-black transition whitespace-nowrap"
    >
      {t('staffLogin.link')}
    </a>
  )
}
