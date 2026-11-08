"use client";

/**
 * Short notification chimes synthesized with the Web Audio API: no audio files to ship or license.
 * Browsers only allow sound after the user has interacted with the page, so call `unlockSounds()`
 * from the first click/keypress; until then `playChime` stays silent instead of throwing.
 */

export type Chime = "message" | "order" | "attention";

/** Notes as [frequency Hz, start offset s, duration s]. */
const MELODIES: Record<Chime, Array<[number, number, number]>> = {
  // two soft notes, like a chat ping
  message: [
    [880, 0, 0.16],
    [1318.5, 0.11, 0.24],
  ],
  // a bright rising arpeggio: money came in
  order: [
    [784, 0, 0.14],
    [987.8, 0.1, 0.14],
    [1174.7, 0.2, 0.14],
    [1568, 0.3, 0.38],
  ],
  // insistent double knock: a person has to act
  attention: [
    [659.3, 0, 0.12],
    [659.3, 0.16, 0.12],
    [987.8, 0.34, 0.3],
  ],
};

let context: AudioContext | null = null;

function audioContext(): AudioContext | null {
  if (typeof window === "undefined" || !("AudioContext" in window)) return null;
  context ??= new AudioContext();
  return context;
}

/** Call from a user gesture; afterwards chimes can play even while the user is elsewhere in the app. */
export async function unlockSounds() {
  const ctx = audioContext();
  if (ctx?.state === "suspended") {
    await ctx.resume().catch(() => undefined);
  }
}

export function playChime(kind: Chime, volume = 0.18) {
  const ctx = audioContext();
  if (!ctx || ctx.state !== "running") return;

  const master = ctx.createGain();
  master.gain.value = volume;
  master.connect(ctx.destination);

  const start = ctx.currentTime + 0.02;
  for (const [frequency, offset, duration] of MELODIES[kind]) {
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = "sine";
    osc.frequency.value = frequency;
    // quick attack, smooth exponential release: a bell, not a beep
    gain.gain.setValueAtTime(0.0001, start + offset);
    gain.gain.exponentialRampToValueAtTime(1, start + offset + 0.015);
    gain.gain.exponentialRampToValueAtTime(0.0001, start + offset + duration);
    osc.connect(gain).connect(master);
    osc.start(start + offset);
    osc.stop(start + offset + duration + 0.05);
  }
}
