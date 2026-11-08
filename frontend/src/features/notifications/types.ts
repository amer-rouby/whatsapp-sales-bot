import type { components } from "@/shared/api/schema";

type Schemas = components["schemas"];

/** Generated from the backend's OpenAPI; these fields are always present in responses. */
export type NotificationItem = Schemas["NotificationItem"] &
  Required<Pick<Schemas["NotificationItem"], "id" | "type" | "customerContact" | "needsAttention" | "simulated" | "at">>;

export interface NotificationPage {
  serverTime: string;
  items: NotificationItem[];
}
