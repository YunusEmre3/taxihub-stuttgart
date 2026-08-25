import { useEffect, useReducer } from 'react'
import { bookingReducer, loadPersistedState, persistState, STEPS } from './state/bookingReducer'
import { I18nProvider, useI18n } from './i18n/I18nContext'
import StaffLoginLink from './components/StaffLoginLink'
import LanguageSwitcher from './components/LanguageSwitcher'
import Hero from './components/Hero'
import TrustStrip from './components/TrustStrip'
import StepIndicator from './components/StepIndicator'
import StepTrip from './components/StepTrip'
import StepVehicle from './components/StepVehicle'
import StepExtras from './components/StepExtras'
import StepContact from './components/StepContact'
import StepReview from './components/StepReview'
import SuccessScreen from './components/SuccessScreen'

function AppShell() {
  const [state, dispatch] = useReducer(bookingReducer, undefined, loadPersistedState)
  const { locale } = useI18n()

  useEffect(() => {
    persistState(state)
  }, [state])

  useEffect(() => {
    document.documentElement.lang = locale
  }, [locale])

  const step = STEPS[state.stepIndex]

  return (
    <div className="min-h-screen bg-white text-th-black">
      <div className="fixed top-4 right-4 z-50 flex items-center gap-2">
        <LanguageSwitcher dark={step === 'hero'} />
        <StaffLoginLink />
      </div>

      {step === 'hero' && (
        <>
          <Hero onStart={() => dispatch({ type: 'GO_TO_STEP', index: 1 })} />
          <TrustStrip />
        </>
      )}

      {step !== 'hero' && step !== 'success' && (
        <div className="max-w-3xl mx-auto px-4 py-16">
          <StepIndicator stepIndex={state.stepIndex} />
          {step === 'trip' && <StepTrip state={state} dispatch={dispatch} />}
          {step === 'vehicle' && <StepVehicle state={state} dispatch={dispatch} />}
          {step === 'extras' && <StepExtras state={state} dispatch={dispatch} />}
          {step === 'contact' && <StepContact state={state} dispatch={dispatch} />}
          {step === 'review' && <StepReview state={state} dispatch={dispatch} />}
        </div>
      )}

      {step === 'success' && <SuccessScreen state={state} dispatch={dispatch} />}
    </div>
  )
}

export default function App() {
  return (
    <I18nProvider>
      <AppShell />
    </I18nProvider>
  )
}
