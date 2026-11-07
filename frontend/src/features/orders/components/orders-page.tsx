import { ListPage, type ListPageConfig } from "@/shared/components/list-page";
import { errorMessage } from "@/shared/lib/errors";
import { formatDateTime, formatMoney } from "@/shared/lib/format";
import { Badge, Card, EmptyState, PageHeader } from "@/shared/ui";
import { ordersApi } from "../api/orders-api";
import type { Order, OrderLine } from "../types";
import { OrderStatusSelect } from "./order-status-select";

const TITLE = "الأوردرات";
const DESCRIPTION = "الأوردرات اللي العملاء أكدوها مع البوت على واتساب. الأسعار من قايمة المنتجات، مش من البوت.";

function lineText(line: OrderLine) {
  const details = [line.size && `مقاس ${line.size}`, line.color].filter(Boolean).join(" · ");
  return `${line.quantity} × ${line.productName}${details ? ` (${details})` : ""}`;
}

const config: ListPageConfig<Order> = {
  title: TITLE,
  description: DESCRIPTION,
  rowKey: (order) => order.id,
  columns: [
    {
      key: "customer",
      header: "العميل",
      cell: (o) => (
        <div>
          <p className="font-semibold">{o.customerName ?? "—"}</p>
          <p className="text-xs text-muted" dir="ltr">
            +{o.customerPhone}
          </p>
        </div>
      ),
    },
    {
      key: "items",
      header: "المنتجات",
      cell: (o) => (
        <ul className="space-y-0.5">
          {o.items.map((line, i) => (
            <li key={i} className={line.unitPrice == null ? "text-danger" : undefined}>
              {lineText(line)}
            </li>
          ))}
        </ul>
      ),
    },
    {
      key: "address",
      header: "العنوان",
      className: "max-w-56",
      cell: (o) => <span className="line-clamp-2">{o.shippingAddress ?? "—"}</span>,
    },
    {
      key: "total",
      header: "الإجمالي",
      cell: (o) => (
        <div className="flex flex-col items-start gap-1">
          <span className="font-semibold">
            {o.total != null && o.currency ? formatMoney(o.total, o.currency) : "—"}
          </span>
          {o.needsReview && <Badge tone="danger">محتاج مراجعة</Badge>}
        </div>
      ),
    },
    { key: "status", header: "الحالة", cell: (o) => <OrderStatusSelect id={o.id} status={o.status} /> },
    { key: "createdAt", header: "الوقت", cell: (o) => formatDateTime(o.createdAt) },
  ],
  empty: { title: "لسه مفيش أوردرات", description: "أول ما عميل يأكد أوردر مع البوت هيظهر هنا على طول" },
};

export async function OrdersPage() {
  let orders: Order[];
  try {
    orders = await ordersApi.list();
  } catch (error) {
    return (
      <div className="flex flex-col gap-6">
        <PageHeader title={TITLE} description={DESCRIPTION} />
        <Card>
          <EmptyState title="مش قادر أجيب الأوردرات" description={errorMessage(error)} />
        </Card>
      </div>
    );
  }
  return <ListPage config={config} rows={orders} />;
}
