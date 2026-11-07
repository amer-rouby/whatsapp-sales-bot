/** Matches the backend `conversations` table. */
export interface ConversationSummary {
  id: string;
  customerName?: string;
  customerWaId: string;
  lastMessage: string;
  updatedAt: string;
}
