import { useI18n } from '../i18n/I18nContext'

// stepIndex is 1..5 within the STEPS array (0=hero, 6=success) - map to 0..4.
export default function StepIndicator({ stepIndex }) {
  const { t } = useI18n()
  const labels = [
    t('stepIndicator.trip'),
    t('stepIndicator.vehicle'),
    t('stepIndicator.extras'),
    t('stepIndicator.contact'),
    t('stepIndicator.review'),
  ]
  const active = stepIndex - 1

  return (
    <ol className="flex items-start mb-8">
      {labels.map((label, i) => (
        <li key={label} className="flex items-center flex-1 last:flex-none">
          <div className="flex flex-col items-center w-16">
            <div
              className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold border-2 ${
                i < active
                  ? 'bg-th-yellow border-th-yellow text-th-black'
                  : i === active
                    ? 'border-th-yellow text-th-yellow-dark'
                    : 'border-gray-300 text-gray-300'
              }`}
            >
              {i + 1}
            </div>
            <span
              className={`mt-1 text-[11px] sm:text-xs text-center leading-tight ${
                i === active ? 'text-th-black font-semibold' : 'text-gray-400'
              }`}
            >
              {label}
            </span>
          </div>
          {i < labels.length - 1 && (
            <div className={`h-0.5 flex-1 mt-4 ${i < active ? 'bg-th-yellow' : 'bg-gray-200'}`} />
          )}
        </li>
      ))}
    </ol>
  )
}
