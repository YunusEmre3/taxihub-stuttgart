import { useEffect, useState } from 'react'
import { fetchContact } from '../api/client'
import { useI18n } from '../i18n/I18nContext'

export default function SuccessScreen({ state, dispatch }) {
  const { t } = useI18n()
  const [contact, setContact] = useState(null)
  const [copiedCode, setCopiedCode] = useState(null)

  useEffect(() => {
    fetchContact().then(setContact).catch(() => setContact(null))
  }, [])

  const result = state.result
  if (!result) {
    return null
  }

  const legLabel = (legType) =>
    legType === 'OUTBOUND' ? t('success.outboundLabel') : legType === 'RETURN' ? t('success.returnLabel') : null

  const codesText = result.legs
    .map((leg) => `${legLabel(leg.legType) ? legLabel(leg.legType) + ': ' : ''}${leg.bookingCode}`)
    .join(', ')
  const summaryText = `Merhaba, ${codesText} kodlu rezervasyonum hakkında bilgi almak istiyorum.`

  const whatsappHref = contact
    ? `https://wa.me/${contact.whatsappNumber}?text=${encodeURIComponent(summaryText)}`
    : '#'
  const mailHref = contact
    ? `mailto:${contact.email}?subject=${encodeURIComponent('Rezervasyon - ' + codesText)}&body=${encodeURIComponent(summaryText)}`
    : '#'

  function copyCode(code) {
    navigator.clipboard.writeText(code).then(() => {
      setCopiedCode(code)
      setTimeout(() => setCopiedCode((current) => (current === code ? null : current)), 1500)
    })
  }

  return (
    <div className="max-w-xl mx-auto px-4 py-20 text-center">
      <div className="text-5xl mb-4">✅</div>
      <h2 className="text-3xl font-bold mb-2">{t('success.title')}</h2>
      <p className="text-gray-500 mb-8">{t('success.subtitle')}</p>

      <div className="bg-th-light-gray rounded-xl p-6 mb-8 text-left">
        {result.legs.map((leg) => (
          <div key={leg.bookingCode} className="flex justify-between items-center py-2 border-b border-gray-200 last:border-0">
            <span className="text-gray-600">
              {legLabel(leg.legType) ? legLabel(leg.legType) + ' · ' : ''}
              {leg.date} {leg.time}
            </span>
            <span className="flex items-center gap-2">
              <span className="font-bold">{leg.bookingCode}</span>
              <button
                type="button"
                onClick={() => copyCode(leg.bookingCode)}
                className="text-xs font-semibold text-th-yellow-dark border border-th-yellow rounded-full px-2 py-1 hover:bg-th-yellow/10 transition"
              >
                {copiedCode === leg.bookingCode ? t('success.copied') : t('success.copy')}
              </button>
            </span>
          </div>
        ))}
        <div className="flex justify-between pt-3 font-bold text-lg">
          <span>{t('success.total')}</span>
          <span>{result.totalEstimatedPrice?.toFixed(2)}€</span>
        </div>
      </div>

      <div className="border-2 border-th-yellow rounded-xl p-6 mb-8">
        <h3 className="font-bold mb-1">{t('success.contactTitle')}</h3>
        <p className="text-sm text-gray-500 mb-4">{t('success.contactSubtitle')}</p>
        <div className="flex flex-col sm:flex-row gap-3 justify-center">
          <a
            href={whatsappHref}
            target="_blank"
            rel="noreferrer"
            className="flex-1 bg-[#25D366] text-white font-semibold px-6 py-3 rounded-full hover:opacity-90 transition"
          >
            {t('success.whatsapp')}
          </a>
          <a
            href={mailHref}
            className="flex-1 bg-th-black text-white font-semibold px-6 py-3 rounded-full hover:opacity-90 transition"
          >
            {t('success.email')}
          </a>
        </div>
        {contact && (
          <p className="text-xs text-gray-400 mt-4">
            {contact.phone} · {contact.address}
          </p>
        )}
      </div>

      <button
        type="button"
        onClick={() => dispatch({ type: 'RESET' })}
        className="text-th-yellow-dark font-semibold hover:underline"
      >
        {t('success.newBooking')}
      </button>
    </div>
  )
}
