export const STEPS = ['hero', 'trip', 'vehicle', 'extras', 'contact', 'review', 'success']

const STORAGE_KEY = 'taxihub.publicBooking.v1'

const emptyLeg = () => ({
  pickupAddress: '',
  dropoffAddress: '',
  date: '',
  time: '',
  waterQty: 0,
  colaQty: 0,
  sodaLemonadeQty: 0,
  orangeJuiceQty: 0,
})

export const initialState = {
  stepIndex: 0,
  tripType: 'ONE_WAY',
  outboundLeg: emptyLeg(),
  returnLeg: emptyLeg(),
  vehicleType: 'STANDARD',
  passengerCount: 1,
  luggageCount: 1,
  babySeatCount: 0,
  childSeatCount: 0,
  boosterSeatCount: 0,
  firstName: '',
  lastName: '',
  email: '',
  phoneNumber: '',
  customerMessage: '',
  paymentMethod: '',
  outboundQuote: null,
  returnQuote: null,
  submitError: null,
  submitting: false,
  result: null,
}

export function loadPersistedState() {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    if (!raw) return initialState
    const parsed = JSON.parse(raw)
    // Never resume mid-submit or on the success screen from a stale reload -
    // those are one-shot transitions, not data the customer typed.
    return { ...initialState, ...parsed, submitting: false, result: null }
  } catch {
    return initialState
  }
}

export function persistState(state) {
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(state))
  } catch {
    // Private browsing / storage full - the wizard still works, it just
    // won't survive a refresh. Not worth surfacing to the customer.
  }
}

export function clearPersistedState() {
  try {
    window.localStorage.removeItem(STORAGE_KEY)
  } catch {
    /* ignore */
  }
}

export function bookingReducer(state, action) {
  switch (action.type) {
    case 'SET_FIELD':
      return { ...state, [action.field]: action.value }

    case 'SET_LEG_FIELD': {
      const key = action.leg === 'outbound' ? 'outboundLeg' : 'returnLeg'
      return { ...state, [key]: { ...state[key], [action.field]: action.value } }
    }

    case 'SET_TRIP_TYPE': {
      if (action.value === 'ROUND_TRIP' && !state.returnLeg.pickupAddress && !state.returnLeg.dropoffAddress) {
        // Sensible default: return trip reverses the outbound route. Still
        // fully editable afterwards.
        return {
          ...state,
          tripType: action.value,
          returnLeg: {
            ...state.returnLeg,
            pickupAddress: state.outboundLeg.dropoffAddress,
            dropoffAddress: state.outboundLeg.pickupAddress,
          },
        }
      }
      return { ...state, tripType: action.value }
    }

    case 'SET_QUOTE':
      return { ...state, [action.leg === 'outbound' ? 'outboundQuote' : 'returnQuote']: action.quote }

    case 'GO_TO_STEP':
      return { ...state, stepIndex: action.index }

    case 'NEXT_STEP':
      return { ...state, stepIndex: Math.min(state.stepIndex + 1, STEPS.length - 1) }

    case 'PREV_STEP':
      return { ...state, stepIndex: Math.max(state.stepIndex - 1, 0) }

    case 'SUBMIT_START':
      return { ...state, submitting: true, submitError: null }

    case 'SUBMIT_SUCCESS':
      return { ...state, submitting: false, result: action.result, stepIndex: STEPS.length - 1 }

    case 'SUBMIT_ERROR':
      return { ...state, submitting: false, submitError: action.message }

    case 'RESET':
      clearPersistedState()
      return initialState

    default:
      return state
  }
}
