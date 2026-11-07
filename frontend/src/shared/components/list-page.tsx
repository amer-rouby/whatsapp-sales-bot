import { Card, DataTable, EmptyState, PageHeader, type Column } from "@/shared/ui";

/**
 * Everything a list screen needs, as data. A feature describes its screen with this
 * config and gets header + table + empty state for free — no duplicated page markup.
 */
export interface ListPageConfig<T> {
  title: string;
  description?: string;
  actions?: React.ReactNode;
  columns: Column<T>[];
  rowKey: (row: T) => string;
  empty: { title: string; description?: string; action?: React.ReactNode };
}

export interface ListPageProps<T> {
  config: ListPageConfig<T>;
  rows: T[];
}

export function ListPage<T>({ config, rows }: ListPageProps<T>) {
  return (
    <div className="flex flex-col gap-6">
      <PageHeader title={config.title} description={config.description} actions={config.actions} />
      <Card>
        <DataTable
          columns={config.columns}
          rows={rows}
          rowKey={config.rowKey}
          empty={<EmptyState {...config.empty} />}
        />
      </Card>
    </div>
  );
}
