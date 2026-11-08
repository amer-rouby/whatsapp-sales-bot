import { apiClient } from "@/shared/lib/api-client";

export interface SimulatorMessage {
  customerId: string;
  customerName?: string;
  text: string;
}

/**
 * Runs the message through the real bot pipeline and resolves with the bot's reply,
 * or null when the shop has taken this chat over (the bot stays silent).
 */
export async function sendSimulatorMessage(message: SimulatorMessage): Promise<string | null> {
  const { reply } = await apiClient.post<{ reply: string | null }>("/dev/simulator/messages", message);
  return reply ?? null;
}

/** What the shop's number sent: text, or a product photo (data URL) with the text as its caption. */
export interface SimulatorIncoming {
  text: string;
  imageUrl?: string | null;
}

/** Messages the shop sent to this chat since the last call (dashboard replies, product photos). */
export async function pendingSimulatorReplies(customerId: string): Promise<SimulatorIncoming[]> {
  const { replies } = await apiClient.get<{ replies: SimulatorIncoming[] }>(`/dev/simulator/messages/${encodeURIComponent(customerId)}`, {
    cache: "no-store",
  });
  return replies;
}
