"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { errorMessage } from "@/shared/lib/errors";
import { Button, ButtonLink, Card, Checkbox, Field, Input, Textarea } from "@/shared/ui";
import { productsApi } from "../api/products-api";
import type { Product, ProductInput } from "../types";

interface FormState {
  name: string;
  category: string;
  description: string;
  price: string;
  currency: string;
  sizes: string;
  colors: string;
  inStock: boolean;
}

const EMPTY: FormState = {
  name: "",
  category: "",
  description: "",
  price: "",
  currency: "EGP",
  sizes: "",
  colors: "",
  inStock: true,
};

function fromProduct(p: Product): FormState {
  return {
    name: p.name,
    category: p.category ?? "",
    description: p.description ?? "",
    price: String(p.price),
    currency: p.currency,
    sizes: p.sizes ?? "",
    colors: p.colors ?? "",
    inStock: p.inStock,
  };
}

/** Client-side checks mirror the backend's; the backend stays the real gatekeeper. */
function validate(form: FormState): Partial<Record<keyof FormState, string>> {
  const errors: Partial<Record<keyof FormState, string>> = {};
  if (!form.name.trim()) errors.name = "اكتب اسم المنتج";
  const price = Number(form.price);
  if (form.price.trim() === "" || Number.isNaN(price) || price < 0) errors.price = "اكتب سعر صحيح (رقم موجب)";
  if (!/^[A-Za-z]{3}$/.test(form.currency.trim())) errors.currency = "3 حروف زي EGP أو SAR";
  return errors;
}

function toInput(form: FormState): ProductInput {
  return {
    name: form.name.trim(),
    category: form.category.trim() || undefined,
    description: form.description.trim() || undefined,
    price: Number(form.price),
    currency: form.currency.trim().toUpperCase(),
    sizes: form.sizes.trim() || undefined,
    colors: form.colors.trim() || undefined,
    inStock: form.inStock,
  };
}

/** One form for both "add" and "edit" — pass `product` to edit. */
export function ProductForm({ product }: { product?: Product }) {
  const router = useRouter();
  const [form, setForm] = useState<FormState>(product ? fromProduct(product) : EMPTY);
  const [errors, setErrors] = useState<Partial<Record<keyof FormState, string>>>({});
  const [serverError, setServerError] = useState<string>();
  const [saving, setSaving] = useState(false);

  function set<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    const found = validate(form);
    setErrors(found);
    if (Object.keys(found).length > 0) return;

    setSaving(true);
    setServerError(undefined);
    try {
      if (product) {
        await productsApi.update(product.id, toInput(form));
      } else {
        await productsApi.create(toInput(form));
      }
      router.push("/products");
      router.refresh();
    } catch (error) {
      setServerError(errorMessage(error));
      setSaving(false);
    }
  }

  return (
    <Card className="p-6">
      <form onSubmit={submit} className="grid gap-5 md:grid-cols-2" noValidate>
        <Field label="اسم المنتج" htmlFor="name" required error={errors.name} className="md:col-span-2">
          <Input id="name" value={form.name} onChange={(e) => set("name", e.target.value)} maxLength={200} />
        </Field>

        <Field label="السعر" htmlFor="price" required error={errors.price}>
          <Input
            id="price"
            type="number"
            inputMode="decimal"
            min={0}
            step="0.01"
            value={form.price}
            onChange={(e) => set("price", e.target.value)}
          />
        </Field>

        <Field label="العملة" htmlFor="currency" required error={errors.currency}>
          <Input
            id="currency"
            value={form.currency}
            onChange={(e) => set("currency", e.target.value)}
            maxLength={3}
            dir="ltr"
          />
        </Field>

        <Field label="المقاسات" htmlFor="sizes" hint="مثلاً: 40, 41, 42" >
          <Input id="sizes" value={form.sizes} onChange={(e) => set("sizes", e.target.value)} maxLength={200} />
        </Field>

        <Field label="الألوان" htmlFor="colors" hint="مثلاً: أبيض، أسود">
          <Input id="colors" value={form.colors} onChange={(e) => set("colors", e.target.value)} maxLength={200} />
        </Field>

        <Field label="القسم" htmlFor="category" hint="مثلاً: أحذية، تيشيرتات">
          <Input id="category" value={form.category} onChange={(e) => set("category", e.target.value)} maxLength={100} />
        </Field>

        <div className="flex items-end pb-2">
          <Checkbox
            id="inStock"
            label="متاح للبيع"
            checked={form.inStock}
            onChange={(e) => set("inStock", e.target.checked)}
          />
        </div>

        <Field
          label="وصف"
          htmlFor="description"
          hint="أي تفاصيل تساعد البوت يبيع: الخامة، المميزات..."
          className="md:col-span-2"
        >
          <Textarea
            id="description"
            value={form.description}
            onChange={(e) => set("description", e.target.value)}
            maxLength={2000}
          />
        </Field>

        {serverError && (
          <p className="rounded-control bg-danger-soft px-3 py-2 text-sm text-danger md:col-span-2" role="alert">
            {serverError}
          </p>
        )}

        <div className="flex gap-2 md:col-span-2">
          <Button type="submit" disabled={saving}>
            {saving ? "بيتحفظ…" : product ? "حفظ التعديلات" : "إضافة المنتج"}
          </Button>
          <ButtonLink href="/products" variant="secondary">
            إلغاء
          </ButtonLink>
        </div>
      </form>
    </Card>
  );
}
