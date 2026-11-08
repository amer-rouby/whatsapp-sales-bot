"use client";

import { Plus, Trash2 } from "lucide-react";
import { useI18n } from "@/shared/i18n/provider";
import { Button, Input } from "@/shared/ui";

export interface SpecDraft {
  key: string;
  name: string;
  value: string;
}

let counter = 0;
export const specKey = () => `spec-${++counter}`;

/** Fixed facts about the product (brand, warranty, material...) as name/value rows. */
export function SpecsEditor({
  specs,
  onChange,
  errors,
  suggestions,
}: {
  specs: SpecDraft[];
  onChange: (specs: SpecDraft[]) => void;
  errors: Record<string, string>;
  suggestions: string[];
}) {
  const { t } = useI18n();
  const f = t.products.form;
  const unused = suggestions.filter((s) => !specs.some((x) => x.name.trim() === s));

  function update(key: string, change: Partial<SpecDraft>) {
    onChange(specs.map((s) => (s.key === key ? { ...s, ...change } : s)));
  }

  return (
    <div className="space-y-3">
      <p className="text-xs text-muted">{f.specsHint}</p>
      {specs.length > 0 && (
        <ul className="space-y-2">
          {specs.map((spec) => (
            <li key={spec.key}>
              <div className="grid grid-cols-[minmax(0,2fr)_minmax(0,3fr)_auto] gap-2">
                <Input
                  value={spec.name}
                  onChange={(e) => update(spec.key, { name: e.target.value })}
                  placeholder={f.specName}
                  aria-label={f.specName}
                  maxLength={100}
                  aria-invalid={!!errors[spec.key]}
                />
                <Input
                  value={spec.value}
                  onChange={(e) => update(spec.key, { value: e.target.value })}
                  placeholder={f.specValue}
                  aria-label={spec.name ? `${spec.name}: ${f.specValue}` : f.specValue}
                  maxLength={300}
                  dir="auto"
                  aria-invalid={!!errors[spec.key]}
                />
                <Button
                  variant="danger"
                  size="icon"
                  onClick={() => onChange(specs.filter((s) => s.key !== spec.key))}
                  aria-label={f.removeSpec}
                  title={f.removeSpec}
                >
                  <Trash2 size={16} aria-hidden />
                </Button>
              </div>
              {errors[spec.key] && (
                <p className="mt-1 text-xs font-bold text-danger" role="alert">
                  {errors[spec.key]}
                </p>
              )}
            </li>
          ))}
        </ul>
      )}
      <div className="flex flex-wrap items-center gap-2">
        <Button variant="outline" size="sm" onClick={() => onChange([...specs, { key: specKey(), name: "", value: "" }])}>
          <Plus size={14} aria-hidden />
          {f.addSpec}
        </Button>
        {unused.length > 0 && (
          <>
            <span className="text-xs text-muted">{f.suggestions}</span>
            {unused.map((name) => (
              <button
                key={name}
                type="button"
                onClick={() => onChange([...specs, { key: specKey(), name, value: "" }])}
                className="rounded-full border border-dashed border-primary/40 px-3 py-1 text-xs font-bold text-primary hover:bg-primary-soft"
              >
                + {name}
              </button>
            ))}
          </>
        )}
      </div>
    </div>
  );
}
