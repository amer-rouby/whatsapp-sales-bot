import {
  Armchair,
  BookOpen,
  Footprints,
  Gem,
  Package,
  Shirt,
  Smartphone,
  SprayCan,
  UtensilsCrossed,
  type LucideIcon,
} from "lucide-react";
import type { Locale } from "@/shared/i18n/config";

/**
 * Ready-made product kinds. Picking one only suggests the usual options (with example values) and specs;
 * the owner can rename, add or remove anything. To support a new kind, add an entry here — nothing else changes.
 */
export interface ProductTemplate {
  id: string;
  icon: LucideIcon;
  label: Record<Locale, string>;
  /** suggested option names, with placeholder values shown as hints */
  options: Array<{ name: Record<Locale, string>; examples: string }>;
  /** suggested spec names */
  specs: Array<Record<Locale, string>>;
}

export const PRODUCT_TEMPLATES: ProductTemplate[] = [
  {
    id: "clothes",
    icon: Shirt,
    label: { ar: "ملابس", en: "Clothes" },
    options: [
      { name: { ar: "المقاس", en: "Size" }, examples: "S, M, L, XL" },
      { name: { ar: "اللون", en: "Colour" }, examples: "أبيض، أسود" },
    ],
    specs: [{ ar: "الخامة", en: "Material" }],
  },
  {
    id: "shoes",
    icon: Footprints,
    label: { ar: "أحذية", en: "Shoes" },
    options: [
      { name: { ar: "المقاس", en: "Size" }, examples: "40, 41, 42" },
      { name: { ar: "اللون", en: "Colour" }, examples: "أبيض، أسود" },
    ],
    specs: [{ ar: "الخامة", en: "Material" }],
  },
  {
    id: "electronics",
    icon: Smartphone,
    label: { ar: "موبايلات وإلكترونيات", en: "Phones & electronics" },
    options: [
      { name: { ar: "السعة", en: "Storage" }, examples: "128GB, 256GB" },
      { name: { ar: "اللون", en: "Colour" }, examples: "أسود، أزرق" },
    ],
    specs: [
      { ar: "الماركة", en: "Brand" },
      { ar: "الضمان", en: "Warranty" },
    ],
  },
  {
    id: "beauty",
    icon: SprayCan,
    label: { ar: "عطور ومستحضرات", en: "Perfume & beauty" },
    options: [{ name: { ar: "الحجم", en: "Size" }, examples: "50ml, 100ml" }],
    specs: [
      { ar: "الماركة", en: "Brand" },
      { ar: "النوع", en: "Type" },
    ],
  },
  {
    id: "food",
    icon: UtensilsCrossed,
    label: { ar: "أكل ومشروبات", en: "Food & drinks" },
    options: [{ name: { ar: "الوزن", en: "Weight" }, examples: "250 جم، 500 جم، 1 كجم" }],
    specs: [
      { ar: "المكونات", en: "Ingredients" },
      { ar: "الصلاحية", en: "Shelf life" },
    ],
  },
  {
    id: "accessories",
    icon: Gem,
    label: { ar: "إكسسوارات", en: "Accessories" },
    options: [{ name: { ar: "اللون", en: "Colour" }, examples: "ذهبي، فضي" }],
    specs: [{ ar: "الخامة", en: "Material" }],
  },
  {
    id: "furniture",
    icon: Armchair,
    label: { ar: "أثاث وديكور", en: "Furniture & decor" },
    options: [
      { name: { ar: "المقاس", en: "Dimensions" }, examples: "120×60 سم" },
      { name: { ar: "اللون", en: "Colour" }, examples: "بيج، رمادي" },
    ],
    specs: [
      { ar: "الخامة", en: "Material" },
      { ar: "الضمان", en: "Warranty" },
    ],
  },
  {
    id: "books",
    icon: BookOpen,
    label: { ar: "كتب", en: "Books" },
    options: [{ name: { ar: "النسخة", en: "Edition" }, examples: "ورقي، إلكتروني" }],
    specs: [
      { ar: "المؤلف", en: "Author" },
      { ar: "عدد الصفحات", en: "Pages" },
    ],
  },
  { id: "other", icon: Package, label: { ar: "أخرى", en: "Other" }, options: [], specs: [] },
];
