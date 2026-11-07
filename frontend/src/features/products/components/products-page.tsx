import { ListPage, type ListPageConfig } from "@/shared/components/list-page";
import { errorMessage } from "@/shared/lib/errors";
import { formatMoney } from "@/shared/lib/format";
import { Badge, ButtonLink, Card, EmptyState, PageHeader } from "@/shared/ui";
import { productsApi } from "../api/products-api";
import type { Product } from "../types";
import { DeleteProductButton } from "./delete-product-button";

const TITLE = "المنتجات";
const DESCRIPTION = "البوت بيبيع اللي هنا بس. أي حاجة مش في القايمة هيقول للعميل إنها مش موجودة.";
const ADD = <ButtonLink href="/products/new">إضافة منتج</ButtonLink>;

const config: ListPageConfig<Product> = {
  title: TITLE,
  description: DESCRIPTION,
  actions: ADD,
  rowKey: (product) => product.id,
  columns: [
    {
      key: "name",
      header: "المنتج",
      cell: (p) => (
        <div>
          <p className="font-semibold">{p.name}</p>
          {p.category && <p className="text-xs text-muted">{p.category}</p>}
        </div>
      ),
    },
    { key: "price", header: "السعر", cell: (p) => formatMoney(p.price, p.currency) },
    { key: "sizes", header: "المقاسات", cell: (p) => p.sizes ?? "—" },
    { key: "colors", header: "الألوان", cell: (p) => p.colors ?? "—" },
    {
      key: "stock",
      header: "المخزون",
      cell: (p) => (p.inStock ? <Badge tone="success">متاح</Badge> : <Badge tone="danger">خلص</Badge>),
    },
    {
      key: "actions",
      header: "",
      className: "w-px whitespace-nowrap",
      cell: (p) => (
        <div className="flex gap-1">
          <ButtonLink href={`/products/${p.id}/edit`} variant="ghost" size="sm">
            تعديل
          </ButtonLink>
          <DeleteProductButton id={p.id} name={p.name} />
        </div>
      ),
    },
  ],
  empty: {
    title: "لسه مفيش منتجات",
    description: "ضيف أول منتج عشان البوت يبدأ يبيعه",
    action: ADD,
  },
};

export async function ProductsPage() {
  let products: Product[];
  try {
    products = await productsApi.list();
  } catch (error) {
    return (
      <div className="flex flex-col gap-6">
        <PageHeader title={TITLE} description={DESCRIPTION} />
        <Card>
          <EmptyState title="مش قادر أجيب المنتجات" description={errorMessage(error)} />
        </Card>
      </div>
    );
  }
  return <ListPage config={config} rows={products} />;
}
