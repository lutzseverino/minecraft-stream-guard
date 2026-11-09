import {
  createContext,
  type ReactNode,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";

export const themeModes = ["light", "dark", "system"] as const;
export type ThemeMode = (typeof themeModes)[number];

type ThemeProviderState = {
  theme: ThemeMode;
  setTheme: (theme: ThemeMode) => void;
};

/* The wall is a cinematic surface, so it boots dark and only changes on intent. */
const defaultTheme: ThemeMode = "dark";
const themeStorageKey = "streamguard-theme";
const ThemeProviderContext = createContext<ThemeProviderState | null>(null);

function isThemeMode(value: string | null | undefined): value is ThemeMode {
  return themeModes.some((mode) => mode === value);
}

function getStoredTheme() {
  if (typeof window === "undefined") {
    return defaultTheme;
  }

  const storedTheme = window.localStorage.getItem(themeStorageKey);
  return isThemeMode(storedTheme) ? storedTheme : defaultTheme;
}

/* Keep in step with the boot script in index.html, which applies the saved
   theme before first paint. Only `.dark` switches themes; light is `:root`. */
function applyTheme(theme: ThemeMode) {
  const dark =
    theme === "system"
      ? window.matchMedia("(prefers-color-scheme: dark)").matches
      : theme === "dark";

  window.document.documentElement.classList.toggle("dark", dark);
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setThemeState] = useState<ThemeMode>(getStoredTheme);

  useEffect(() => {
    applyTheme(theme);
    window.localStorage.setItem(themeStorageKey, theme);
  }, [theme]);

  useEffect(() => {
    if (theme !== "system") {
      return;
    }

    const media = window.matchMedia("(prefers-color-scheme: dark)");
    const updateSystemTheme = () => applyTheme("system");

    media.addEventListener("change", updateSystemTheme);
    return () => media.removeEventListener("change", updateSystemTheme);
  }, [theme]);

  const value = useMemo(
    () => ({
      theme,
      setTheme: setThemeState,
    }),
    [theme],
  );

  return (
    <ThemeProviderContext.Provider value={value}>
      {children}
    </ThemeProviderContext.Provider>
  );
}

export function useTheme() {
  const context = useContext(ThemeProviderContext);

  if (!context) {
    throw new Error("useTheme must be used within a ThemeProvider.");
  }

  return context;
}
