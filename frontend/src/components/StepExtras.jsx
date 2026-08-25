import { useEffect, useState } from 'react'
import { fetchPricing } from '../api/client'
import { useI18n } from '../i18n/I18nContext'
import PriceSummaryCard from './PriceSummaryCard'
import StepNav from './StepNav'

const CHILD_SEAT_ROWS = [
  { code: 'BABY_SEAT', field: 'babySeatCount', labelKey: 'stepExtras.babySeat' },
  { code: 'CHILD_SEAT', field: 'childSeatCount', labelKey: 'stepExtras.childSeat' },
  { code: 'BOOSTER_SEAT', field: 'boosterSeatCount', labelKey: 'stepExtras.boosterSeat' },
]

const MINIBAR_ROWS = [
  { code: 'WATER', field: 'waterQty', labelKey: 'stepExtras.water' },
  { code: 'COLA', field: 'colaQty', labelKey: 'stepExtras.cola' },
  { code: 'SODA_LEMONADE', field: 'sodaLemonadeQty', labelKey: 'stepExtras.sodaLemonade' },
  { code: 'ORANGE_JUICE', field: 'orangeJuiceQty', labelKey: 'stepExtras.orangeJuice' },
]

function QuantityInput({ value, onChange, max = 8 }) {
  return (
    <input
      type="number"
      min={0}
      max={max}
      value={value}
      onChange={(e) => onChange(Math.max(0, Math.min(max, Number(e.target.value) || 0)))}
      className="w-20 border border-gray-300 rounded-lg px-3 py-2 text-center"
    />
  )
}

function MinibarGroup({ title, leg, legKey, dispatch, priceOf, t }) {
  return (
    <div className="mb-4">
      {title && <h3 className="font-bold mb-2">{title}</h3>}
      <div className="space-y-3">
        {MINIBAR_ROWS.map((row) => (
          <div key={row.code} className="flex items-center justify-between p-3 rounded-lg border border-gray-200">
            <span>{t(row.labelKey)}</span>
            <div className="flex items-center gap-3">
              <span className="text-sm text-gray-500 w-14 text-right">{priceOf(row.code).toFixed(2)}€</span>
              <QuantityInput
                value={leg[row.field]}
                onChange={(value) => dispatch({ type: 'SET_LEG_FIELD', leg: legKey, field: row.field, value })}
              />
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

export default function StepExtras({ state, dispatch }) {
  const { t } = useI18n()
  const [extras, setExtras] = useState([])

  useEffect(() => {
    fetchPricing().then((data) => setExtras(data.extras ?? [])).catch(() => setExtras([]))
  }, [])

  const priceOf = (code) => extras.find((e) => e.code === code)?.price ?? 0

  const childSeatTotal = CHILD_SEAT_ROWS.reduce((sum, row) => sum + state[row.field] * priceOf(row.code), 0)
  const outboundMinibarTotal = MINIBAR_ROWS.reduce((sum, row) => sum + state.outboundLeg[row.field] * priceOf(row.code), 0)
  const returnMinibarTotal =
    state.tripType === 'ROUND_TRIP'
      ? MINIBAR_ROWS.reduce((sum, row) => sum + state.returnLeg[row.field] * priceOf(row.code), 0)
      : 0
  const extrasTotal = childSeatTotal + outboundMinibarTotal + returnMinibarTotal

  const tripPrice =
    (state.outboundQuote?.success ? state.outboundQuote.estimatedPrice : 0) +
    (state.tripType === 'ROUND_TRIP' && state.returnQuote?.success ? state.returnQuote.estimatedPrice : 0)

  return (
    <div>
      <h2 className="text-2xl font-bold mb-1">{t('stepExtras.title')}</h2>
      <p className="text-gray-500 mb-6">{t('stepExtras.subtitle')}</p>

      <PriceSummaryCard tripPrice={tripPrice} extrasPrice={extrasTotal} />

      <div className="mb-8">
        <h3 className="font-bold mb-1 text-xs uppercase tracking-wide text-gray-400">{t('stepExtras.childSeatSection')}</h3>
        <p className="text-xs text-gray-400 mb-3">{t('stepExtras.childSeatHint')}</p>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          {CHILD_SEAT_ROWS.map((row) => (
            <div key={row.code} className="p-3 rounded-lg border border-gray-200">
              <div className="text-sm font-semibold mb-1">{t(row.labelKey)}</div>
              <div className="flex items-center justify-between">
                <span className="text-th-green text-xs font-semibold">{t('stepExtras.free')}</span>
                <QuantityInput
                  value={state[row.field]}
                  onChange={(value) => dispatch({ type: 'SET_FIELD', field: row.field, value })}
                />
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="mb-2">
        <h3 className="font-bold mb-1 text-xs uppercase tracking-wide text-gray-400">{t('stepExtras.minibarSection')}</h3>
        <p className="text-xs text-gray-400 mb-3">{t('stepExtras.minibarHint')}</p>
      </div>

      <MinibarGroup
        title={state.tripType === 'ROUND_TRIP' ? t('stepTrip.outbound') : null}
        leg={state.outboundLeg}
        legKey="outbound"
        dispatch={dispatch}
        priceOf={priceOf}
        t={t}
      />

      {state.tripType === 'ROUND_TRIP' && (
        <MinibarGroup
          title={t('stepTrip.return')}
          leg={state.returnLeg}
          legKey="return"
          dispatch={dispatch}
          priceOf={priceOf}
          t={t}
        />
      )}

      <div className="bg-th-light-gray rounded-xl p-4 my-6 flex justify-between items-center">
        <span className="text-sm text-gray-600">{t('stepExtras.extrasSubtotal')}</span>
        <span className="text-lg font-bold">{extrasTotal.toFixed(2)}€</span>
      </div>

      <div className="flex flex-wrap gap-x-5 gap-y-2 text-xs text-gray-400 font-semibold mb-2">
        <span>✓ {t('trust.badge.fixedPrice')}</span>
        <span>✓ {t('trust.badge.noHiddenFees')}</span>
        <span>✓ {t('trust.badge.payInCar')}</span>
        <span>✓ {t('trust.badge.available247')}</span>
      </div>

      <StepNav onBack={() => dispatch({ type: 'PREV_STEP' })} onNext={() => dispatch({ type: 'NEXT_STEP' })} />
    </div>
  )
}
