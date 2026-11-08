import type { components } from "@/shared/api/schema";

type Schemas = components["schemas"];
type ProductResponse = Schemas["ProductResponse"];

/** Generated from the backend's OpenAPI; these fields are always present in responses. */
export type Product = Omit<ProductResponse, "images"> &
  Required<Pick<ProductResponse, "id" | "name" | "price" | "currency" | "inStock" | "options" | "specs">> & {
    images: ProductImage[];
  };

export type ProductOption = Schemas["ProductOption"];
export type OptionValue = Schemas["OptionValue"];
export type ProductSpec = Schemas["ProductSpec"];
export type ProductImage = Required<Schemas["ImageResponse"]>;

/** Body for create/update. */
export type ProductInput = Schemas["ProductRequest"];
