import { cn } from "@/shared/lib/cn";

export interface CheckboxProps extends Omit<React.InputHTMLAttributes<HTMLInputElement>, "type"> {
  label: string;
}

export function Checkbox({ label, className, id, ...props }: CheckboxProps) {
  return (
    <label
      htmlFor={id}
      className={cn(
        "inline-flex cursor-pointer items-center gap-2.5 rounded-control border border-border bg-surface px-3 py-2.5 text-sm font-bold",
        "transition hover:border-primary has-checked:border-primary has-checked:bg-primary-soft",
        className,
      )}
    >
      <input id={id} type="checkbox" className="size-4 accent-(--color-primary)" {...props} />
      {label}
    </label>
  );
}
