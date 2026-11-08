"use client";

import { Plus, Tag, Trash2, X } from "lucide-react";
import { useState } from "react";
import { fmt } from "@/shared/i18n/config";
import { useI18n } from "@/shared/i18n/provider";
import { cn } from "@/shared/lib/cn";
import { Button, Input } from "@/shared/ui";

export interface OptionDraft {
  key: string;
  name: string;
  /** this option's values carry their own prices (one option at most) */
  priced: boolean;
  values: ValueDraft[];
  /** placeholder examples from the product kind */
  examples?: string;
}

export interface ValueDraft {
  value: string;
  /** text while typing; "" = base price */
  price: string;
  inStock: boolean;
}

export type OptionErrors = Record<string, string>;

let counter = 0;
export const optionKey = () => `option-${++counter}`;

/**
 * Options the customer picks from (size, colour, storage, volume...). Values are chips: type and press Enter.
 * One option may give each value its own price; any value can be marked sold out on its own.
 */
export function OptionsEditor({
  options,
  onChange,
  errors,
  suggestions,
  basePrice,
}: {
  options: OptionDraft[];
  /** shown as the placeholder of an empty value price (that value costs the base price) */
  basePrice: string;
  onChange: (options: OptionDraft[]) => void;
  errors: OptionErrors;
  suggestions: Array<{ name: string; examples: string }>;
}) {
  const { t } = useI18n();
  const f = t.products.form;
  const unused = suggestions.filter((s) => !options.some((o) => o.name.trim() === s.name));

  function update(key: string, change: Partial<OptionDraft>) {
    onChange(
      options.map((o) => {
        if (o.key === key) return { ...o, ...change };
        // turning prices on for one option turns them off for the others
        return change.priced ? { ...o, priced: false } : o;
      }),
    );
  }

  function add(name = "", examples?: string) {
    onChange([...options, { key: optionKey(), name, priced: false, values: [], examples }]);
  }

  return (
    <div className="space-y-4">
      <p className="text-xs text-muted">{f.optionsHint}</p>

      {options.map((option) => (
        <OptionCard
          key={option.key}
          option={option}
          error={errors[option.key]}
          basePrice={basePrice}
          onChange={(change) => update(option.key, change)}
          onRemove={() => onChange(options.filter((o) => o.key !== option.key))}
        />
      ))}

      <div className="flex flex-wrap items-center gap-2">
        <Button variant="outline" size="sm" onClick={() => add()}>
          <Plus size={14} aria-hidden />
          {f.addOption}
        </Button>
        {unused.length > 0 && (
          <>
            <span className="text-xs text-muted">{f.suggestions}</span>
            {unused.map((s) => (
              <button
                key={s.name}
                type="button"
                onClick={() => add(s.name, s.examples)}
                className="rounded-full border border-dashed border-primary/40 px-3 py-1 text-xs font-bold text-primary hover:bg-primary-soft"
              >
                + {s.name}
              </button>
            ))}
          </>
        )}
      </div>
    </div>
  );
}

function OptionCard({
  option,
  error,
  basePrice,
  onChange,
  onRemove,
}: {
  option: OptionDraft;
  error?: string;
  basePrice: string;
  onChange: (change: Partial<OptionDraft>) => void;
  onRemove: () => void;
}) {
  const { t } = useI18n();
  const f = t.products.form;
  const [draft, setDraft] = useState("");
  const id = option.key;

  function addValues(text: string) {
    // pasting "S, M, L" adds three values at once
    const fresh = text
      .split(/[,،\n]/)
      .map((v) => v.trim())
      .filter((v) => v && !option.values.some((x) => x.value.toLowerCase() === v.toLowerCase()));
    const unique = [...new Map(fresh.map((v) => [v.toLowerCase(), v])).values()];
    if (unique.length > 0) {
      onChange({ values: [...option.values, ...unique.map((value) => ({ value, price: "", inStock: true }))] });
    }
    setDraft("");
  }

  function setValue(index: number, change: Partial<ValueDraft>) {
    onChange({ values: option.values.map((v, i) => (i === index ? { ...v, ...change } : v)) });
  }

  return (
    <fieldset className={cn("rounded-xl border bg-background p-4", error ? "border-danger" : "border-border")}>
      <legend className="sr-only">{option.name || f.optionName}</legend>
      <div className="flex flex-wrap items-end gap-3">
        <div className="min-w-48 flex-1">
          <label htmlFor={`${id}-name`} className="mb-1.5 block text-xs font-bold text-brand-800">
            {f.optionName}
          </label>
          <Input
            id={`${id}-name`}
            value={option.name}
            onChange={(e) => onChange({ name: e.target.value })}
            placeholder={f.optionNamePlaceholder}
            maxLength={100}
            aria-invalid={!!error}
          />
        </div>
        <label className="flex cursor-pointer items-center gap-2 rounded-control border border-border bg-surface px-3 py-2 text-xs font-bold has-checked:border-primary has-checked:bg-primary-soft">
          <input
            type="checkbox"
            checked={option.priced}
            onChange={(e) => onChange({ priced: e.target.checked })}
            className="size-4 accent-(--color-primary)"
          />
          {f.pricedToggle}
        </label>
        <Button variant="danger" size="icon" onClick={onRemove} aria-label={f.removeOption} title={f.removeOption}>
          <Trash2 size={16} aria-hidden />
        </Button>
      </div>
      {option.priced && <p className="mt-2 text-xs text-muted">{f.pricedHint}</p>}

      <ul className={cn("mt-3 flex flex-wrap gap-2", option.priced && "flex-col")}>
        {option.values.map((value, index) => (
          <li
            key={value.value}
            className={cn(
              "flex items-center gap-1.5 rounded-full border bg-surface py-1 pe-1 ps-3 text-sm",
              value.inStock ? "border-border" : "border-dashed border-danger/50 text-muted",
              option.priced && "w-full max-w-md rounded-xl",
            )}
          >
            <span dir="auto" className={cn("font-bold", !value.inStock && "line-through", option.priced && "flex-1")}>
              {value.value}
            </span>
            {option.priced && (
              <Input
                type="number"
                inputMode="decimal"
                min={0}
                step="0.01"
                value={value.price}
                onChange={(e) => setValue(index, { price: e.target.value })}
                aria-label={fmt(f.valuePrice, { value: value.value })}
                placeholder={basePrice || t.products.form.price}
                className="h-8 w-28 text-sm"
              />
            )}
            <button
              type="button"
              onClick={() => setValue(index, { inStock: !value.inStock })}
              aria-pressed={!value.inStock}
              aria-label={fmt(value.inStock ? f.markSoldOut : f.markAvailable, { value: value.value })}
              title={fmt(value.inStock ? f.markSoldOut : f.markAvailable, { value: value.value })}
              className={cn(
                "rounded-full px-2 py-0.5 text-[11px] font-bold transition",
                value.inStock ? "bg-success-soft text-success hover:bg-success-soft/70" : "bg-danger-soft text-danger",
              )}
            >
              {value.inStock ? t.products.inStock : t.products.soldOut}
            </button>
            <button
              type="button"
              onClick={() => onChange({ values: option.values.filter((_, i) => i !== index) })}
              aria-label={fmt(f.removeValue, { value: value.value })}
              className="grid size-6 place-items-center rounded-full text-muted hover:bg-danger-soft hover:text-danger"
            >
              <X size={13} aria-hidden />
            </button>
          </li>
        ))}
      </ul>

      <div className="mt-3 flex gap-2">
        <div className="relative flex-1">
          <Tag size={14} className="pointer-events-none absolute start-3 top-1/2 -translate-y-1/2 text-muted" aria-hidden />
          <Input
            id={`${id}-value`}
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter" && !e.nativeEvent.isComposing) {
                e.preventDefault();
                addValues(draft);
              }
            }}
            onBlur={() => draft.trim() && addValues(draft)}
            onPaste={(e) => {
              const text = e.clipboardData.getData("text");
              if (/[,،\n]/.test(text)) {
                e.preventDefault();
                addValues(text);
              }
            }}
            placeholder={option.examples ? fmt(f.valueExamples, { examples: option.examples }) : f.valuePlaceholder}
            aria-label={`${option.name || f.optionName}: ${f.valuePlaceholder}`}
            maxLength={100}
            className="ps-9"
          />
        </div>
        <Button variant="secondary" onClick={() => addValues(draft)} disabled={!draft.trim()}>
          {f.addValue}
        </Button>
      </div>

      {error && (
        <p className="mt-2 text-xs font-bold text-danger" role="alert">
          {error}
        </p>
      )}
    </fieldset>
  );
}
