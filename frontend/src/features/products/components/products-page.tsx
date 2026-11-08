import { Package, Plus } from "lucide-react";
import { ListPage, type ListPageConfig } from "@/shared/components/list-page";
import { fmt, type Locale } from "@/shared/i18n/config";
import { productImageUrl } from "@/shared/lib/product-image";
import type { Dictionary } from "@/shared/i18n/dictionaries/ar";
import { getI18n } from "@/shared/i18n/server";
import { errorMessage } from "@/shared/lib/errors";
import { formatMoney } from "@/shared/lib/format";
import { load } from "@/shared/lib/load";
import { Badge, ButtonLink } from "@/shared/ui";
import { productsQueries } from "../api/products-queries";
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
            <ProductThumb product={p} />
            <div className="min-w-0">
              <p dir="auto" className="truncate text-start font-bold text-brand-800">{p.name}</p>
              <p className="text-xs text-muted">
                {[p.category, p.images.length > 1 && fmt(t.products.photoCount, { count: p.images.length })]
                  .filter(Boolean)
                  .join(" · ")}
              </p>
            </div>
          </div>
        ),
      },
      {
        key: "price",
        header: t.products.columns.price,
        cell: (p) => (
          <span className="font-bold">
            {hasOptionPrices(p)
              ? fmt(t.products.priceFrom, { price: formatMoney(lowestPrice(p), p.currency, locale) })
              : formatMoney(p.price, p.currency, locale)}
          </span>
        ),
      },
      {
        key: "options",
        header: t.products.columns.options,
        cell: (p) =>
          p.options.length === 0 ? (
            none
          ) : (
            <ul className="flex max-w-xs flex-col gap-1">
              {p.options.map((o) => (
                <li key={o.name} className="truncate text-xs" dir="auto">
                  <span className="font-bold text-brand-800">{o.name}: </span>
                  <span className="text-muted">{(o.values ?? []).map((v) => v.value).join("، ")}</span>
                </li>
              ))}
            </ul>
          ),
      },
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

function hasOptionPrices(p: Product) {
  return p.options.some((o) => (o.values ?? []).some((v) => v.price != null));
}

/** The cheapest price a customer can pay: an option's own price, else the base price. */
function lowestPrice(p: Product) {
  const prices = p.options.flatMap((o) => (o.values ?? []).map((v) => v.price ?? p.price));
  return prices.length > 0 ? Math.min(...prices) : p.price;
}

function ProductThumb({ product }: { product: Product }) {
  const main = product.images[0];
  if (!main) {
    return (
      <span className="grid size-12 shrink-0 place-items-center rounded-xl bg-primary-soft text-primary">
        <Package size={20} aria-hidden />
      </span>
    );
  }
  return (
    // eslint-disable-next-line @next/next/no-img-element -- next/image's optimizer can't send the session
    <img
      src={productImageUrl(main.id, "thumb")}
      alt=""
      width={48}
      height={48}
      loading="lazy"
      className="size-12 shrink-0 rounded-xl bg-secondary object-cover ring-1 ring-border"
    />
  );
}

export async function ProductsPage() {
  const { t, locale } = await getI18n();
  const result = await load(productsQueries.list);
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
