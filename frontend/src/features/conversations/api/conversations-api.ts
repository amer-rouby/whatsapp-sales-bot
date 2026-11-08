import { apiClient } from "@/shared/lib/api-client";
import type { ChatMessage } from "../types";

const BASE = "/api/conversations";

/** Changes from client components (through proxy.ts). Reads live in conversations-queries.ts. */
export const conversationsApi = {
  /** Sends on WhatsApp as the shop and pauses the bot in this chat. */
  reply: (id: string, text: string) => apiClient.post<ChatMessage>(`${BASE}/${id}/messages`, { text }),
  /** false hands the chat back to the bot. */
  setBotPaused: (id: string, paused: boolean) => apiClient.put<void>(`${BASE}/${id}/bot`, { paused }),
};
