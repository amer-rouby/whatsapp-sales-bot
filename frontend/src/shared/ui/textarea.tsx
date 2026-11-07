import { cn } from "@/shared/lib/cn";

export type TextareaProps = React.TextareaHTMLAttributes<HTMLTextAreaElement>;

export function Textarea({ className, ...props }: TextareaProps) {
  return (
    <textarea
      className={cn(
        "min-h-24 w-full rounded-control border border-border bg-surface px-3 py-2 text-sm",
        "placeholder:text-muted focus:border-primary focus:outline-none",
        className,
      )}
      {...props}
    />
  );
}
