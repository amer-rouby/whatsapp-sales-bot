"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { cn } from "@/shared/lib/cn";
import { errorMessage } from "@/shared/lib/errors";
import { ordersApi } from "../api/orders-api";
import { STATUS_META } from "../status";
import { ORDER_STATUSES, type OrderStatus } from "../types";

export function OrderStatusSelect({ id, status }: { id: string; status: OrderStatus }) {
  const router = useRouter();
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
      window.alert(errorMessage(error));
    } finally {
      setBusy(false);
    }
  }

  return (
    <select
      aria-label="حالة الأوردر"
      value={value}
      disabled={busy}
      onChange={(e) => change(e.target.value as OrderStatus)}
      className={cn(
        "h-8 rounded-control border border-border bg-surface px-2 text-sm font-semibold",
        "focus:border-primary focus:outline-none disabled:opacity-60",
      )}
    >
      {ORDER_STATUSES.map((s) => (
        <option key={s} value={s}>
          {STATUS_META[s].label}
        </option>
      ))}
    </select>
  );
}
