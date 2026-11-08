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

/** Messages the shop sent to this chat since the last call (e.g. replies written in the dashboard). */
export async function pendingSimulatorReplies(customerId: string): Promise<string[]> {
  const { replies } = await apiClient.get<{ replies: string[] }>(`/dev/simulator/messages/${encodeURIComponent(customerId)}`, {
    cache: "no-store",
  });
  return replies;
}
