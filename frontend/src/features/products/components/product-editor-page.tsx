import { notFound } from "next/navigation";
import { ApiError } from "@/shared/lib/api-client";
import { PageHeader } from "@/shared/ui";
import { productsApi } from "../api/products-api";
import { ProductForm } from "./product-form";

export function NewProductPage() {
  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="منتج جديد" description="اللي هتكتبه هنا هو اللي البوت هيقوله للعملاء بالظبط." />
      <ProductForm />
    </div>
  );
}

export async function EditProductPage({ id }: { id: string }) {
  const product = await productsApi.get(id).catch((error: unknown) => {
    if (error instanceof ApiError && error.problem.status === 404) notFound();
    throw error;
  });
  return (
    <div className="flex flex-col gap-6">
      <PageHeader title={`تعديل: ${product.name}`} />
      <ProductForm product={product} />
    </div>
  );
}
