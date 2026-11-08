/**
 * Everything the photo editor does to pixels, in the browser:
 * decode (EXIF orientation applied) → rotate/flip → crop or fit on white → colour adjustments → JPEG.
 *
 * Colour work is plain pixel math (not `ctx.filter`, which Safari ignores), so the preview and the
 * exported photo are identical on every browser.
 */

/** Largest side kept while editing; big phone photos are reduced once on load so editing stays smooth. */
const WORKING_SIDE = 2400;
/** Largest side of the exported photo (the server keeps it as is, WhatsApp shows it sharp). */
export const EXPORT_SIDE = 1200;
const EXPORT_QUALITY = 0.85;

export type Aspect = "square" | "portrait" | "original";
export type Rotation = 0 | 90 | 180 | 270;

/** -100..100, 0 = unchanged. */
export interface Adjustments {
  brightness: number;
  contrast: number;
  saturation: number;
  warmth: number;
}

/** Found by "auto enhance": input black/white points (0..255) and a gamma (> 1 lifts the mid-tones). */
export interface Levels {
  black: number;
  white: number;
  gamma: number;
}

export interface EditState {
  aspect: Aspect;
  /** "crop" fills the frame (zoom + move); "fit" shows the whole photo on a white background. */
  mode: "crop" | "fit";
  rotation: Rotation;
  flipX: boolean;
  /** ≥ 1; only used in crop mode */
  zoom: number;
  /** centre of the crop, 0..1 of the rotated photo */
  centerX: number;
  centerY: number;
  adjustments: Adjustments;
  levels: Levels | null;
}

export const NEUTRAL: Adjustments = { brightness: 0, contrast: 0, saturation: 0, warmth: 0 };

export function initialState(): EditState {
  return {
    aspect: "square",
    mode: "crop",
    rotation: 0,
    flipX: false,
    zoom: 1,
    centerX: 0.5,
    centerY: 0.5,
    adjustments: { ...NEUTRAL },
    levels: null,
  };
}

export class UnsupportedImageError extends Error {}

/** Decodes a picked file (JPEG, PNG, WebP, and HEIC where the browser can) into a canvas of at most WORKING_SIDE. */
export async function loadPhoto(file: Blob): Promise<HTMLCanvasElement> {
  const url = URL.createObjectURL(file);
  try {
    const img = new Image();
    img.decoding = "async";
    img.src = url;
    await img.decode().catch(() => {
      throw new UnsupportedImageError("This file can't be opened as a photo");
    });
    const scale = Math.min(1, WORKING_SIDE / Math.max(img.naturalWidth, img.naturalHeight));
    return drawScaled(img, Math.round(img.naturalWidth * scale), Math.round(img.naturalHeight * scale));
  } finally {
    URL.revokeObjectURL(url);
  }
}

function drawScaled(source: CanvasImageSource, width: number, height: number): HTMLCanvasElement {
  const canvas = document.createElement("canvas");
  canvas.width = Math.max(1, width);
  canvas.height = Math.max(1, height);
  const ctx = canvas.getContext("2d")!;
  ctx.imageSmoothingEnabled = true;
  ctx.imageSmoothingQuality = "high";
  ctx.drawImage(source, 0, 0, canvas.width, canvas.height);
  return canvas;
}

/** The photo turned and mirrored as the user asked. */
export function orient(source: HTMLCanvasElement, rotation: Rotation, flipX: boolean): HTMLCanvasElement {
  const sideways = rotation === 90 || rotation === 270;
  const canvas = document.createElement("canvas");
  canvas.width = sideways ? source.height : source.width;
  canvas.height = sideways ? source.width : source.height;
  const ctx = canvas.getContext("2d")!;
  ctx.translate(canvas.width / 2, canvas.height / 2);
  ctx.rotate((rotation * Math.PI) / 180);
  if (flipX) ctx.scale(-1, 1);
  ctx.drawImage(source, -source.width / 2, -source.height / 2);
  return canvas;
}

/** Width / height of the output frame. */
export function frameRatio(aspect: Aspect, width: number, height: number): number {
  if (aspect === "square") return 1;
  if (aspect === "portrait") return 4 / 5;
  return width / height;
}

export interface Rect {
  x: number;
  y: number;
  width: number;
  height: number;
}

/** The part of the (rotated) photo that ends up in the frame, in its pixels. Keeps the crop inside the photo. */
export function cropRect(width: number, height: number, state: EditState): Rect {
  const ratio = frameRatio(state.aspect, width, height);
  let w = width / height > ratio ? height * ratio : width;
  let h = w / ratio;
  w /= state.zoom;
  h /= state.zoom;
  const x = clamp(state.centerX * width - w / 2, 0, width - w);
  const y = clamp(state.centerY * height - h / 2, 0, height - h);
  return { x, y, width: w, height: h };
}

/** The crop centre after keeping the crop inside the photo (used after zooming out or rotating). */
export function clampCenter(width: number, height: number, state: EditState): { centerX: number; centerY: number } {
  const rect = cropRect(width, height, state);
  return { centerX: (rect.x + rect.width / 2) / width, centerY: (rect.y + rect.height / 2) / height };
}

/** Renders the final frame from the rotated photo: cropped (or fitted on white), resized, colour-adjusted. */
export function renderFrame(oriented: HTMLCanvasElement, state: EditState, maxSide: number): HTMLCanvasElement {
  const { width, height } = oriented;
  const ratio = frameRatio(state.aspect, width, height);
  const canvas = document.createElement("canvas");
  const ctx = canvas.getContext("2d")!;
  ctx.imageSmoothingEnabled = true;
  ctx.imageSmoothingQuality = "high";

  if (state.mode === "fit" && state.aspect !== "original") {
    // whole photo, centred on white, never enlarged
    const long = Math.min(maxSide, Math.max(width / Math.min(1, ratio), height * Math.max(1, ratio)));
    canvas.width = Math.round(ratio >= 1 ? long : long * ratio);
    canvas.height = Math.round(ratio >= 1 ? long / ratio : long);
    const scale = Math.min(canvas.width / width, canvas.height / height);
    const w = width * scale;
    const h = height * scale;
    ctx.fillStyle = "#ffffff";
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    ctx.drawImage(oriented, (canvas.width - w) / 2, (canvas.height - h) / 2, w, h);
  } else {
    const rect = cropRect(width, height, state);
    const scale = Math.min(1, maxSide / Math.max(rect.width, rect.height));
    canvas.width = Math.max(1, Math.round(rect.width * scale));
    canvas.height = Math.max(1, Math.round(rect.height * scale));
    ctx.fillStyle = "#ffffff"; // transparent PNGs become white, as JPEG needs
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    ctx.drawImage(oriented, rect.x, rect.y, rect.width, rect.height, 0, 0, canvas.width, canvas.height);
  }

  applyColour(canvas, state.adjustments, state.levels);
  return canvas;
}

/** A small, colour-adjusted copy of the whole rotated photo for the live crop view. */
export function previewOf(oriented: HTMLCanvasElement, adjustments: Adjustments, levels: Levels | null, maxSide = 900) {
  const scale = Math.min(1, maxSide / Math.max(oriented.width, oriented.height));
  const canvas = drawScaled(oriented, Math.round(oriented.width * scale), Math.round(oriented.height * scale));
  applyColour(canvas, adjustments, levels);
  return canvas;
}

export function exportJpeg(canvas: HTMLCanvasElement): Promise<Blob> {
  return new Promise((resolve, reject) =>
    canvas.toBlob((blob) => (blob ? resolve(blob) : reject(new Error("Export failed"))), "image/jpeg", EXPORT_QUALITY),
  );
}

export function isNeutral(adjustments: Adjustments, levels: Levels | null): boolean {
  return levels === null && Object.values(adjustments).every((v) => v === 0);
}

/**
 * Brightness / contrast / levels go through one lookup table; saturation and warmth are per pixel.
 * brightness ±100 → ×0.5..×1.5, contrast ±100 → ×0.5..×1.5 around mid-grey,
 * saturation ±100 → grey..double, warmth ±100 → ±12% red / ∓12% blue.
 */
export function applyColour(canvas: HTMLCanvasElement, a: Adjustments, levels: Levels | null): void {
  if (isNeutral(a, levels)) return;
  const ctx = canvas.getContext("2d", { willReadFrequently: true })!;
  const image = ctx.getImageData(0, 0, canvas.width, canvas.height);
  const px = image.data;

  const lut = new Uint8ClampedArray(256);
  const brightness = 1 + a.brightness / 200;
  const contrast = 1 + a.contrast / 200;
  const black = levels?.black ?? 0;
  const white = levels?.white ?? 255;
  const inverseGamma = 1 / (levels?.gamma ?? 1);
  for (let v = 0; v < 256; v++) {
    const stretched = clamp((v - black) / Math.max(1, white - black), 0, 1);
    let x = 255 * Math.pow(stretched, inverseGamma);
    x *= brightness;
    x = (x - 128) * contrast + 128;
    lut[v] = x; // Uint8ClampedArray rounds and clamps
  }

  const saturation = 1 + a.saturation / 100;
  const warm = a.warmth / 100;
  const red = 1 + 0.12 * warm;
  const blue = 1 - 0.12 * warm;
  const colour = saturation !== 1 || warm !== 0;

  for (let i = 0; i < px.length; i += 4) {
    let r = lut[px[i]];
    let g = lut[px[i + 1]];
    let b = lut[px[i + 2]];
    if (colour) {
      const grey = 0.2126 * r + 0.7152 * g + 0.0722 * b;
      r = (grey + (r - grey) * saturation) * red;
      g = grey + (g - grey) * saturation;
      b = (grey + (b - grey) * saturation) * blue;
    }
    px[i] = r;
    px[i + 1] = g;
    px[i + 2] = b;
  }
  ctx.putImageData(image, 0, 0);
}

/**
 * "Auto enhance": stretches the darkest/brightest 0.5% to full range (gently: shadows are never crushed and a
 * white-background product shot keeps its white), lifts the mid-tones of a dark photo with a gamma curve
 * (a bright photo is left as bright), and adds a touch of colour and contrast.
 */
export function autoEnhance(source: HTMLCanvasElement): { levels: Levels; adjustments: Adjustments } {
  const sample = drawScaled(source, Math.min(400, source.width), Math.round(Math.min(400, source.width) * (source.height / source.width)));
  const data = sample.getContext("2d", { willReadFrequently: true })!.getImageData(0, 0, sample.width, sample.height).data;
  const histogram = new Array<number>(256).fill(0);
  let total = 0;
  let sum = 0;
  for (let i = 0; i < data.length; i += 4) {
    const luma = Math.round(0.2126 * data[i] + 0.7152 * data[i + 1] + 0.0722 * data[i + 2]);
    histogram[luma]++;
    total++;
    sum += luma;
  }
  const black = Math.min(20, percentile(histogram, total, 0.005));
  const white = Math.max(215, percentile(histogram, total, 0.995));
  const mean = clamp((sum / total - black) / Math.max(1, white - black), 0.01, 0.99);
  // the gamma that would bring the average to 45% grey, only ever brightening, and within a natural range
  const gamma = mean < 0.45 ? clamp(Math.log(mean) / Math.log(0.45), 1, 1.8) : 1;
  return {
    levels: { black, white, gamma },
    adjustments: { brightness: 0, contrast: 8, saturation: 12, warmth: 0 },
  };
}

function percentile(histogram: number[], total: number, fraction: number): number {
  const target = total * fraction;
  let seen = 0;
  for (let v = 0; v < 256; v++) {
    seen += histogram[v];
    if (seen >= target) return v;
  }
  return 255;
}

export function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value));
}
