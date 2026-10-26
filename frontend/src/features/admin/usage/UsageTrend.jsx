import { useI18n } from '../../../shared/i18n/useI18n.js'
export default function UsageTrend({ data, unit }) {
  const { t, locale } = useI18n()
  const date = value => new Intl.DateTimeFormat(locale === 'vi' ? 'vi-VN' : 'en-GB', { timeZone: 'UTC', month: 'short', day: 'numeric', ...(unit === 'hour' ? { hour: '2-digit' } : {}) }).format(new Date(value))
  const max = Math.max(1, ...data.map(bucket => bucket.requests))
  return <section className="cs-surface usage-trend"><h2>{t('Request volume over time')}</h2><p className="cs-caption">{t(unit === 'hour' ? 'Hourly buckets, UTC. Only buckets with requests are shown.' : 'Daily buckets, UTC. Only buckets with requests are shown.')}</p>
    <figure><div className={'usage-bars' + (data.length > 40 ? ' usage-bars--dense' : '')} role="img" aria-label={t('Request volume chart. Exact values are available below.')}>
      {data.map(bucket => <div className="usage-bar" key={bucket.startedAt} title={date(bucket.startedAt) + ': ' + bucket.requests} style={{ height: Math.max(2, bucket.requests / max * 100) + '%' }} />)}
    </div><figcaption className="usage-trend-range"><span>{data.length ? date(data[0].startedAt) : ''}</span><span>{data.length ? date(data.at(-1).startedAt) : ''}</span></figcaption></figure>
    <details className="cs-disclosure"><summary>{t('View trend values')}</summary><div className="usage-trend-values"><table><caption>{t('Requests per UTC bucket')}</caption><thead><tr><th>{t('Time')}</th><th>{t('Requests')}</th></tr></thead><tbody>{data.map(bucket => <tr key={bucket.startedAt}><th scope="row"><time dateTime={bucket.startedAt}>{date(bucket.startedAt)}</time></th><td>{bucket.requests}</td></tr>)}</tbody></table></div></details>
  </section>
}
