import type { components } from "@/shared/api/schema";

type Schemas = components["schemas"];

/** Generated from the backend's OpenAPI; these fields are always present in responses. */
export type ConversationSummary = Schemas["ConversationSummary"] &
  Required<Pick<Schemas["ConversationSummary"], "id" | "customerWaId" | "simulated" | "lastMessage" | "lastDirection" | "lastMessageAt" | "messageCount">>;

export type ChatMessage = Schemas["ChatMessage"] &
  Required<Pick<Schemas["ChatMessage"], "id" | "direction" | "body" | "sentAt">>;

export type ConversationDetail = Omit<Schemas["ConversationDetail"], "messages"> &
  Required<Pick<Schemas["ConversationDetail"], "id" | "customerWaId" | "simulated">> & { messages: ChatMessage[] };
