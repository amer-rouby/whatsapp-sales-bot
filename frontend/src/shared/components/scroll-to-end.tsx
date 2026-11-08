"use client";

import { useEffect, useRef } from "react";

/** Keeps a chat scrolled to the newest message whenever the number of messages changes. */
export function ScrollToEnd({ trigger }: { trigger: number }) {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    ref.current?.scrollIntoView({ block: "end" });
  }, [trigger]);
  return <div ref={ref} />;
}
