import { cn } from "@/shared/lib/cn";

/** One column of a {@link DataTable}: a header and how to render a cell from a row. */
export interface Column<T> {
  key: string;
  header: string;
  cell: (row: T) => React.ReactNode;
  className?: string;
}

export interface DataTableProps<T> {
  columns: Column<T>[];
  rows: T[];
  rowKey: (row: T) => string;
  /** Rendered instead of the table when there are no rows. */
  empty?: React.ReactNode;
}

/** Generic, config-driven table. Features pass columns; they never write <table> markup. */
export function DataTable<T>({ columns, rows, rowKey, empty }: DataTableProps<T>) {
  if (rows.length === 0 && empty) {
    return <>{empty}</>;
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="bg-primary-soft/60 text-xs text-brand-700">
            {columns.map((column) => (
              <th key={column.key} className={cn("px-5 py-3 text-start font-bold", column.className)}>
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-border">
          {rows.map((row) => (
            <tr key={rowKey(row)} className="transition hover:bg-background/70">
              {columns.map((column) => (
                <td key={column.key} className={cn("px-5 py-4 align-middle", column.className)}>
                  {column.cell(row)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
