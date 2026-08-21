import { useEffect, useState } from "react";
import { logout as apiLogout } from "./api/auth";
import { AUTH_EXPIRED_EVENT } from "./api/client";
import { getPairingUsage } from "./api/pairings";
import { tokenStorage } from "./api/tokenStorage";
import styles from "./App.module.css";
import { AppHeader } from "./components/AppHeader";
import { TabBar } from "./components/TabBar";
import { LoginScreen } from "./screens/LoginScreen";
import { PairingScreen } from "./screens/PairingScreen";
import { RecordDetailScreen } from "./screens/RecordDetailScreen";
import { RecordFormScreen } from "./screens/RecordFormScreen";
import { RecordsCalendarScreen } from "./screens/RecordsCalendarScreen";
import { SignupScreen } from "./screens/SignupScreen";
import type { AppScreen, PairingSuggestion } from "./types";

const DEFAULT_USAGE_LIMIT = 10;

export default function App() {
  const [authenticated, setAuthenticated] = useState(() => tokenStorage.getRefreshToken() !== null);
  const [screen, setScreen] = useState<AppScreen>(() =>
    tokenStorage.getRefreshToken() ? { kind: "main", tab: "pairing" } : { kind: "login" },
  );
  const [usageRemaining, setUsageRemaining] = useState<number | null>(null);
  const [usageLimit, setUsageLimit] = useState(DEFAULT_USAGE_LIMIT);
  const [lastSuggestion, setLastSuggestion] = useState<PairingSuggestion | null>(null);

  useEffect(() => {
    function handleAuthExpired() {
      setAuthenticated(false);
      setLastSuggestion(null);
      setScreen({ kind: "login" });
    }
    window.addEventListener(AUTH_EXPIRED_EVENT, handleAuthExpired);
    return () => window.removeEventListener(AUTH_EXPIRED_EVENT, handleAuthExpired);
  }, []);

  // api-spec.md 3.3節：S3（ペアリング提案画面）とS5の入り口②（記録作成のAI提案エリア）表示時に利用状況を取得する
  useEffect(() => {
    if (!authenticated) return;
    const needsUsage = (screen.kind === "main" && screen.tab === "pairing") || screen.kind === "recordCreate";
    if (!needsUsage) return;

    let cancelled = false;
    getPairingUsage()
      .then((usage) => {
        if (cancelled) return;
        setUsageRemaining(usage.remainingCount);
        setUsageLimit(usage.limit);
      })
      .catch(() => {
        // 取得失敗時は「確認中」表示のまま。AI提案リクエスト自体のエラーは各画面側で表示する
      });
    return () => {
      cancelled = true;
    };
  }, [authenticated, screen]);

  function handleAuthenticated() {
    setAuthenticated(true);
    setScreen({ kind: "main", tab: "pairing" });
  }

  async function handleLogout() {
    await apiLogout();
    setAuthenticated(false);
    setLastSuggestion(null);
    setUsageRemaining(null);
    setScreen({ kind: "login" });
  }

  function handleSuggestionGenerated(suggestion: PairingSuggestion) {
    setLastSuggestion(suggestion);
    setUsageRemaining(suggestion.remainingCount);
  }

  return (
    <div className={styles.shell}>
      <div className={styles.body}>{renderScreen()}</div>
    </div>
  );

  function renderScreen() {
    if (!authenticated || screen.kind === "login" || screen.kind === "signup") {
      if (screen.kind === "signup") {
        return (
          <SignupScreen onSignup={handleAuthenticated} onBack={() => setScreen({ kind: "login" })} />
        );
      }
      return (
        <LoginScreen onLogin={handleAuthenticated} onNavigateSignup={() => setScreen({ kind: "signup" })} />
      );
    }

    switch (screen.kind) {
      case "main":
        return (
          <>
            <AppHeader
              title="Chococo"
              onLogoClick={() => setScreen({ kind: "main", tab: "pairing" })}
              onAdd={
                screen.tab === "records"
                  ? () => setScreen({ kind: "recordCreate", origin: "records" })
                  : undefined
              }
              onLogout={handleLogout}
            />
            {screen.tab === "pairing" ? (
              <PairingScreen
                usageRemaining={usageRemaining}
                usageLimit={usageLimit}
                suggestion={lastSuggestion}
                onSuggestionGenerated={handleSuggestionGenerated}
                onSaveAsRecord={(suggestion) =>
                  setScreen({ kind: "recordCreate", origin: "pairing", suggestion })
                }
              />
            ) : (
              <RecordsCalendarScreen
                onSelectRecord={(recordId) => setScreen({ kind: "recordDetail", recordId })}
                onCreateForDate={(dateKey) =>
                  setScreen({ kind: "recordCreate", origin: "records", presetDate: dateKey })
                }
              />
            )}
            <TabBar activeTab={screen.tab} onChange={(tab) => setScreen({ kind: "main", tab })} />
          </>
        );

      case "recordCreate":
        return (
          <RecordFormScreen
            mode="create"
            presetDate={screen.presetDate}
            initialSuggestion={screen.suggestion}
            usageRemaining={usageRemaining}
            usageLimit={usageLimit}
            onSuggestionGenerated={handleSuggestionGenerated}
            onBack={() => setScreen({ kind: "main", tab: screen.origin })}
            onSaved={(record) => setScreen({ kind: "recordDetail", recordId: record.id })}
          />
        );

      case "recordDetail":
        return (
          <RecordDetailScreen
            recordId={screen.recordId}
            onBack={() => setScreen({ kind: "main", tab: "records" })}
            onEdit={() => setScreen({ kind: "recordEdit", recordId: screen.recordId })}
            onDeleted={() => setScreen({ kind: "main", tab: "records" })}
          />
        );

      case "recordEdit":
        return (
          <RecordFormScreen
            mode="edit"
            recordId={screen.recordId}
            onBack={() => setScreen({ kind: "recordDetail", recordId: screen.recordId })}
            onSaved={(record) => setScreen({ kind: "recordDetail", recordId: record.id })}
          />
        );

      default:
        return null;
    }
  }
}
