"use client";

import { AlertTriangle, Camera, ChevronLeft, ChevronRight, ImagePlus, Pencil, RotateCw, Star, Trash2 } from "lucide-react";
import { useRef, useState } from "react";
import { fmt } from "@/shared/i18n/config";
import { useI18n } from "@/shared/i18n/provider";
import { cn } from "@/shared/lib/cn";
import { productImageUrl } from "@/shared/lib/product-image";
import { Button } from "@/shared/ui";
import { MAX_PHOTOS, type GalleryItem, type PhotoGalleryState } from "../use-photo-gallery";
import { PhotoEditor } from "./photo-editor";

/**
 * The product's photos: drop or pick files (or use the phone camera), each opens the editor first.
 * The first photo is the main one. Reorder by dragging, or with the arrow buttons (keyboard and touch).
 */
export function PhotoGallery({ photos }: { photos: PhotoGalleryState }) {
  const { t } = useI18n();
  const p = t.products.photos;
  const files = useRef<HTMLInputElement>(null);
  const camera = useRef<HTMLInputElement>(null);
  const [dropping, setDropping] = useState(false);
  const [dragKey, setDragKey] = useState<string | null>(null);
  const full = photos.items.length >= MAX_PHOTOS;

  function pick(list: FileList | null) {
    if (list && list.length > 0) photos.addFiles(list);
  }

  async function confirmRemove(item: GalleryItem) {
    if (item.kind === "saved" && !window.confirm(p.confirmRemove)) return;
    await photos.remove(item.key);
  }

  return (
    <div className="space-y-3">
      <p className="text-xs text-muted">{p.hint}</p>

      <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
        {photos.items.map((item, index) => (
          <li
            key={item.key}
            draggable={item.kind === "saved" || item.status === "pending"}
            onDragStart={(event) => {
              setDragKey(item.key);
              event.dataTransfer.effectAllowed = "move";
            }}
            onDragOver={(event) => {
              if (dragKey && dragKey !== item.key) event.preventDefault();
            }}
            onDrop={(event) => {
              event.preventDefault();
              if (dragKey) void photos.moveTo(dragKey, index);
              setDragKey(null);
            }}
            onDragEnd={() => setDragKey(null)}
            className={cn(
              "group relative aspect-square overflow-hidden rounded-xl border bg-white shadow-sm transition",
              index === 0 ? "border-gold-500 ring-2 ring-gold-500/40" : "border-border",
              dragKey === item.key && "opacity-40",
            )}
          >
            <PhotoThumb item={item} />

            {index === 0 && (
              <span className="absolute start-2 top-2 inline-flex items-center gap-1 rounded-full bg-gold-500 px-2 py-0.5 text-[11px] font-extrabold text-brand-900 shadow">
                <Star size={11} aria-hidden className="fill-current" />
                {p.main}
              </span>
            )}

            {item.kind === "local" && item.status !== "pending" && <UploadState item={item} onRetry={() => photos.retry(item.key)} />}
            {item.kind === "local" && item.status === "pending" && (
              <span className="absolute inset-x-2 bottom-12 rounded-md bg-black/55 px-2 py-0.5 text-center text-[11px] font-bold text-white">
                {p.pending}
              </span>
            )}

            <div className="absolute inset-x-0 bottom-0 flex items-center justify-between gap-1 bg-gradient-to-t from-black/70 to-transparent p-2 pt-6 opacity-100 transition sm:opacity-0 sm:group-focus-within:opacity-100 sm:group-hover:opacity-100">
              <div className="flex gap-1">
                <IconButton label={p.moveBefore} onClick={() => photos.moveTo(item.key, index - 1)} disabled={index === 0}>
                  <ChevronRight size={15} aria-hidden className="ltr:rotate-180" />
                </IconButton>
                <IconButton
                  label={p.moveAfter}
                  onClick={() => photos.moveTo(item.key, index + 1)}
                  disabled={index === photos.items.length - 1}
                >
                  <ChevronLeft size={15} aria-hidden className="ltr:rotate-180" />
                </IconButton>
                {index > 0 && (
                  <IconButton label={p.makeMain} onClick={() => photos.moveTo(item.key, 0)}>
                    <Star size={15} aria-hidden />
                  </IconButton>
                )}
              </div>
              <div className="flex gap-1">
                <IconButton label={p.edit} onClick={() => void photos.edit(item.key)} disabled={isUploading(item)}>
                  <Pencil size={15} aria-hidden />
                </IconButton>
                <IconButton label={p.remove} onClick={() => void confirmRemove(item)} disabled={isUploading(item)} danger>
                  <Trash2 size={15} aria-hidden />
                </IconButton>
              </div>
            </div>
          </li>
        ))}

        {!full && (
          <li className="aspect-square">
            <div
              onDragOver={(event) => {
                if (event.dataTransfer.types.includes("Files")) {
                  event.preventDefault();
                  setDropping(true);
                }
              }}
              onDragLeave={() => setDropping(false)}
              onDrop={(event) => {
                event.preventDefault();
                setDropping(false);
                pick(event.dataTransfer.files);
              }}
              className={cn(
                "flex h-full flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed p-3 text-center transition",
                dropping ? "border-primary bg-primary-soft" : "border-border bg-background hover:border-primary/60",
              )}
            >
              <ImagePlus size={26} className="text-primary" aria-hidden />
              <p className="text-xs text-muted">{p.drop}</p>
              <div className="flex flex-wrap justify-center gap-1.5">
                <Button size="sm" variant="outline" onClick={() => files.current?.click()}>
                  {p.choose}
                </Button>
                <Button size="sm" variant="ghost" className="sm:hidden" onClick={() => camera.current?.click()}>
                  <Camera size={14} aria-hidden />
                  {p.camera}
                </Button>
              </div>
            </div>
          </li>
        )}
      </ul>

      <input
        ref={files}
        type="file"
        accept="image/*"
        multiple
        hidden
        onChange={(event) => {
          pick(event.target.files);
          event.target.value = "";
        }}
      />
      <input
        ref={camera}
        type="file"
        accept="image/*"
        capture="environment"
        hidden
        onChange={(event) => {
          pick(event.target.files);
          event.target.value = "";
        }}
      />

      {(photos.error || full) && (
        <p className="flex items-center gap-2 text-xs font-bold text-muted" role="status">
          <AlertTriangle size={14} aria-hidden className="text-gold-600" />
          {photos.error === "order" ? t.errors.server : p.limit}
        </p>
      )}

      {photos.editing && (
        <PhotoEditor
          key={photos.editing.id}
          source={photos.editing.source}
          step={photos.editingStep}
          onApply={photos.applyEdit}
          onCancel={photos.skipEdit}
        />
      )}
    </div>
  );
}

function isUploading(item: GalleryItem) {
  return item.kind === "local" && item.status === "uploading";
}

/** Thumbnail with a soft skeleton until it has loaded. */
function PhotoThumb({ item }: { item: GalleryItem }) {
  const [loaded, setLoaded] = useState(false);
  const src = item.kind === "saved" ? productImageUrl(item.image.id, "thumb") : item.url;
  return (
    <>
      {!loaded && <span className="absolute inset-0 animate-pulse bg-secondary" aria-hidden />}
      {/* eslint-disable-next-line @next/next/no-img-element -- next/image's optimizer can't send the session */}
      <img
        src={src}
        alt=""
        draggable={false}
        onLoad={() => setLoaded(true)}
        className={cn("size-full object-cover transition-opacity duration-300", loaded ? "opacity-100" : "opacity-0")}
      />
    </>
  );
}

function UploadState({ item, onRetry }: { item: Extract<GalleryItem, { kind: "local" }>; onRetry: () => void }) {
  const { t } = useI18n();
  if (item.status === "failed") {
    return (
      <div className="absolute inset-0 flex flex-col items-center justify-center gap-2 bg-black/60 text-white" role="alert">
        <p className="text-xs font-bold">{t.products.photos.failed}</p>
        <Button size="sm" variant="gold" onClick={onRetry}>
          <RotateCw size={14} aria-hidden />
          {t.products.photos.retry}
        </Button>
      </div>
    );
  }
  const percent = Math.round(item.progress * 100);
  return (
    <div className="absolute inset-x-2 top-1/2 -translate-y-1/2 rounded-lg bg-black/60 p-2 text-white" role="status">
      <p className="mb-1 text-center text-[11px] font-bold" data-ltr>
        {fmt(t.products.photos.uploading, { percent })}
      </p>
      <div className="h-1.5 overflow-hidden rounded-full bg-white/30">
        <div className="h-full rounded-full bg-gold-500 transition-[width]" style={{ width: `${percent}%` }} />
      </div>
    </div>
  );
}

function IconButton({
  label,
  onClick,
  disabled,
  danger,
  children,
}: {
  label: string;
  onClick: () => void;
  disabled?: boolean;
  danger?: boolean;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      aria-label={label}
      title={label}
      className={cn(
        "grid size-8 place-items-center rounded-full bg-white/90 text-brand-800 shadow transition hover:bg-white disabled:opacity-40",
        "focus-visible:outline-2 focus-visible:outline-gold-500",
        danger && "text-danger",
      )}
    >
      {children}
    </button>
  );
}
