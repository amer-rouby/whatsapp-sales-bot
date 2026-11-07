import type { components } from "@/shared/api/schema";

type ProductResponse = components["schemas"]["ProductResponse"];

/** Generated from the backend's OpenAPI; these fields are always present in responses. */
export type Product = ProductResponse &
  Required<Pick<ProductResponse, "id" | "name" | "price" | "currency" | "inStock">>;

/** Body for create/update. */
export type ProductInput = components["schemas"]["ProductRequest"];
