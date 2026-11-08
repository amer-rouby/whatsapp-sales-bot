"use client";

import { Camera, Info, ListChecks, Save, SlidersHorizontal } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import type { Dictionary } from "@/shared/i18n/dictionaries/ar";
import { useI18n } from "@/shared/i18n/provider";
import { errorMessage } from "@/shared/lib/errors";
import { Button, ButtonLink, Card, Checkbox, Field, Input, Select, Textarea } from "@/shared/ui";
import { productsApi } from "../api/products-api";
import { PRODUCT_TEMPLATES } from "../product-templates";
import type { Product, ProductInput } from "../types";
import { usePhotoGallery } from "../use-photo-gallery";
import { optionKey, OptionsEditor, type OptionDraft, type OptionErrors } from "./options-editor";
import { PhotoGallery } from "./photo-gallery";
import { specKey, SpecsEditor, type SpecDraft } from "./specs-editor";

interface FormState {
  name: string;
  kind: string;
  category: string;
  description: string;
  price: string;
  currency: string;
  inStock: boolean;
  options: OptionDraft[];
  specs: SpecDraft[];
}

interface FormErrors {
  name?: string;
  price?: string;
  currency?: string;
  options: OptionErrors;
  specs: Record<string, string>;
}

const EMPTY: FormState = {
  name: "",
  kind: "",
  category: "",
  description: "",
  price: "",
  currency: "EGP",
  inStock: true,
  options: [],
  specs: [],
};

function fromProduct(p: Product): FormState {
  return {
    name: p.name,
    kind: "",
    category: p.category ?? "",
    description: p.description ?? "",
    price: String(p.price),
    currency: p.currency,
    inStock: p.inStock,
    options: p.options.map((o) => ({
      key: optionKey(),
      name: o.name ?? "",
      priced: (o.values ?? []).some((v) => v.price != null),
      values: (o.values ?? []).map((v) => ({
        value: v.value ?? "",
        price: v.price != null ? String(v.price) : "",
        inStock: v.inStock ?? true,
      })),
    })),
    specs: p.specs.map((s) => ({ key: specKey(), name: s.name ?? "", value: s.value ?? "" })),
  };
}

const normal = (s: string) => s.trim().replace(/\s+/g, " ").toLowerCase();

/** Client-side checks mirror the backend's; the backend stays the real gatekeeper. */
function validate(form: FormState, t: Dictionary): FormErrors {
  const f = t.products.form.errors;
  const errors: FormErrors = { options: {}, specs: {} };
  if (!form.name.trim()) errors.name = f.name;
  const price = Number(form.price);
  if (form.price.trim() === "" || Number.isNaN(price) || price < 0) errors.price = f.price;
  if (!/^[A-Za-z]{3}$/.test(form.currency.trim())) errors.currency = f.currency;

  const seen = new Set<string>();
  for (const option of form.options) {
    const named = option.name.trim() !== "";
    if (!named && option.values.length === 0) continue;
    if (!named) errors.options[option.key] = f.optionName;
    else if (option.values.length === 0) errors.options[option.key] = f.optionValues;
    else if (seen.has(normal(option.name))) errors.options[option.key] = f.optionDuplicate;
    else if (option.priced && option.values.some((v) => v.price.trim() !== "" && !(Number(v.price) >= 0)))
      errors.options[option.key] = f.valuePrice;
    seen.add(normal(option.name));
  }
  for (const spec of form.specs) {
    if ((spec.name.trim() === "") !== (spec.value.trim() === "")) errors.specs[spec.key] = f.specIncomplete;
  }
  return errors;
}

function hasErrors(errors: FormErrors) {
  return !!(errors.name || errors.price || errors.currency) || Object.keys(errors.options).length > 0 || Object.keys(errors.specs).length > 0;
}

function toInput(form: FormState): ProductInput {
  return {
    name: form.name.trim(),
    category: form.category.trim() || undefined,
    description: form.description.trim() || undefined,
    price: Number(form.price),
    currency: form.currency.trim().toUpperCase(),
    inStock: form.inStock,
    options: form.options
      .filter((o) => o.name.trim() || o.values.length > 0)
      .map((o) => ({
        name: o.name.trim(),
        values: o.values.map((v) => ({
          value: v.value,
          price: o.priced && v.price.trim() !== "" ? Number(v.price) : undefined,
          inStock: v.inStock,
        })),
      })),
    specs: form.specs
      .filter((s) => s.name.trim() || s.value.trim())
      .map((s) => ({ name: s.name.trim(), value: s.value.trim() })),
  };
}

/** One form for both "add" and "edit" — pass `product` to edit. */
export function ProductForm({ product }: { product?: Product }) {
  const router = useRouter();
  const { t, locale } = useI18n();
  const f = t.products.form;
  const [form, setForm] = useState<FormState>(product ? fromProduct(product) : EMPTY);
  const [errors, setErrors] = useState<FormErrors>({ options: {}, specs: {} });
  const [serverError, setServerError] = useState<string>();
  const [saving, setSaving] = useState<"product" | "photos" | null>(null);
  // a new product is created on the first save; if a photo then fails, the next save updates it instead
  const [createdId, setCreatedId] = useState<string>();
  const productId = product?.id ?? createdId;
  const photos = usePhotoGallery(productId, product?.images);

  const template = PRODUCT_TEMPLATES.find((x) => x.id === form.kind);
  const optionSuggestions = (template?.options ?? []).map((o) => ({ name: o.name[locale], examples: o.examples }));
  const specSuggestions = (template?.specs ?? []).map((s) => s[locale]);

  function set<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  /** Picking a kind on an empty product fills in its usual options and specs; otherwise they're offered as chips. */
  function chooseKind(kind: string) {
    const chosen = PRODUCT_TEMPLATES.find((x) => x.id === kind);
    setForm((current) => {
      const blank = current.options.length === 0 && current.specs.length === 0;
      if (!chosen || !blank) return { ...current, kind };
      return {
        ...current,
        kind,
        category: current.category || (chosen.id === "other" ? "" : chosen.label[locale]),
        options: chosen.options.map((o) => ({ key: optionKey(), name: o.name[locale], priced: false, values: [], examples: o.examples })),
        specs: chosen.specs.map((s) => ({ key: specKey(), name: s[locale], value: "" })),
      };
    });
  }

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    const found = validate(form, t);
    setErrors(found);
    if (hasErrors(found)) return;

    setSaving("product");
    setServerError(undefined);
    try {
      const input = toInput(form);
      if (productId) {
        await productsApi.update(productId, input);
      } else {
        const created = await productsApi.create(input);
        setCreatedId(created.id);
        setSaving("photos");
        if (!(await photos.uploadPending(created.id))) {
          setServerError(t.products.photos.someFailed);
          setSaving(null);
          return;
        }
      }
      router.push("/products");
      router.refresh();
    } catch (error) {
      setServerError(errorMessage(error, t));
      setSaving(null);
    }
  }

  return (
    <form onSubmit={submit} noValidate className="space-y-5">
      <Section icon={Info} title={f.sections.basics}>
        <div className="grid gap-6 md:grid-cols-2">
          <Field label={f.name} htmlFor="name" required error={errors.name} className="md:col-span-2">
            <Input id="name" value={form.name} onChange={(e) => set("name", e.target.value)} maxLength={200} aria-invalid={!!errors.name} />
          </Field>

          <Field label={f.kind} htmlFor="kind" hint={f.kindHint}>
            <Select id="kind" value={form.kind} onChange={(e) => chooseKind(e.target.value)}>
              <option value="">—</option>
              {PRODUCT_TEMPLATES.map((x) => (
                <option key={x.id} value={x.id}>
                  {x.label[locale]}
                </option>
              ))}
            </Select>
          </Field>

          <Field label={f.category} htmlFor="category" hint={f.categoryHint}>
            <Input id="category" value={form.category} onChange={(e) => set("category", e.target.value)} maxLength={100} />
          </Field>

          <div className="grid grid-cols-[1fr_7rem] gap-3 md:col-span-2 md:max-w-md">
            <Field label={f.price} htmlFor="price" required error={errors.price} hint={f.priceHint}>
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

          <Field label={f.description} htmlFor="description" hint={f.descriptionHint} className="md:col-span-2">
            <Textarea id="description" value={form.description} onChange={(e) => set("description", e.target.value)} maxLength={2000} />
          </Field>

          <div className="md:col-span-2">
            <Checkbox id="inStock" label={f.inStock} checked={form.inStock} onChange={(e) => set("inStock", e.target.checked)} />
          </div>
        </div>
      </Section>

      <Section icon={Camera} title={f.sections.photos}>
        <PhotoGallery photos={photos} />
      </Section>

      <Section icon={SlidersHorizontal} title={f.sections.options}>
        <OptionsEditor
          options={form.options}
          onChange={(options) => set("options", options)}
          errors={errors.options}
          suggestions={optionSuggestions}
          basePrice={form.price}
        />
      </Section>

      <Section icon={ListChecks} title={f.sections.specs}>
        <SpecsEditor specs={form.specs} onChange={(specs) => set("specs", specs)} errors={errors.specs} suggestions={specSuggestions} />
      </Section>

      <div className="sticky bottom-0 z-10 -mx-1 rounded-t-2xl border-t border-border bg-surface/95 px-4 py-4 shadow-[0_-8px_24px_-12px_rgb(0_0_0/0.15)] backdrop-blur">
        {serverError && (
          <p className="mb-3 rounded-control bg-danger-soft px-4 py-3 text-sm font-bold text-danger" role="alert">
            {serverError}
          </p>
        )}
        <div className="flex flex-wrap gap-2">
          <Button type="submit" disabled={saving !== null || photos.busy}>
            <Save size={16} aria-hidden />
            {saving === "photos" ? f.uploadingPhotos : saving ? f.saving : productId ? f.saveChanges : f.create}
          </Button>
          <ButtonLink href="/products" variant="secondary">
            {t.common.cancel}
          </ButtonLink>
        </div>
      </div>
    </form>
  );
}

function Section({ icon: Icon, title, children }: { icon: typeof Info; title: string; children: React.ReactNode }) {
  return (
    <Card className="p-5 sm:p-7">
      <h2 className="mb-5 flex items-center gap-2 text-base font-bold text-brand-800">
        <span className="grid size-8 place-items-center rounded-lg bg-primary-soft text-primary">
          <Icon size={16} aria-hidden />
        </span>
        {title}
      </h2>
      {children}
    </Card>
  );
}
