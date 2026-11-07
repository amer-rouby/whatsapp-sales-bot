import { cn } from "@/shared/lib/cn";

export type SelectProps = React.SelectHTMLAttributes<HTMLSelectElement>;

export function Select({ className, ...props }: SelectProps) {
  return <select className={cn("field-input cursor-pointer py-2 font-bold", className)} {...props} />;
}
