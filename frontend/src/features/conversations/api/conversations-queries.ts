import "server-only";
import { serverApi } from "@/shared/lib/api-client.server";
import type { ConversationDetail, ConversationSummary } from "../types";

const BASE = "/api/conversations";
const FRESH: RequestInit = { cache: "no-store" };

/** Server-side reads (the inbox is read-only for now). */
export const conversationsQueries = {
  list: () => serverApi.get<ConversationSummary[]>(BASE, FRESH),
  get: (id: string) => serverApi.get<ConversationDetail>(`${BASE}/${id}`, FRESH),
};
