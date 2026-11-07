import { PageHeader } from "@/shared/ui";
import { ChatSimulator } from "./chat-simulator";

export function SimulatorPage() {
  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="جرّب البوت"
        description="كلّم البوت كأنك عميل من غير واتساب. الرسالة بتعدّي على نفس الخطوات اللي رسايل واتساب الحقيقية بتعدّي عليها."
      />
      <ChatSimulator />
    </div>
  );
}
