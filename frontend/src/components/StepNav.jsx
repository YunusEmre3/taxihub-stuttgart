import { useI18n } from '../i18n/I18nContext'

export default function StepNav({ onBack, onNext, nextLabel, nextDisabled = false, error }) {
  const { t } = useI18n()
  return (
    <div className="mt-8">
      {error && <p className="text-th-red text-sm mb-3">{error}</p>}
      <div className="flex justify-between">
        <button
          type="button"
          onClick={onBack}
          className="px-6 py-3 rounded-full border border-gray-300 text-th-black font-semibold hover:bg-gray-50"
        >
          {t('stepNav.back')}
        </button>
        <button
          type="button"
          onClick={onNext}
          disabled={nextDisabled}
          className="px-8 py-3 rounded-full bg-th-yellow text-th-black font-bold disabled:opacity-40 disabled:cursor-not-allowed hover:bg-th-yellow-dark transition"
        >
          {nextLabel ?? t('stepNav.next')}
        </button>
      </div>
    </div>
  )
}
