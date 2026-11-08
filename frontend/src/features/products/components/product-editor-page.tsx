import { notFound } from "next/navigation";
import { fmt } from "@/shared/i18n/config";
import { getI18n } from "@/shared/i18n/server";
import { ApiError } from "@/shared/lib/api-client";
import { PageHeader } from "@/shared/ui";
import { productsQueries } from "../api/products-queries";
import { ProductForm } from "./product-form";

export async function NewProductPage() {
  const { t } = await getI18n();
  return (
    <div className="flex flex-col gap-6 animate-fade-up">
      <PageHeader title={t.products.newTitle} description={t.products.newDescription} />
      <ProductForm />
    </div>
  );
}

export async function EditProductPage({ id }: { id: string }) {
  const { t } = await getI18n();
  const product = await productsQueries.get(id).catch((error: unknown) => {
    if (error instanceof ApiError && error.problem.status === 404) notFound();
    throw error;
  });
  return (
    <div className="flex flex-col gap-6 animate-fade-up">
      <PageHeader title={fmt(t.products.editTitle, { name: product.name })} />
      <ProductForm product={product} />
    </div>
  );
}
