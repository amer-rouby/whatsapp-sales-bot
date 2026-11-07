"use client";

import { Trash2 } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { fmt } from "@/shared/i18n/config";
import { useI18n } from "@/shared/i18n/provider";
import { errorMessage } from "@/shared/lib/errors";
import { Button } from "@/shared/ui";
import { productsApi } from "../api/products-api";

export function DeleteProductButton({ id, name }: { id: string; name: string }) {
  const router = useRouter();
  const { t } = useI18n();
  const [busy, setBusy] = useState(false);

  async function remove() {
    if (!window.confirm(fmt(t.products.confirmDelete, { name }))) return;
    setBusy(true);
    try {
      await productsApi.remove(id);
      router.refresh();
    } catch (error) {
      window.alert(errorMessage(error, t));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Button variant="danger" size="sm" onClick={remove} disabled={busy}>
      <Trash2 size={14} aria-hidden />
      {t.common.delete}
    </Button>
  );
}
