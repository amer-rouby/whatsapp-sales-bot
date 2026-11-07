import { cn } from "@/shared/lib/cn";

export type InputProps = React.InputHTMLAttributes<HTMLInputElement>;

export function Input({ className, ...props }: InputProps) {
  return <input className={cn("field-input", className)} {...props} />;
}
