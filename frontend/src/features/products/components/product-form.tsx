"use client";

import { Save } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import type { Dictionary } from "@/shared/i18n/dictionaries/ar";
import { useI18n } from "@/shared/i18n/provider";
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

type FormErrors = Partial<Record<keyof FormState, string>>;

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
function validate(form: FormState, t: Dictionary): FormErrors {
  const errors: FormErrors = {};
  if (!form.name.trim()) errors.name = t.products.form.errors.name;
  const price = Number(form.price);
  if (form.price.trim() === "" || Number.isNaN(price) || price < 0) errors.price = t.products.form.errors.price;
  if (!/^[A-Za-z]{3}$/.test(form.currency.trim())) errors.currency = t.products.form.errors.currency;
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
  const { t } = useI18n();
  const f = t.products.form;
  const [form, setForm] = useState<FormState>(product ? fromProduct(product) : EMPTY);
  const [errors, setErrors] = useState<FormErrors>({});
  const [serverError, setServerError] = useState<string>();
  const [saving, setSaving] = useState(false);

  function set<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    const found = validate(form, t);
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
      setServerError(errorMessage(error, t));
      setSaving(false);
    }
  }

  return (
    <form onSubmit={submit} noValidate>
      <Card className="p-6 sm:p-8">
        <div className="grid gap-6 md:grid-cols-2">
          <Field label={f.name} htmlFor="name" required error={errors.name} className="md:col-span-2">
            <Input
              id="name"
              value={form.name}
              onChange={(e) => set("name", e.target.value)}
              maxLength={200}
              aria-invalid={!!errors.name}
            />
          </Field>

          <div className="grid grid-cols-[1fr_7rem] gap-3">
            <Field label={f.price} htmlFor="price" required error={errors.price}>
              <Input
                id="price"
                type="number"
                inputMode="decimal"
                min={0}
                step="0.01"
                value={form.price}
                onChange={(e) => set("price", e.target.value)}
                aria-invalid={!!errors.price}
              />
            </Field>
            <Field label={f.currency} htmlFor="currency" required error={errors.currency}>
              <Input
                id="currency"
                value={form.currency}
                onChange={(e) => set("currency", e.target.value)}
                maxLength={3}
                dir="ltr"
                className="uppercase"
                aria-invalid={!!errors.currency}
              />
            </Field>
          </div>

          <Field label={f.category} htmlFor="category" hint={f.categoryHint}>
            <Input id="category" value={form.category} onChange={(e) => set("category", e.target.value)} maxLength={100} />
          </Field>

          <Field label={f.sizes} htmlFor="sizes" hint={f.sizesHint}>
            <Input id="sizes" value={form.sizes} onChange={(e) => set("sizes", e.target.value)} maxLength={200} />
          </Field>

          <Field label={f.colors} htmlFor="colors" hint={f.colorsHint}>
            <Input id="colors" value={form.colors} onChange={(e) => set("colors", e.target.value)} maxLength={200} />
          </Field>

          <Field label={f.description} htmlFor="description" hint={f.descriptionHint} className="md:col-span-2">
            <Textarea
              id="description"
              value={form.description}
              onChange={(e) => set("description", e.target.value)}
              maxLength={2000}
            />
          </Field>

          <div className="md:col-span-2">
            <Checkbox id="inStock" label={f.inStock} checked={form.inStock} onChange={(e) => set("inStock", e.target.checked)} />
          </div>
        </div>

        {serverError && (
          <p className="mt-6 rounded-control bg-danger-soft px-4 py-3 text-sm font-bold text-danger" role="alert">
            {serverError}
          </p>
        )}

        <div className="mt-8 flex flex-wrap gap-2 border-t border-border pt-6">
          <Button type="submit" disabled={saving}>
            <Save size={16} aria-hidden />
            {saving ? f.saving : product ? f.saveChanges : f.create}
          </Button>
          <ButtonLink href="/products" variant="secondary">
            {t.common.cancel}
          </ButtonLink>
        </div>
      </Card>
    </form>
  );
}
