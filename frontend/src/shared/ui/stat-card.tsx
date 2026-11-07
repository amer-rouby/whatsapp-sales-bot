import type { LucideIcon } from "lucide-react";
import { cn } from "@/shared/lib/cn";

export interface StatCardProps {
  label: string;
  value: string;
  hint?: string;
  icon: LucideIcon;
  /** "gold" highlights the single most important number on a page */
  accent?: "brand" | "gold";
}

export function StatCard({ label, value, hint, icon: Icon, accent = "brand" }: StatCardProps) {
  return (
    <div className="card relative overflow-hidden p-5 animate-fade-up">
      {/* soft brand glow in the corner */}
      <span
        className={cn(
          "pointer-events-none absolute -end-8 -top-8 size-28 rounded-full opacity-60",
          accent === "gold" ? "bg-gold-100" : "bg-primary-soft",
        )}
        aria-hidden
      />
      <div className="relative flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-sm font-bold text-muted">{label}</p>
          <p className="mt-2 text-2xl font-extrabold leading-tight text-brand-800 xl:text-3xl">
            {value}
          </p>
          {hint && <p className="mt-1 text-xs text-muted">{hint}</p>}
        </div>
        <span
          className={cn(
            "grid size-11 shrink-0 place-items-center rounded-xl",
            accent === "gold" ? "bg-gold-500 text-brand-900" : "bg-primary text-primary-foreground",
          )}
        >
          <Icon size={20} aria-hidden />
        </span>
      </div>
    </div>
  );
}
