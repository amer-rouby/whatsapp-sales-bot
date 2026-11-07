import { cva, type VariantProps } from "class-variance-authority";
import Link from "next/link";
import { cn } from "@/shared/lib/cn";

export const buttonVariants = cva(
  "inline-flex cursor-pointer items-center justify-center gap-2 rounded-control font-bold transition " +
    "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-500 " +
    "disabled:pointer-events-none disabled:opacity-50",
  {
    variants: {
      variant: {
        primary: "bg-primary text-primary-foreground shadow-sm hover:bg-primary-hover",
        gold: "bg-gold-500 text-brand-900 shadow-sm hover:bg-gold-600",
        outline: "border border-primary bg-surface text-primary hover:bg-primary-soft",
        secondary: "bg-secondary text-foreground hover:bg-secondary-hover",
        ghost: "text-foreground hover:bg-secondary",
        danger: "text-danger hover:bg-danger-soft",
      },
      size: {
        sm: "h-8 px-3 text-xs",
        md: "h-10 px-4 text-sm",
        lg: "h-12 px-6 text-base",
        icon: "size-9",
      },
    },
    defaultVariants: { variant: "primary", size: "md" },
  },
);

type ButtonVariants = VariantProps<typeof buttonVariants>;

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement>, ButtonVariants {}

export function Button({ className, variant, size, type = "button", ...props }: ButtonProps) {
  return <button type={type} className={cn(buttonVariants({ variant, size }), className)} {...props} />;
}

export interface ButtonLinkProps extends React.ComponentProps<typeof Link>, ButtonVariants {}

/** A link that looks like a button — same variants, real navigation. */
export function ButtonLink({ className, variant, size, ...props }: ButtonLinkProps) {
  return <Link className={cn(buttonVariants({ variant, size }), className)} {...props} />;
}
