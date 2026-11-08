import { ShoppingBag } from "lucide-react";
import { ListPage, type ListPageConfig } from "@/shared/components/list-page";
import type { Locale } from "@/shared/i18n/config";
import type { Dictionary } from "@/shared/i18n/dictionaries/ar";
import { getI18n } from "@/shared/i18n/server";
import { errorMessage } from "@/shared/lib/errors";
import { formatDateTime, formatMoney } from "@/shared/lib/format";
import { load } from "@/shared/lib/load";
import { Badge } from "@/shared/ui";
import { ordersQueries } from "../api/orders-queries";
import type { Order } from "../types";
import { OrderLines } from "./order-lines";
import { OrderStatusSelect } from "./order-status-select";

function config(t: Dictionary, locale: Locale): ListPageConfig<Order> {
  const c = t.orders.columns;
  return {
    title: t.orders.title,
    description: t.orders.description,
    rowKey: (order) => order.id,
    columns: [
      {
        key: "customer",
        header: c.customer,
        cell: (o) => (
          <div>
            <p dir="auto" className="text-start font-bold text-brand-800">{o.customerName ?? t.common.none}</p>
            <p className="text-xs text-muted" data-ltr>
              +{o.customerPhone}
            </p>
          </div>
        ),
      },
      { key: "items", header: c.items, cell: (o) => <OrderLines lines={o.items} /> },
      {
        key: "address",
        header: c.address,
        className: "max-w-60",
        cell: (o) => <span dir="auto" className="line-clamp-2 text-start text-muted">{o.shippingAddress ?? t.common.none}</span>,
      },
      {
        key: "total",
        header: c.total,
        cell: (o) => (
          <div className="flex flex-col items-start gap-1">
            <span className="font-extrabold text-brand-800">
              {o.total != null && o.currency ? formatMoney(o.total, o.currency, locale) : t.common.none}
            </span>
            {o.needsReview && <Badge tone="danger">{t.orders.needsReview}</Badge>}
          </div>
        ),
      },
      { key: "status", header: c.status, cell: (o) => <OrderStatusSelect id={o.id} status={o.status} /> },
      {
        key: "createdAt",
        header: c.time,
        className: "whitespace-nowrap",
        cell: (o) => <span className="text-xs text-muted">{formatDateTime(o.createdAt, locale)}</span>,
      },
    ],
    empty: { title: t.orders.emptyTitle, description: t.orders.emptyDescription, icon: ShoppingBag },
  };
}

export async function OrdersPage() {
  const { t, locale } = await getI18n();
  const result = await load(ordersQueries.list);
  if (!result.ok) {
    return (
      <ListPage config={config(t, locale)} error={{ title: t.orders.loadError, description: errorMessage(result.error, t) }} />
    );
  }
  return <ListPage config={config(t, locale)} rows={result.data} />;
}
