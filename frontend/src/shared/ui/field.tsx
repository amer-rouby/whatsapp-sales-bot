import { cn } from "@/shared/lib/cn";

export interface FieldProps {
  label: string;
  /** id of the control inside, so clicking the label focuses it */
  htmlFor: string;
  hint?: string;
  error?: string;
  required?: boolean;
  className?: string;
  children: React.ReactNode;
}

/** Label + control + hint/error. Every form field in the app uses this, so forms look the same. */
export function Field({ label, htmlFor, hint, error, required, className, children }: FieldProps) {
  return (
    <div className={cn("flex flex-col gap-1.5", className)}>
      <label htmlFor={htmlFor} className="text-sm font-bold text-brand-800">
        {label}
        {required && <span className="ms-1 text-danger">*</span>}
      </label>
      {children}
      {error ? (
        <p className="text-xs font-bold text-danger" role="alert">
          {error}
        </p>
      ) : (
        hint && <p className="text-xs text-muted">{hint}</p>
      )}
    </div>
  );
}
