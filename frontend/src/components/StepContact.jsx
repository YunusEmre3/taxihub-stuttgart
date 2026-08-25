import { useI18n } from '../i18n/I18nContext'
import StepNav from './StepNav'

const PAYMENT_METHODS = ['CASH', 'CARD', 'INVOICE']

function Field({ label, value, onChange, type = 'text', placeholder }) {
  return (
    <div>
      <label className="block text-sm font-semibold mb-1">{label}</label>
      <input
        type={type}
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        className="w-full border border-gray-300 rounded-lg px-4 py-3 focus:border-th-yellow focus:ring-2 focus:ring-th-yellow/30 outline-none"
      />
    </div>
  )
}

export default function StepContact({ state, dispatch }) {
  const { t } = useI18n()
  const valid = state.firstName.trim() && state.lastName.trim() && state.email.trim() && state.phoneNumber.trim()

  const set = (field) => (e) => dispatch({ type: 'SET_FIELD', field, value: e.target.value })

  return (
    <div>
      <h2 className="text-2xl font-bold mb-1">{t('stepContact.title')}</h2>
      <p className="text-gray-500 mb-6">{t('stepContact.subtitle')}</p>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-4">
        <Field label={t('stepContact.firstName')} value={state.firstName} onChange={set('firstName')} />
        <Field label={t('stepContact.lastName')} value={state.lastName} onChange={set('lastName')} />
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-4">
        <Field label={t('stepContact.email')} type="email" value={state.email} onChange={set('email')} />
        <Field label={t('stepContact.phone')} value={state.phoneNumber} onChange={set('phoneNumber')} placeholder="+49 ..." />
      </div>

      <div className="mb-4">
        <label className="block text-sm font-semibold mb-1">{t('stepContact.paymentMethod')}</label>
        <select
          value={state.paymentMethod}
          onChange={set('paymentMethod')}
          className="w-full border border-gray-300 rounded-lg px-4 py-3"
        >
          <option value="">{t('stepContact.select')}</option>
          {PAYMENT_METHODS.map((method) => (
            <option key={method} value={method}>
              {t(`stepContact.payment.${method}`)}
            </option>
          ))}
        </select>
      </div>

      <div className="mb-4">
        <label className="block text-sm font-semibold mb-1">{t('stepContact.note')}</label>
        <textarea
          value={state.customerMessage}
          onChange={set('customerMessage')}
          rows={3}
          placeholder={t('stepContact.notePlaceholder')}
          className="w-full border border-gray-300 rounded-lg px-4 py-3"
        />
      </div>

      <StepNav
        onBack={() => dispatch({ type: 'PREV_STEP' })}
        onNext={() => dispatch({ type: 'NEXT_STEP' })}
        nextDisabled={!valid}
        error={!valid ? t('stepContact.validationError') : null}
      />
    </div>
  )
}
