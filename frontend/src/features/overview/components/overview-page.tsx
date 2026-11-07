import { BellRing, CloudOff, Package, ShoppingBag, ShoppingCart, Wallet } from "lucide-react";
import { OrderLines, ordersApi, STATUS_TONE } from "@/features/orders";
import { productsApi } from "@/features/products";
import { fmt } from "@/shared/i18n/config";
import { getI18n } from "@/shared/i18n/server";
import { errorMessage } from "@/shared/lib/errors";
import { formatDateTime, formatMoney, formatNumber } from "@/shared/lib/format";
import { load } from "@/shared/lib/load";
import { Badge, ButtonLink, Card, CardHeader, EmptyState, PageHeader, StatCard } from "@/shared/ui";
import { computeStats } from "../stats";

export async function OverviewPage() {
  const { t, locale } = await getI18n();
  const header = <PageHeader title={t.overview.title} description={t.overview.description} />;

  const result = await load(() => Promise.all([ordersApi.list(), productsApi.list()]));
  if (!result.ok) {
    return (
      <div className="flex flex-col gap-6">
        {header}
        <Card>
          <EmptyState icon={CloudOff} title={t.overview.loadError} description={errorMessage(result.error, t)} />
        </Card>
      </div>
    );
  }
  const stats = computeStats(...result.data);

  return (
    <div className="flex flex-col gap-6 animate-fade-up">
      {header}

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          accent="gold"
          icon={Wallet}
          label={t.overview.revenue}
          value={stats.revenue ? formatMoney(stats.revenue.amount, stats.revenue.currency, locale) : t.common.none}
          hint={t.overview.revenueHint}
        />
        <StatCard icon={ShoppingCart} label={t.overview.ordersToday} value={formatNumber(stats.ordersToday, locale)} />
        <StatCard
          icon={BellRing}
          label={t.overview.needsFollowUp}
          value={formatNumber(stats.needsFollowUp, locale)}
          hint={t.overview.needsFollowUpHint}
        />
        <StatCard
          icon={Package}
          label={t.overview.productsAvailable}
          value={formatNumber(stats.productsAvailable, locale)}
          hint={fmt(t.overview.ofTotal, { total: formatNumber(stats.productsTotal, locale) })}
        />
      </section>

      <Card className="overflow-hidden">
        <CardHeader
          title={t.overview.recentOrders}
          icon={<ShoppingBag size={18} aria-hidden />}
          action={
            stats.recentOrders.length > 0 && (
              <ButtonLink href="/orders" variant="outline" size="sm">
                {t.common.viewAll}
              </ButtonLink>
            )
          }
        />
        {stats.recentOrders.length === 0 ? (
          <EmptyState icon={ShoppingBag} title={t.orders.emptyTitle} description={t.overview.noOrders} />
        ) : (
          <ul className="divide-y divide-border">
            {stats.recentOrders.map((o) => (
              <li key={o.id} className="flex flex-wrap items-center gap-x-6 gap-y-2 px-5 py-4">
                <div className="min-w-40 flex-1">
                  <p dir="auto" className="text-start font-bold text-brand-800">{o.customerName ?? t.common.none}</p>
                  <p className="text-xs text-muted" data-ltr>
                    {formatDateTime(o.createdAt, locale)}
                  </p>
                </div>
                <div className="min-w-48 flex-[2] text-sm">
                  <OrderLines lines={o.items} t={t} />
                </div>
                <span className="font-extrabold text-brand-800">
                  {o.total != null && o.currency ? formatMoney(o.total, o.currency, locale) : t.common.none}
                </span>
                <Badge tone={STATUS_TONE[o.status]}>{t.orders.statuses[o.status]}</Badge>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  );
}
