import { apiClient } from "@/shared/lib/api-client";

export interface SimulatorMessage {
  customerId: string;
  customerName?: string;
  text: string;
}

/** Runs the message through the real bot pipeline and resolves with the bot's reply. */
export async function sendSimulatorMessage(message: SimulatorMessage): Promise<string> {
  const { reply } = await apiClient.post<{ reply: string }>("/dev/simulator/messages", message);
  return reply;
}
