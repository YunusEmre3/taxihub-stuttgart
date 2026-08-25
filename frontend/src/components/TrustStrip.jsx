import taxiImage from '../assets/taxi-cab.jpg'
import { useI18n } from '../i18n/I18nContext'

export default function TrustStrip() {
  const { t } = useI18n()
  const points = [
    { icon: '🕐', title: t('trust.point1.title'), desc: t('trust.point1.desc') },
    { icon: '✅', title: t('trust.point2.title'), desc: t('trust.point2.desc') },
    { icon: '💶', title: t('trust.point3.title'), desc: t('trust.point3.desc') },
  ]

  return (
    <section className="max-w-5xl mx-auto px-4 py-16 grid grid-cols-1 md:grid-cols-2 gap-10 items-center">
      <div className="rounded-2xl overflow-hidden shadow-lg">
        <img src={taxiImage} alt="Stuttgart taksi" className="w-full h-64 object-cover" />
      </div>
      <div className="space-y-6">
        {points.map((point) => (
          <div key={point.title} className="flex gap-4">
            <div className="text-2xl">{point.icon}</div>
            <div>
              <div className="font-bold text-th-black">{point.title}</div>
              <div className="text-sm text-gray-500">{point.desc}</div>
            </div>
          </div>
        ))}
      </div>
    </section>
  )
}
