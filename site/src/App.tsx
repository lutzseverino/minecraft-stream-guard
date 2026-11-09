import { AppTooltipProvider } from "@/components/app/app-tooltip";
import { StreamWallPage } from "@/pages/stream-wall/stream-wall-page";
import { ThemeProvider } from "@/theme/ThemeProvider";

export default function App() {
  return (
    <ThemeProvider>
      <AppTooltipProvider>
        <StreamWallPage />
      </AppTooltipProvider>
    </ThemeProvider>
  );
}
