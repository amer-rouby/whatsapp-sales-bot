"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { useI18n } from "@/shared/i18n/provider";
import { cn } from "@/shared/lib/cn";
import { errorMessage } from "@/shared/lib/errors";
import { Select } from "@/shared/ui";
import { ordersApi } from "../api/orders-api";
import { ORDER_STATUSES, type OrderStatus } from "../types";

const TONE_CLASS: Record<OrderStatus, string> = {
  NEW: "border-warning/40 bg-warning-soft text-warning",
  CONFIRMED: "border-primary/30 bg-primary-soft text-primary",
  SHIPPED: "border-success/40 bg-success-soft text-success",
  CANCELLED: "border-danger/40 bg-danger-soft text-danger",
};

export function OrderStatusSelect({ id, status }: { id: string; status: OrderStatus }) {
  const router = useRouter();
  const { t } = useI18n();
  const [value, setValue] = useState(status);
  const [busy, setBusy] = useState(false);

  async function change(next: OrderStatus) {
    const previous = value;
    setValue(next); // optimistic
    setBusy(true);
    try {
      await ordersApi.changeStatus(id, next);
      router.refresh();
    } catch (error) {
      setValue(previous);
      window.alert(errorMessage(error, t));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Select
      aria-label={t.orders.statusLabel}
      value={value}
      disabled={busy}
      onChange={(e) => change(e.target.value as OrderStatus)}
      className={cn("h-9 w-auto rounded-full px-3 text-xs", TONE_CLASS[value])}
    >
      {ORDER_STATUSES.map((s) => (
        <option key={s} value={s}>
          {t.orders.statuses[s]}
        </option>
      ))}
    </Select>
  );
}
