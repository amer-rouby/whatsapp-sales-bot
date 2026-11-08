"use client";

import { Crop, FlipHorizontal2, Palette, RotateCcw, RotateCw, Sparkles, X } from "lucide-react";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { fmt } from "@/shared/i18n/config";
import { useI18n } from "@/shared/i18n/provider";
import { cn } from "@/shared/lib/cn";
import { Button } from "@/shared/ui";
import {
  autoEnhance,
  clamp,
  clampCenter,
  cropRect,
  EXPORT_SIDE,
  exportJpeg,
  frameRatio,
  initialState,
  isNeutral,
  loadPhoto,
  NEUTRAL,
  orient,
  previewOf,
  renderFrame,
  type Adjustments,
  type Aspect,
  type EditState,
} from "../lib/image-pipeline";

const MAX_ZOOM = 5;
const SLIDERS: Array<keyof Adjustments> = ["brightness", "contrast", "saturation", "warmth"];

export interface PhotoEditorProps {
  /** the photo to edit (a picked file, or a saved photo fetched again) */
  source: Blob;
  /** e.g. "1 of 3" when several photos were picked at once */
  step?: { current: number; total: number };
  onApply: (photo: Blob) => void;
  onCancel: () => void;
}

/** Crop, turn and colour-correct one photo, then hand back a ready JPEG. */
export function PhotoEditor({ source, step, onApply, onCancel }: PhotoEditorProps) {
  const { t } = useI18n();
  const e = t.products.editor;
  const dialog = useRef<HTMLDialogElement>(null);
  const [base, setBase] = useState<HTMLCanvasElement | null>(null);
  const [failed, setFailed] = useState(false);
  const [state, setState] = useState<EditState>(initialState);
  const [tab, setTab] = useState<"crop" | "colours">("crop");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    dialog.current?.showModal();
  }, []);

  // the gallery mounts a fresh editor (new key) for every photo, so state starts clean
  useEffect(() => {
    let cancelled = false;
    loadPhoto(source)
      .then((canvas) => !cancelled && setBase(canvas))
      .catch(() => !cancelled && setFailed(true));
    return () => {
      cancelled = true;
    };
  }, [source]);

  const oriented = useMemo(() => (base ? orient(base, state.rotation, state.flipX) : null), [base, state.rotation, state.flipX]);

  // colour-adjusted preview, recomputed at most once per frame while a slider moves
  const [preview, setPreview] = useState<HTMLCanvasElement | null>(null);
  useEffect(() => {
    if (!oriented) return;
    const id = requestAnimationFrame(() => setPreview(previewOf(oriented, state.adjustments, state.levels)));
    return () => cancelAnimationFrame(id);
  }, [oriented, state.adjustments, state.levels]);

  const update = useCallback(
    (change: Partial<EditState>) =>
      setState((current) => {
        const next = { ...current, ...change };
        if (!oriented) return next;
        return { ...next, ...clampCenter(oriented.width, oriented.height, next) };
      }),
    [oriented],
  );

  async function apply() {
    if (!oriented) return;
    setBusy(true);
    try {
      // let the "preparing" state paint before the heavy pixel work (a timer, not a frame: frames pause in background tabs)
      await new Promise((r) => setTimeout(r, 30));
      onApply(await exportJpeg(renderFrame(oriented, state, EXPORT_SIDE)));
    } finally {
      setBusy(false);
    }
  }

  function rotate(by: 90 | -90) {
    update({ rotation: (((state.rotation + by) % 360) + 360) % 360 as EditState["rotation"], zoom: 1, centerX: 0.5, centerY: 0.5 });
  }

  function auto() {
    if (!oriented) return;
    const enhanced = autoEnhance(oriented);
    update({ levels: enhanced.levels, adjustments: enhanced.adjustments });
  }

  const canCrop = state.mode === "crop" || state.aspect === "original";

  return (
    <dialog
      ref={dialog}
      onCancel={(event) => {
        event.preventDefault();
        if (!busy) onCancel();
      }}
      aria-labelledby="photo-editor-title"
      className="m-auto max-h-[100dvh] w-full max-w-3xl overflow-hidden rounded-none bg-surface p-0 text-foreground shadow-2xl backdrop:bg-brand-900/70 backdrop:backdrop-blur-sm sm:max-h-[94dvh] sm:rounded-2xl"
    >
      <div className="flex max-h-[100dvh] flex-col sm:max-h-[94dvh]">
        <header className="flex items-center gap-3 border-b border-border px-4 py-3">
          <h2 id="photo-editor-title" className="flex-1 font-bold text-brand-800">
            {e.title}
            {step && step.total > 1 && (
              <span className="ms-2 text-xs font-normal text-muted" data-ltr>
                {step.current}/{step.total}
              </span>
            )}
          </h2>
          <Button variant="ghost" size="icon" onClick={onCancel} disabled={busy} aria-label={t.common.cancel}>
            <X size={18} aria-hidden />
          </Button>
        </header>

        <div className="min-h-0 flex-1 overflow-y-auto">
          <div className="checkerboard grid place-items-center p-3 sm:p-6">
            {failed ? (
              <p className="py-16 text-center text-sm font-bold text-danger" role="alert">
                {fmt(t.products.photos.unsupported, { name: source instanceof File ? source.name : "" })}
              </p>
            ) : oriented && preview ? (
              <CropView oriented={oriented} preview={preview} state={state} canCrop={canCrop} onChange={update} label={e.dragHint} />
            ) : (
              <div className="aspect-square w-[min(32rem,100%)] animate-pulse rounded-xl bg-secondary" aria-label={e.processing} />
            )}
          </div>

          <div className="border-t border-border">
            <div role="tablist" className="flex gap-1 px-3 pt-3">
              <TabButton active={tab === "crop"} onClick={() => setTab("crop")} icon={Crop}>
                {e.crop}
              </TabButton>
              <TabButton active={tab === "colours"} onClick={() => setTab("colours")} icon={Palette}>
                {e.colours}
              </TabButton>
            </div>

            {tab === "crop" ? (
              <div className="grid gap-4 p-4 sm:grid-cols-2">
                <Segmented
                  label={e.aspect}
                  value={state.aspect}
                  options={[
                    ["square", e.square],
                    ["portrait", e.portrait],
                    ["original", e.original],
                  ]}
                  onChange={(aspect) => update({ aspect: aspect as Aspect, zoom: 1, centerX: 0.5, centerY: 0.5 })}
                />
                {state.aspect !== "original" && (
                  <Segmented
                    label={e.cropMode}
                    value={state.mode}
                    options={[
                      ["crop", e.cropMode],
                      ["fit", e.fitMode],
                    ]}
                    onChange={(mode) => update({ mode: mode as EditState["mode"], zoom: 1 })}
                  />
                )}
                <Slider
                  id="zoom"
                  label={e.zoom}
                  min={1}
                  max={MAX_ZOOM}
                  step={0.01}
                  value={state.zoom}
                  disabled={!canCrop}
                  display={`${Math.round(state.zoom * 100)}%`}
                  onChange={(zoom) => update({ zoom })}
                />
                <div className="flex items-end gap-2">
                  <Button variant="secondary" size="sm" onClick={() => rotate(-90)} aria-label={`${e.rotate} ↺`}>
                    <RotateCcw size={16} aria-hidden />
                  </Button>
                  <Button variant="secondary" size="sm" onClick={() => rotate(90)}>
                    <RotateCw size={16} aria-hidden />
                    {e.rotate}
                  </Button>
                  <Button
                    variant={state.flipX ? "outline" : "secondary"}
                    size="sm"
                    aria-pressed={state.flipX}
                    onClick={() => update({ flipX: !state.flipX })}
                  >
                    <FlipHorizontal2 size={16} aria-hidden />
                    {e.flip}
                  </Button>
                </div>
              </div>
            ) : (
              <div className="grid gap-4 p-4 sm:grid-cols-2">
                <div className="flex flex-wrap gap-2 sm:col-span-2">
                  <Button variant="gold" size="sm" onClick={auto} disabled={!oriented}>
                    <Sparkles size={16} aria-hidden />
                    {e.auto}
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => update({ adjustments: { ...NEUTRAL }, levels: null })}
                    disabled={isNeutral(state.adjustments, state.levels)}
                  >
                    {e.reset}
                  </Button>
                </div>
                {SLIDERS.map((key) => (
                  <Slider
                    key={key}
                    id={key}
                    label={e[key]}
                    min={-100}
                    max={100}
                    step={1}
                    value={state.adjustments[key]}
                    display={state.adjustments[key] > 0 ? `+${state.adjustments[key]}` : String(state.adjustments[key])}
                    onChange={(value) => update({ adjustments: { ...state.adjustments, [key]: value } })}
                    onReset={() => update({ adjustments: { ...state.adjustments, [key]: 0 } })}
                  />
                ))}
              </div>
            )}
          </div>
        </div>

        <footer className="flex flex-wrap justify-end gap-2 border-t border-border bg-background px-4 py-3">
          <Button variant="secondary" onClick={onCancel} disabled={busy}>
            {t.common.cancel}
          </Button>
          <Button onClick={apply} disabled={!oriented || busy}>
            {busy ? e.processing : step && step.current < step.total ? fmt(e.next, { current: step.current + 1, total: step.total }) : e.apply}
          </Button>
        </footer>
      </div>
    </dialog>
  );
}

/**
 * The frame the customer will see. In crop mode the photo moves under it: drag to move, wheel or two fingers to
 * zoom, arrow keys and +/- from the keyboard.
 */
function CropView({
  oriented,
  preview,
  state,
  canCrop,
  onChange,
  label,
}: {
  oriented: HTMLCanvasElement;
  preview: HTMLCanvasElement;
  state: EditState;
  canCrop: boolean;
  onChange: (change: Partial<EditState>) => void;
  label: string;
}) {
  const frame = useRef<HTMLDivElement>(null);
  const canvas = useRef<HTMLCanvasElement>(null);
  const pointers = useRef(new Map<number, { x: number; y: number }>());
  const [frameWidth, setFrameWidth] = useState(0);
  const [dragging, setDragging] = useState(false);

  const ratio = frameRatio(state.aspect, oriented.width, oriented.height);
  const fit = state.mode === "fit" && state.aspect !== "original";

  useEffect(() => {
    const el = frame.current;
    if (!el) return;
    const observer = new ResizeObserver(([entry]) => setFrameWidth(entry.contentRect.width));
    observer.observe(el);
    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    const target = canvas.current;
    if (!target) return;
    target.width = preview.width;
    target.height = preview.height;
    target.getContext("2d")!.drawImage(preview, 0, 0);
  }, [preview]);

  // where the photo sits inside the frame, in CSS pixels
  const frameHeight = frameWidth / ratio;
  let style: React.CSSProperties;
  if (fit) {
    const scale = Math.min(frameWidth / oriented.width, frameHeight / oriented.height);
    style = {
      width: oriented.width * scale,
      height: oriented.height * scale,
      left: (frameWidth - oriented.width * scale) / 2,
      top: (frameHeight - oriented.height * scale) / 2,
    };
  } else {
    const rect = cropRect(oriented.width, oriented.height, state);
    const scale = frameWidth / rect.width;
    style = { width: oriented.width * scale, height: oriented.height * scale, left: -rect.x * scale, top: -rect.y * scale };
  }

  /** Moves the crop by a distance in CSS pixels. */
  function panBy(dx: number, dy: number) {
    const width = Number(style.width) || 1;
    const height = Number(style.height) || 1;
    onChange({ centerX: state.centerX - dx / width, centerY: state.centerY - dy / height });
  }

  function zoomTo(zoom: number) {
    onChange({ zoom: clamp(zoom, 1, MAX_ZOOM) });
  }

  function onPointerDown(event: React.PointerEvent) {
    if (!canCrop) return;
    event.currentTarget.setPointerCapture(event.pointerId);
    pointers.current.set(event.pointerId, { x: event.clientX, y: event.clientY });
    setDragging(true);
  }

  function onPointerMove(event: React.PointerEvent) {
    const previous = pointers.current.get(event.pointerId);
    if (!previous) return;
    const points = [...pointers.current.values()];
    if (points.length === 2) {
      const other = points.find((p) => p !== previous)!;
      const before = Math.hypot(previous.x - other.x, previous.y - other.y);
      const after = Math.hypot(event.clientX - other.x, event.clientY - other.y);
      if (before > 0) zoomTo(state.zoom * (after / before));
    } else {
      panBy(event.clientX - previous.x, event.clientY - previous.y);
    }
    pointers.current.set(event.pointerId, { x: event.clientX, y: event.clientY });
  }

  function onPointerUp(event: React.PointerEvent) {
    pointers.current.delete(event.pointerId);
    if (pointers.current.size === 0) setDragging(false);
  }

  // wheel zoom needs a non-passive listener so the page doesn't scroll
  useEffect(() => {
    const el = frame.current;
    if (!el || !canCrop) return;
    const onWheel = (event: WheelEvent) => {
      event.preventDefault();
      zoomTo(state.zoom * Math.exp(-event.deltaY * 0.0015));
    };
    el.addEventListener("wheel", onWheel, { passive: false });
    return () => el.removeEventListener("wheel", onWheel);
  });

  function onKeyDown(event: React.KeyboardEvent) {
    if (!canCrop) return;
    const step = event.shiftKey ? 40 : 10;
    const moves: Record<string, [number, number]> = {
      ArrowLeft: [step, 0],
      ArrowRight: [-step, 0],
      ArrowUp: [0, step],
      ArrowDown: [0, -step],
    };
    if (moves[event.key]) {
      event.preventDefault();
      panBy(...moves[event.key]);
    } else if (event.key === "+" || event.key === "=") {
      zoomTo(state.zoom * 1.1);
    } else if (event.key === "-") {
      zoomTo(state.zoom / 1.1);
    }
  }

  return (
    <div
      ref={frame}
      role="img"
      aria-label={label}
      title={canCrop ? label : undefined}
      tabIndex={canCrop ? 0 : -1}
      onPointerDown={onPointerDown}
      onPointerMove={onPointerMove}
      onPointerUp={onPointerUp}
      onPointerCancel={onPointerUp}
      onKeyDown={onKeyDown}
      dir="ltr"
      className={cn(
        "relative touch-none select-none overflow-hidden rounded-xl bg-white shadow-lg ring-1 ring-black/10",
        "focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-gold-500",
        canCrop && (dragging ? "cursor-grabbing" : "cursor-grab"),
      )}
      // as wide as fits, but never taller than 55% of the screen, so the frame keeps its exact shape
      style={{ aspectRatio: String(ratio), width: `min(32rem, 100%, calc(55dvh * ${ratio}))` }}
    >
      <canvas ref={canvas} className="pointer-events-none absolute max-w-none" style={style} />
      {canCrop && (
        // rule-of-thirds guide, stronger while moving
        <div
          aria-hidden
          className={cn(
            "pointer-events-none absolute inset-0 transition-opacity",
            "bg-[linear-gradient(to_right,transparent_33.1%,rgb(255_255_255/0.7)_33.3%,transparent_33.5%,transparent_66.4%,rgb(255_255_255/0.7)_66.6%,transparent_66.8%),linear-gradient(to_bottom,transparent_33.1%,rgb(255_255_255/0.7)_33.3%,transparent_33.5%,transparent_66.4%,rgb(255_255_255/0.7)_66.6%,transparent_66.8%)]",
            dragging ? "opacity-100" : "opacity-0",
          )}
        />
      )}
    </div>
  );
}

function TabButton({
  active,
  onClick,
  icon: Icon,
  children,
}: {
  active: boolean;
  onClick: () => void;
  icon: typeof Crop;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      role="tab"
      aria-selected={active}
      onClick={onClick}
      className={cn(
        "flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-bold transition",
        active ? "bg-primary-soft text-primary" : "text-muted hover:bg-secondary",
      )}
    >
      <Icon size={16} aria-hidden />
      {children}
    </button>
  );
}

function Segmented({
  label,
  value,
  options,
  onChange,
}: {
  label: string;
  value: string;
  options: Array<[string, string]>;
  onChange: (value: string) => void;
}) {
  return (
    <fieldset>
      <legend className="mb-1.5 text-xs font-bold text-muted">{label}</legend>
      <div className="inline-flex flex-wrap gap-1 rounded-xl bg-secondary p-1">
        {options.map(([key, text]) => (
          <button
            key={key}
            type="button"
            aria-pressed={value === key}
            onClick={() => onChange(key)}
            className={cn(
              "rounded-lg px-3 py-1.5 text-xs font-bold transition",
              value === key ? "bg-surface text-primary shadow-sm" : "text-muted hover:text-foreground",
            )}
          >
            {text}
          </button>
        ))}
      </div>
    </fieldset>
  );
}

function Slider({
  id,
  label,
  min,
  max,
  step,
  value,
  display,
  disabled,
  onChange,
  onReset,
}: {
  id: string;
  label: string;
  min: number;
  max: number;
  step: number;
  value: number;
  display: string;
  disabled?: boolean;
  onChange: (value: number) => void;
  onReset?: () => void;
}) {
  return (
    <div className={cn("flex flex-col gap-1.5", disabled && "opacity-50")}>
      <label htmlFor={`photo-${id}`} className="flex items-center justify-between text-xs font-bold text-muted">
        {label}
        <span data-ltr className="tabular-nums text-foreground">
          {display}
        </span>
      </label>
      <input
        id={`photo-${id}`}
        type="range"
        min={min}
        max={max}
        step={step}
        value={value}
        disabled={disabled}
        onChange={(event) => onChange(Number(event.target.value))}
        onDoubleClick={onReset}
        className="w-full accent-(--color-primary)"
      />
    </div>
  );
}
