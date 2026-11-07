export interface PageHeaderProps {
  title: string;
  description?: string;
  /** Buttons shown at the end of the header (e.g. "Add product"). */
  actions?: React.ReactNode;
}

export function PageHeader({ title, description, actions }: PageHeaderProps) {
  return (
    <header className="flex flex-wrap items-end justify-between gap-4">
      <div className="min-w-0">
        <h2 className="text-2xl font-extrabold text-brand-800">{title}</h2>
        {description && <p className="mt-1 max-w-2xl text-sm leading-relaxed text-muted">{description}</p>}
      </div>
      {actions && <div className="flex items-center gap-2">{actions}</div>}
    </header>
  );
}
