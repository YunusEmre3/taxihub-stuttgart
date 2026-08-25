import { useEffect, useState } from 'react'
import { fetchPricing, calculateRoute } from '../api/client'
import { useI18n } from '../i18n/I18nContext'
import StepNav from './StepNav'

const VEHICLE_ICONS = {
  STANDARD: '🚕',
  COMFORT: '🚗',
  VAN: '🚐',
  BUSINESS: '🚙',
}

export default function StepVehicle({ state, dispatch }) {
  const { t } = useI18n()
  const [pricing, setPricing] = useState(null)
  const [loadingQuote, setLoadingQuote] = useState(false)

  useEffect(() => {
    fetchPricing().then(setPricing).catch(() => setPricing(null))
  }, [])

  useEffect(() => {
    let cancelled = false

    async function loadQuotes() {
      if (!state.outboundLeg.pickupAddress || !state.outboundLeg.dropoffAddress) return
      setLoadingQuote(true)
      try {
        const outboundQuote = await calculateRoute(
          state.outboundLeg.pickupAddress, state.outboundLeg.dropoffAddress, state.vehicleType)
        if (!cancelled) dispatch({ type: 'SET_QUOTE', leg: 'outbound', quote: outboundQuote })

        if (state.tripType === 'ROUND_TRIP' && state.returnLeg.pickupAddress && state.returnLeg.dropoffAddress) {
          const returnQuote = await calculateRoute(
            state.returnLeg.pickupAddress, state.returnLeg.dropoffAddress, state.vehicleType)
          if (!cancelled) dispatch({ type: 'SET_QUOTE', leg: 'return', quote: returnQuote })
        }
      } finally {
        if (!cancelled) setLoadingQuote(false)
      }
    }

    loadQuotes()
    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [
    state.vehicleType,
    state.outboundLeg.pickupAddress,
    state.outboundLeg.dropoffAddress,
    state.returnLeg.pickupAddress,
    state.returnLeg.dropoffAddress,
    state.tripType,
  ])

  const totalEstimate =
    (state.outboundQuote?.success ? state.outboundQuote.estimatedPrice : 0) +
    (state.tripType === 'ROUND_TRIP' && state.returnQuote?.success ? state.returnQuote.estimatedPrice : 0)

  return (
    <div>
      <h2 className="text-2xl font-bold mb-1">{t('stepVehicle.title')}</h2>
      <p className="text-gray-500 mb-6">{t('stepVehicle.subtitle')}</p>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-8">
        {(pricing?.vehicleTypes ?? []).map((row) => {
          const selected = state.vehicleType === row.vehicleType
          return (
            <button
              key={row.vehicleType}
              type="button"
              onClick={() => dispatch({ type: 'SET_FIELD', field: 'vehicleType', value: row.vehicleType })}
              className={`text-left p-4 rounded-xl border-2 transition ${
                selected ? 'border-th-yellow bg-th-yellow/10' : 'border-gray-200 hover:border-gray-300'
              }`}
            >
              <div className="text-3xl mb-2">{VEHICLE_ICONS[row.vehicleType] ?? '🚗'}</div>
              <div className="font-bold">{t(`stepVehicle.vehicle.${row.vehicleType}.label`)}</div>
              <div className="text-sm text-gray-500 mb-2">{t(`stepVehicle.vehicle.${row.vehicleType}.desc`)}</div>
              <div className="text-sm text-th-black font-semibold">
                {row.baseFare.toFixed(2)}€ + {row.pricePerKm.toFixed(2)}€/km
              </div>
            </button>
          )
        })}
      </div>

      <div className="grid grid-cols-2 gap-4 mb-8">
        <div>
          <label className="block text-sm font-semibold mb-1">{t('stepVehicle.passengerCount')}</label>
          <input
            type="number"
            min={1}
            max={8}
            value={state.passengerCount}
            onChange={(e) => dispatch({ type: 'SET_FIELD', field: 'passengerCount', value: Number(e.target.value) })}
            className="w-full border border-gray-300 rounded-lg px-4 py-3"
          />
        </div>
        <div>
          <label className="block text-sm font-semibold mb-1">{t('stepVehicle.luggageCount')}</label>
          <input
            type="number"
            min={0}
            max={10}
            value={state.luggageCount}
            onChange={(e) => dispatch({ type: 'SET_FIELD', field: 'luggageCount', value: Number(e.target.value) })}
            className="w-full border border-gray-300 rounded-lg px-4 py-3"
          />
        </div>
      </div>

      <div className="bg-th-light-gray rounded-xl p-4 mb-8">
        {loadingQuote && <p className="text-sm text-gray-500">{t('stepVehicle.calculating')}</p>}
        {!loadingQuote && state.outboundQuote && !state.outboundQuote.success && (
          <p className="text-sm text-th-red">{state.outboundQuote.errorMessage}</p>
        )}
        {!loadingQuote && state.outboundQuote?.success && (
          <div className="flex justify-between items-center">
            <span className="text-sm text-gray-600">{t('stepVehicle.priceLabel')}</span>
            <span className="text-xl font-bold text-th-black">{totalEstimate.toFixed(2)}€</span>
          </div>
        )}
      </div>

      <StepNav onBack={() => dispatch({ type: 'PREV_STEP' })} onNext={() => dispatch({ type: 'NEXT_STEP' })} />
    </div>
  )
}
