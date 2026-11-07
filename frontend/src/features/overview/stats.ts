import type { Order } from "@/features/orders";
import type { Product } from "@/features/products";

export interface OverviewStats {
  ordersToday: number;
  /** total of non-cancelled orders in the most used currency (mixing currencies would be meaningless) */
  revenue: { amount: number; currency: string } | null;
  needsFollowUp: number;
  productsAvailable: number;
  productsTotal: number;
  recentOrders: Order[];
}

function isToday(iso: string, now: Date) {
  const d = new Date(iso);
  return d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth() && d.getDate() === now.getDate();
}

/** Pure function so it is trivial to reason about and test. */
export function computeStats(orders: Order[], products: Product[], now = new Date()): OverviewStats {
  const counted = orders.filter((o) => o.status !== "CANCELLED" && o.total != null && o.currency);
  const byCurrency = new Map<string, number>();
  for (const o of counted) {
    byCurrency.set(o.currency!, (byCurrency.get(o.currency!) ?? 0) + Number(o.total));
  }
  const main = [...byCurrency.entries()].sort((a, b) => b[1] - a[1])[0];

  return {
    ordersToday: orders.filter((o) => isToday(o.createdAt, now)).length,
    revenue: main ? { currency: main[0], amount: main[1] } : null,
    needsFollowUp: orders.filter((o) => o.status === "NEW").length,
    productsAvailable: products.filter((p) => p.inStock).length,
    productsTotal: products.length,
    recentOrders: orders.slice(0, 5),
  };
}
