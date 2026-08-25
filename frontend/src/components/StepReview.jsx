import { submitPublicBooking } from '../api/client'
import { useI18n } from '../i18n/I18nContext'

const CHILD_SEAT_ROWS = [
  { field: 'babySeatCount', labelKey: 'stepExtras.babySeat' },
  { field: 'childSeatCount', labelKey: 'stepExtras.childSeat' },
  { field: 'boosterSeatCount', labelKey: 'stepExtras.boosterSeat' },
]

const MINIBAR_ROWS = [
  { field: 'waterQty', labelKey: 'stepExtras.water' },
  { field: 'colaQty', labelKey: 'stepExtras.cola' },
  { field: 'sodaLemonadeQty', labelKey: 'stepExtras.sodaLemonade' },
  { field: 'orangeJuiceQty', labelKey: 'stepExtras.orangeJuice' },
]

function ReviewSection({ title, onEdit, editLabel, children }) {
  return (
    <div className="border border-gray-200 rounded-xl p-4 mb-4">
      <div className="flex justify-between items-center mb-2">
        <h3 className="font-bold">{title}</h3>
        <button type="button" onClick={onEdit} className="text-sm text-th-yellow-dark font-semibold hover:underline">
          {editLabel}
        </button>
      </div>
      <div className="text-sm text-gray-700 space-y-1">{children}</div>
    </div>
  )
}

function extrasSummary(state, t) {
  const items = []
  CHILD_SEAT_ROWS.forEach((row) => {
    if (state[row.field] > 0) items.push(`${t(row.labelKey)} x${state[row.field]}`)
  })
  MINIBAR_ROWS.forEach((row) => {
    if (state.outboundLeg[row.field] > 0) {
      const suffix = state.tripType === 'ROUND_TRIP' ? ` (${t('stepReview.outboundLabel')})` : ''
      items.push(`${t(row.labelKey)} x${state.outboundLeg[row.field]}${suffix}`)
    }
  })
  if (state.tripType === 'ROUND_TRIP') {
    MINIBAR_ROWS.forEach((row) => {
      if (state.returnLeg[row.field] > 0) {
        items.push(`${t(row.labelKey)} x${state.returnLeg[row.field]} (${t('stepReview.returnLabel')})`)
      }
    })
  }
  return items
}

export default function StepReview({ state, dispatch }) {
  const { t } = useI18n()

  async function handleSubmit() {
    dispatch({ type: 'SUBMIT_START' })
    try {
      const payload = {
        tripType: state.tripType,
        firstName: state.firstName,
        lastName: state.lastName,
        email: state.email,
        phoneNumber: state.phoneNumber,
        customerMessage: state.customerMessage,
        vehicleType: state.vehicleType,
        passengerCount: state.passengerCount,
        luggageCount: state.luggageCount,
        paymentMethod: state.paymentMethod || null,
        babySeatCount: state.babySeatCount,
        childSeatCount: state.childSeatCount,
        boosterSeatCount: state.boosterSeatCount,
        outboundLeg: state.outboundLeg,
        returnLeg: state.tripType === 'ROUND_TRIP' ? state.returnLeg : null,
      }
      const result = await submitPublicBooking(payload)
      dispatch({ type: 'SUBMIT_SUCCESS', result })
    } catch (err) {
      dispatch({ type: 'SUBMIT_ERROR', message: err.message || t('stepReview.genericError') })
    }
  }

  const extras = extrasSummary(state, t)

  return (
    <div>
      <h2 className="text-2xl font-bold mb-1">{t('stepReview.title')}</h2>
      <p className="text-gray-500 mb-6">{t('stepReview.subtitle')}</p>

      <ReviewSection title={t('stepReview.sectionRoute')} editLabel={t('stepReview.edit')} onEdit={() => dispatch({ type: 'GO_TO_STEP', index: 1 })}>
        <p>
          <strong>{t('stepReview.outboundLabel')}:</strong> {state.outboundLeg.pickupAddress} → {state.outboundLeg.dropoffAddress} (
          {state.outboundLeg.date} {state.outboundLeg.time})
        </p>
        {state.tripType === 'ROUND_TRIP' && (
          <p>
            <strong>{t('stepReview.returnLabel')}:</strong> {state.returnLeg.pickupAddress} → {state.returnLeg.dropoffAddress} (
            {state.returnLeg.date} {state.returnLeg.time})
          </p>
        )}
      </ReviewSection>

      <ReviewSection title={t('stepReview.sectionVehicle')} editLabel={t('stepReview.edit')} onEdit={() => dispatch({ type: 'GO_TO_STEP', index: 2 })}>
        <p>
          {t(`stepVehicle.vehicle.${state.vehicleType}.label`)} · {state.passengerCount} {t('stepReview.passengerUnit')} ·{' '}
          {state.luggageCount} {t('stepReview.luggageUnit')}
        </p>
      </ReviewSection>

      <ReviewSection title={t('stepReview.sectionExtras')} editLabel={t('stepReview.edit')} onEdit={() => dispatch({ type: 'GO_TO_STEP', index: 3 })}>
        <p>{extras.length > 0 ? extras.join(', ') : t('stepReview.noExtras')}</p>
      </ReviewSection>

      <ReviewSection title={t('stepReview.sectionContact')} editLabel={t('stepReview.edit')} onEdit={() => dispatch({ type: 'GO_TO_STEP', index: 4 })}>
        <p>
          {state.firstName} {state.lastName} · {state.email} · {state.phoneNumber}
        </p>
        {state.paymentMethod && (
          <p>
            {t('stepReview.payment')}: {t(`stepContact.payment.${state.paymentMethod}`)}
          </p>
        )}
        {state.customerMessage && (
          <p className="text-gray-500">
            {t('stepReview.note')}: {state.customerMessage}
          </p>
        )}
      </ReviewSection>

      {state.submitError && <p className="text-th-red text-sm mb-4">{state.submitError}</p>}

      <div className="mt-8 flex justify-between">
        <button
          type="button"
          onClick={() => dispatch({ type: 'PREV_STEP' })}
          className="px-6 py-3 rounded-full border border-gray-300 font-semibold hover:bg-gray-50"
        >
          {t('stepNav.back')}
        </button>
        <button
          type="button"
          onClick={handleSubmit}
          disabled={state.submitting}
          className="px-8 py-3 rounded-full bg-th-yellow text-th-black font-bold disabled:opacity-40 hover:bg-th-yellow-dark transition"
        >
          {state.submitting ? t('stepReview.submitting') : t('stepReview.submit')}
        </button>
      </div>
    </div>
  )
}
