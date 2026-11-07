import { Package, Plus } from "lucide-react";
import { ListPage, type ListPageConfig } from "@/shared/components/list-page";
import type { Locale } from "@/shared/i18n/config";
import type { Dictionary } from "@/shared/i18n/dictionaries/ar";
import { getI18n } from "@/shared/i18n/server";
import { errorMessage } from "@/shared/lib/errors";
import { formatMoney } from "@/shared/lib/format";
import { load } from "@/shared/lib/load";
import { Badge, ButtonLink } from "@/shared/ui";
import { productsApi } from "../api/products-api";
import type { Product } from "../types";
import { DeleteProductButton } from "./delete-product-button";

function config(t: Dictionary, locale: Locale): ListPageConfig<Product> {
  const add = (
    <ButtonLink href="/products/new">
      <Plus size={16} aria-hidden />
      {t.products.add}
    </ButtonLink>
  );
  const none = <span className="text-muted">{t.common.none}</span>;

  return {
    title: t.products.title,
    description: t.products.description,
    actions: add,
    rowKey: (product) => product.id,
    columns: [
      {
        key: "name",
        header: t.products.columns.product,
        cell: (p) => (
          <div className="flex items-center gap-3">
            <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-primary-soft text-primary">
              <Package size={18} aria-hidden />
            </span>
            <div className="min-w-0">
              <p dir="auto" className="truncate text-start font-bold text-brand-800">{p.name}</p>
              {p.category && <p className="text-xs text-muted">{p.category}</p>}
            </div>
          </div>
        ),
      },
      {
        key: "price",
        header: t.products.columns.price,
        cell: (p) => <span className="font-bold">{formatMoney(p.price, p.currency, locale)}</span>,
      },
      { key: "sizes", header: t.products.columns.sizes, cell: (p) => (p.sizes ? <span data-ltr>{p.sizes}</span> : none) },
      { key: "colors", header: t.products.columns.colors, cell: (p) => p.colors ?? none },
      {
        key: "stock",
        header: t.products.columns.stock,
        cell: (p) =>
          p.inStock ? <Badge tone="success">{t.products.inStock}</Badge> : <Badge tone="danger">{t.products.soldOut}</Badge>,
      },
      {
        key: "actions",
        header: "",
        className: "w-px whitespace-nowrap",
        cell: (p) => (
          <div className="flex justify-end gap-1">
            <ButtonLink href={`/products/${p.id}/edit`} variant="ghost" size="sm">
              {t.common.edit}
            </ButtonLink>
            <DeleteProductButton id={p.id} name={p.name} />
          </div>
        ),
      },
    ],
    empty: { title: t.products.emptyTitle, description: t.products.emptyDescription, action: add, icon: Package },
  };
}

export async function ProductsPage() {
  const { t, locale } = await getI18n();
  const result = await load(productsApi.list);
  if (!result.ok) {
    return (
      <ListPage
        config={config(t, locale)}
        error={{ title: t.products.loadError, description: errorMessage(result.error, t) }}
      />
    );
  }
  return <ListPage config={config(t, locale)} rows={result.data} />;
}
