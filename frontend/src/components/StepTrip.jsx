import { useI18n } from '../i18n/I18nContext'
import StepNav from './StepNav'

function LegFields({ leg, minDate, onChange, t }) {
  return (
    <div className="space-y-4">
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-semibold mb-1">{t('stepTrip.pickupAddress')}</label>
          <input
            type="text"
            value={leg.pickupAddress}
            onChange={(e) => onChange('pickupAddress', e.target.value)}
            placeholder={t('stepTrip.pickupPlaceholder')}
            className="w-full border border-gray-300 rounded-lg px-4 py-3 focus:border-th-yellow focus:ring-2 focus:ring-th-yellow/30 outline-none"
          />
        </div>
        <div>
          <label className="block text-sm font-semibold mb-1">{t('stepTrip.dropoffAddress')}</label>
          <input
            type="text"
            value={leg.dropoffAddress}
            onChange={(e) => onChange('dropoffAddress', e.target.value)}
            placeholder={t('stepTrip.dropoffPlaceholder')}
            className="w-full border border-gray-300 rounded-lg px-4 py-3 focus:border-th-yellow focus:ring-2 focus:ring-th-yellow/30 outline-none"
          />
        </div>
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-semibold mb-1">{t('stepTrip.date')}</label>
          <input
            type="date"
            min={minDate}
            value={leg.date}
            onChange={(e) => onChange('date', e.target.value)}
            className="w-full border border-gray-300 rounded-lg px-4 py-3 focus:border-th-yellow focus:ring-2 focus:ring-th-yellow/30 outline-none"
          />
        </div>
        <div>
          <label className="block text-sm font-semibold mb-1">{t('stepTrip.time')}</label>
          <input
            type="time"
            value={leg.time}
            onChange={(e) => onChange('time', e.target.value)}
            className="w-full border border-gray-300 rounded-lg px-4 py-3 focus:border-th-yellow focus:ring-2 focus:ring-th-yellow/30 outline-none"
          />
        </div>
      </div>
    </div>
  )
}

export default function StepTrip({ state, dispatch }) {
  const { t } = useI18n()
  const minDate = new Date().toISOString().slice(0, 10)

  const outboundValid =
    state.outboundLeg.pickupAddress.trim() &&
    state.outboundLeg.dropoffAddress.trim() &&
    state.outboundLeg.date &&
    state.outboundLeg.time

  const returnValid =
    state.tripType === 'ONE_WAY' ||
    (state.returnLeg.pickupAddress.trim() &&
      state.returnLeg.dropoffAddress.trim() &&
      state.returnLeg.date &&
      state.returnLeg.time)

  return (
    <div>
      <h2 className="text-2xl font-bold mb-1">{t('stepTrip.title')}</h2>
      <p className="text-gray-500 mb-6">{t('stepTrip.subtitle')}</p>

      <div className="flex gap-3 mb-6">
        <button
          type="button"
          onClick={() => dispatch({ type: 'SET_TRIP_TYPE', value: 'ONE_WAY' })}
          className={`flex-1 py-3 rounded-lg font-semibold border-2 transition ${
            state.tripType === 'ONE_WAY' ? 'border-th-yellow bg-th-yellow/10' : 'border-gray-200 text-gray-500'
          }`}
        >
          {t('stepTrip.oneWay')}
        </button>
        <button
          type="button"
          onClick={() => dispatch({ type: 'SET_TRIP_TYPE', value: 'ROUND_TRIP' })}
          className={`flex-1 py-3 rounded-lg font-semibold border-2 transition ${
            state.tripType === 'ROUND_TRIP' ? 'border-th-yellow bg-th-yellow/10' : 'border-gray-200 text-gray-500'
          }`}
        >
          {t('stepTrip.roundTrip')}
        </button>
      </div>

      <h3 className="font-bold text-th-black mb-3">{t('stepTrip.outbound')}</h3>
      <LegFields
        leg={state.outboundLeg}
        minDate={minDate}
        t={t}
        onChange={(field, value) => dispatch({ type: 'SET_LEG_FIELD', leg: 'outbound', field, value })}
      />

      {state.tripType === 'ROUND_TRIP' && (
        <>
          <h3 className="font-bold text-th-black mt-8 mb-3">{t('stepTrip.return')}</h3>
          <LegFields
            leg={state.returnLeg}
            minDate={state.outboundLeg.date || minDate}
            t={t}
            onChange={(field, value) => dispatch({ type: 'SET_LEG_FIELD', leg: 'return', field, value })}
          />
        </>
      )}

      <StepNav
        onBack={() => dispatch({ type: 'GO_TO_STEP', index: 0 })}
        onNext={() => dispatch({ type: 'NEXT_STEP' })}
        nextDisabled={!outboundValid || !returnValid}
        error={!outboundValid || !returnValid ? t('stepTrip.validationError') : null}
      />
    </div>
  )
}
