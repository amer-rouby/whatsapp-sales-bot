import { ConversationsLayout } from "@/features/conversations";

export default function Layout({ children }: { children: React.ReactNode }) {
  return <ConversationsLayout>{children}</ConversationsLayout>;
}
