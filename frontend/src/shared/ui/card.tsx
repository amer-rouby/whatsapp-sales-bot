import { cn } from "@/shared/lib/cn";

export function Card({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("card", className)} {...props} />;
}

/** Card heading row: title (with optional icon) and an optional action on the other side. */
export function CardHeader({
  title,
  icon,
  action,
}: {
  title: string;
  icon?: React.ReactNode;
  action?: React.ReactNode;
}) {
  return (
    <div className="flex items-center justify-between gap-3 border-b border-border px-5 py-4">
      <h2 className="flex items-center gap-2 text-base font-bold text-brand-700">
        {icon}
        {title}
      </h2>
      {action}
    </div>
  );
}
