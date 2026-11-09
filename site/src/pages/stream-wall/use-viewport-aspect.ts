import { useEffect, useState } from "react";

/** Tracks the viewport's width / height ratio, used to drive the layout math. */
export function useViewportAspect(): number {
  const [aspect, setAspect] = useState<number>(() =>
    typeof window === "undefined"
      ? 16 / 9
      : window.innerWidth / window.innerHeight,
  );

  useEffect(() => {
    const update = () => setAspect(window.innerWidth / window.innerHeight);
    update();
    window.addEventListener("resize", update);
    window.addEventListener("orientationchange", update);
    return () => {
      window.removeEventListener("resize", update);
      window.removeEventListener("orientationchange", update);
    };
  }, []);

  return aspect;
}
