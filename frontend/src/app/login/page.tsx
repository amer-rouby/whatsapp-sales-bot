import type { Metadata } from "next";
import { LoginPage } from "@/features/auth";
import { getI18n } from "@/shared/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  const { t } = await getI18n();
  return { title: t.auth.pageTitle };
}

export default async function Page({ searchParams }: { searchParams: Promise<{ next?: string | string[] }> }) {
  const { next } = await searchParams;
  return <LoginPage next={typeof next === "string" ? next : undefined} />;
}
