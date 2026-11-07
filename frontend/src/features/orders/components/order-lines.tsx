import { AlertTriangle } from "lucide-react";
import { fmt } from "@/shared/i18n/config";
import type { Dictionary } from "@/shared/i18n/dictionaries/ar";
import type { OrderLine } from "../types";

/** "2 × White sneaker (size 42 · white)"; unpriced lines (not in the catalog) are flagged. */
export function OrderLines({ lines, t }: { lines: OrderLine[]; t: Dictionary }) {
  return (
    <ul className="space-y-1">
      {lines.map((line, i) => {
        const details = [line.size && fmt(t.orders.size, { size: line.size }), line.color].filter(Boolean).join(" · ");
        const unknown = line.unitPrice == null;
        return (
          <li key={i} dir="auto" className={unknown ? "flex items-center gap-1 text-start text-danger" : "text-start"}>
            {unknown && <AlertTriangle size={14} aria-hidden />}
            <span className="font-bold">
              <span data-ltr>{line.quantity}×</span> {line.productName}
            </span>
            {details && <span className="text-muted"> ({details})</span>}
          </li>
        );
      })}
    </ul>
  );
}
