"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { productImageUrl } from "@/shared/lib/product-image";
import { productsApi } from "./api/products-api";
import type { ProductImage } from "./types";

export const MAX_PHOTOS = 8;

/** A photo on the server, or one edited in the browser that isn't uploaded yet. */
export type GalleryItem =
  | { key: string; kind: "saved"; image: ProductImage }
  | {
      key: string;
      kind: "local";
      blob: Blob;
      url: string;
      status: "pending" | "uploading" | "failed";
      progress: number;
      /** when this is an edit of a saved photo: the one it replaces (removed once this one is up) */
      replaces?: string;
    };

/** Photos picked but not edited yet (the editor opens for each in turn). */
interface EditJob {
  id: string;
  source: Blob;
  /** set when re-editing an existing item */
  itemKey?: string;
}

let counter = 0;
const nextKey = () => `photo-${++counter}`;

/**
 * Everything the product form does with photos. For a saved product each change goes to the server at once;
 * for a new product photos wait in the browser and `uploadPending` sends them right after the product is created.
 */
export function usePhotoGallery(productId: string | undefined, initial: ProductImage[] = []) {
  const [items, setItems] = useState<GalleryItem[]>(() => initial.map((image) => ({ key: image.id, kind: "saved", image })));
  const [queue, setQueue] = useState<EditJob[]>([]);
  const [queueSize, setQueueSize] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const itemsRef = useRef(items);
  useEffect(() => {
    itemsRef.current = items;
  }, [items]);

  // free the in-memory previews when the form goes away
  useEffect(
    () => () => {
      for (const item of itemsRef.current) if (item.kind === "local") URL.revokeObjectURL(item.url);
    },
    [],
  );

  const patch = useCallback((key: string, change: Partial<Extract<GalleryItem, { kind: "local" }>>) => {
    setItems((current) => current.map((i) => (i.key === key && i.kind === "local" ? { ...i, ...change } : i)));
  }, []);

  /** Saves the current order of the saved photos (the first one is the main photo). */
  const syncOrder = useCallback(
    async (list: GalleryItem[]) => {
      if (!productId) return;
      const ids = list.filter((i) => i.kind === "saved").map((i) => (i as { image: ProductImage }).image.id);
      if (ids.length > 1) await productsApi.reorderImages(productId, ids);
    },
    [productId],
  );

  const upload = useCallback(
    async (id: string, item: Extract<GalleryItem, { kind: "local" }>) => {
      patch(item.key, { status: "uploading", progress: 0 });
      try {
        const image = await productsApi.uploadImage(id, item.blob, (progress) => patch(item.key, { progress }));
        URL.revokeObjectURL(item.url);
        const settle = (list: GalleryItem[]): GalleryItem[] =>
          list
            .filter((i) => i.key !== item.replaces)
            .map((i) => (i.key === item.key ? { key: image.id, kind: "saved", image } : i));
        setItems(settle);
        const next = settle(itemsRef.current);
        if (item.replaces) {
          // the server appended the new photo: put it where the old one was, then drop the old one
          await syncOrder(next);
          await productsApi.removeImage(id, item.replaces);
        } else if (next.findIndex((i) => i.key === image.id) !== next.filter((i) => i.kind === "saved").length - 1) {
          await syncOrder(next);
        }
        return true;
      } catch {
        patch(item.key, { status: "failed" });
        return false;
      }
    },
    [patch, syncOrder],
  );

  /** Opens the editor for each picked file, in order (up to the limit). */
  const addFiles = useCallback((files: FileList | File[]) => {
    const room = MAX_PHOTOS - itemsRef.current.length;
    const picked = [...files].slice(0, Math.max(0, room));
    setError(picked.length < files.length ? "limit" : null);
    if (picked.length === 0) return;
    setQueue((current) => [...current, ...picked.map((source) => ({ id: nextKey(), source }))]);
    setQueueSize((size) => size + picked.length);
  }, []);

  /** Re-opens the editor on an existing photo (saved photos are fetched in full size again). */
  const edit = useCallback(async (key: string) => {
    const item = itemsRef.current.find((i) => i.key === key);
    if (!item) return;
    const source = item.kind === "local" ? item.blob : await fetch(productImageUrl(item.image.id, "full")).then((r) => r.blob());
    setQueue([{ id: nextKey(), source, itemKey: key }]);
    setQueueSize(1);
  }, []);

  /** The editor finished the current job. */
  const applyEdit = useCallback(
    (blob: Blob) => {
      const job = queue[0];
      setQueue((current) => current.slice(1));
      if (queue.length <= 1) setQueueSize(0);
      if (!job) return;

      const url = URL.createObjectURL(blob);
      const existing = job.itemKey ? itemsRef.current.find((i) => i.key === job.itemKey) : undefined;
      let item: Extract<GalleryItem, { kind: "local" }>;
      if (existing?.kind === "local") {
        URL.revokeObjectURL(existing.url);
        item = { ...existing, blob, url, status: "pending", progress: 0 };
        setItems((current) => current.map((i) => (i.key === existing.key ? item : i)));
      } else {
        item = { key: nextKey(), kind: "local", blob, url, status: "pending", progress: 0, replaces: existing?.key };
        // an edited saved photo takes its place in the gallery; a new one goes last
        setItems((current) =>
          existing ? current.map((i) => (i.key === existing.key ? item : i)) : [...current, item],
        );
      }
      if (productId) void upload(productId, item);
    },
    [queue, productId, upload],
  );

  const skipEdit = useCallback(() => {
    setQueue((current) => current.slice(1));
    if (queue.length <= 1) setQueueSize(0);
  }, [queue.length]);

  const remove = useCallback(
    async (key: string) => {
      const item = itemsRef.current.find((i) => i.key === key);
      if (!item) return;
      if (item.kind === "saved" && productId) {
        await productsApi.removeImage(productId, item.image.id);
      }
      if (item.kind === "local") URL.revokeObjectURL(item.url);
      setItems((current) => current.filter((i) => i.key !== key));
    },
    [productId],
  );

  /** Moves a photo to a position (0 = main photo) and saves the order. */
  const moveTo = useCallback(
    async (key: string, index: number) => {
      const current = itemsRef.current;
      const from = current.findIndex((i) => i.key === key);
      if (from < 0) return;
      const to = Math.max(0, Math.min(current.length - 1, index));
      if (from === to) return;
      const next = [...current];
      const [moved] = next.splice(from, 1);
      next.splice(to, 0, moved);
      setItems(next);
      try {
        await syncOrder(next);
      } catch {
        setItems(current);
        setError("order");
      }
    },
    [syncOrder],
  );

  /** For a new product: uploads the waiting photos in gallery order. @returns false when any failed */
  const uploadPending = useCallback(
    async (id: string) => {
      let ok = true;
      for (const item of itemsRef.current) {
        if (item.kind === "local" && item.status !== "uploading") {
          ok = (await upload(id, item)) && ok;
        }
      }
      return ok;
    },
    [upload],
  );

  const retry = useCallback(
    (key: string) => {
      const item = itemsRef.current.find((i) => i.key === key);
      if (item?.kind === "local" && productId) void upload(productId, item);
    },
    [productId, upload],
  );

  return {
    items,
    error,
    clearError: () => setError(null),
    editing: queue[0] ?? null,
    editingStep: queue.length > 0 ? { current: queueSize - queue.length + 1, total: queueSize } : undefined,
    busy: items.some((i) => i.kind === "local" && i.status === "uploading"),
    addFiles,
    edit,
    applyEdit,
    skipEdit,
    remove,
    moveTo,
    retry,
    uploadPending,
  };
}

export type PhotoGalleryState = ReturnType<typeof usePhotoGallery>;
