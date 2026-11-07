"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { errorMessage } from "@/shared/lib/errors";
import { Button } from "@/shared/ui";
import { productsApi } from "../api/products-api";

export function DeleteProductButton({ id, name }: { id: string; name: string }) {
  const router = useRouter();
  const [busy, setBusy] = useState(false);

  async function remove() {
    if (!window.confirm(`تمسح "${name}"؟ البوت مش هيعرضه للعملاء تاني.`)) return;
    setBusy(true);
    try {
      await productsApi.remove(id);
      router.refresh();
    } catch (error) {
      window.alert(errorMessage(error));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Button variant="ghost" size="sm" className="text-danger" onClick={remove} disabled={busy}>
      مسح
    </Button>
  );
}
