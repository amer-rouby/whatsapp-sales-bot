import { CloudOff, type LucideIcon } from "lucide-react";
import { Card, DataTable, EmptyState, PageHeader, type Column } from "@/shared/ui";

/**
 * Everything a list screen needs, as data. A feature describes its screen with this
 * config and gets header + table + empty state + error state — no duplicated page markup.
 */
export interface ListPageConfig<T> {
  title: string;
  description?: string;
  actions?: React.ReactNode;
  columns: Column<T>[];
  rowKey: (row: T) => string;
  empty: { title: string; description?: string; action?: React.ReactNode; icon?: LucideIcon };
}

export type ListPageProps<T> =
  | { config: ListPageConfig<T>; rows: T[]; error?: undefined }
  /** Loading failed: show the header and a friendly error instead of the table. */
  | { config: ListPageConfig<T>; rows?: undefined; error: { title: string; description: string } };

export function ListPage<T>({ config, rows, error }: ListPageProps<T>) {
  return (
    <div className="flex flex-col gap-6 animate-fade-up">
      <PageHeader title={config.title} description={config.description} actions={error ? undefined : config.actions} />
      <Card className="overflow-hidden">
        {error ? (
          <EmptyState icon={CloudOff} title={error.title} description={error.description} />
        ) : (
          <DataTable
            columns={config.columns}
            rows={rows}
            rowKey={config.rowKey}
            empty={<EmptyState {...config.empty} />}
          />
        )}
      </Card>
    </div>
  );
}
