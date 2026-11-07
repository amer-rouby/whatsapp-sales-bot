import { getI18n } from "@/shared/i18n/server";
import { PageHeader } from "@/shared/ui";
import { ChatSimulator } from "./chat-simulator";

export async function SimulatorPage() {
  const { t } = await getI18n();
  return (
    <div className="flex flex-col gap-6 animate-fade-up">
      <PageHeader title={t.simulator.title} description={t.simulator.description} />
      <ChatSimulator />
    </div>
  );
}
