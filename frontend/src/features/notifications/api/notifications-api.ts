import { apiClient } from "@/shared/lib/api-client";
import type { NotificationPage } from "../types";

/** Polled from the browser (through proxy.ts). Pass the previous page's serverTime as `after`. */
export const notificationsApi = {
  feed: (after: string | null) =>
    apiClient.get<NotificationPage>(`/api/notifications${after ? `?after=${encodeURIComponent(after)}` : ""}`, {
      cache: "no-store",
    }),
};
