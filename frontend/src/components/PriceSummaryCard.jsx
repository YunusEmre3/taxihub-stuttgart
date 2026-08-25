import { useI18n } from '../i18n/I18nContext'

export default function PriceSummaryCard({ tripPrice, extrasPrice }) {
  const { t } = useI18n()
  const total = (tripPrice ?? 0) + (extrasPrice ?? 0)

  return (
    <div className="bg-th-black text-white rounded-xl p-5 mb-6 border-2 border-th-yellow">
      <div className="flex justify-between text-sm text-gray-300 mb-1">
        <span>{t('priceSummary.trip')}</span>
        <span>{(tripPrice ?? 0).toFixed(2)}€</span>
      </div>
      <div className="flex justify-between text-sm text-gray-300 mb-3">
        <span>{t('priceSummary.extras')}</span>
        <span>{(extrasPrice ?? 0).toFixed(2)}€</span>
      </div>
      <div className="flex justify-between items-center pt-3 border-t border-white/20">
        <span className="text-xs uppercase tracking-wide text-th-yellow font-bold">{t('priceSummary.total')}</span>
        <span className="text-2xl font-extrabold text-th-yellow">{total.toFixed(2)}€</span>
      </div>
    </div>
  )
}
