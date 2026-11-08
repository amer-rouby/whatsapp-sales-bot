import { AlertTriangle } from "lucide-react";
import type { OrderLine } from "../types";

/** "2 × Phone (Storage 256GB · Colour black)"; unpriced lines (not in the catalog) are flagged. */
export function OrderLines({ lines }: { lines: OrderLine[] }) {
  return (
    <ul className="space-y-1">
      {lines.map((line, i) => {
        const details = (line.options ?? []).map((o) => `${o.name} ${o.value}`).join(" · ");
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
