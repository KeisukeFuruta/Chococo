import { useState } from "react";
import styles from "./App.module.css";
import { AppHeader } from "./components/AppHeader";
import { TabBar } from "./components/TabBar";
import { INITIAL_RECORDS } from "./mockData";
import { LoginScreen } from "./screens/LoginScreen";
import { PairingScreen } from "./screens/PairingScreen";
import { RecordDetailScreen } from "./screens/RecordDetailScreen";
import { RecordFormScreen, type RecordFormValues } from "./screens/RecordFormScreen";
import { RecordsCalendarScreen } from "./screens/RecordsCalendarScreen";
import { SignupScreen } from "./screens/SignupScreen";
import type { AppScreen, PairingSuggestionResult, SweetsRecord } from "./types";
import { todayDateKey } from "./utils/date";

const DAILY_LIMIT = 10;

export default function App() {
  const [authenticated, setAuthenticated] = useState(false);
  const [screen, setScreen] = useState<AppScreen>({ kind: "login" });
  const [records, setRecords] = useState<SweetsRecord[]>(INITIAL_RECORDS);
  const [usageRemaining, setUsageRemaining] = useState(DAILY_LIMIT);
  const [lastSuggestion, setLastSuggestion] = useState<PairingSuggestionResult | null>(null);

  function handleAuthenticated() {
    setAuthenticated(true);
    setScreen({ kind: "main", tab: "pairing" });
  }

  function handleLogout() {
    setAuthenticated(false);
    setLastSuggestion(null);
    setScreen({ kind: "login" });
  }

  function handleUsageConsumed() {
    setUsageRemaining((remaining) => Math.max(0, remaining - 1));
  }

  function handleSuggestionGenerated(suggestion: PairingSuggestionResult) {
    setLastSuggestion(suggestion);
    handleUsageConsumed();
  }

  function handleCreateRecord(values: RecordFormValues, suggestion?: PairingSuggestionResult) {
    const newRecord: SweetsRecord = {
      id: `r${Date.now()}`,
      sweetsName: values.sweetsName,
      date: values.date,
      memo: values.memo || undefined,
      photoDataUrl: values.photoDataUrl,
      pairing: suggestion,
    };
    setRecords((prev) => [...prev, newRecord]);
    setScreen({ kind: "recordDetail", recordId: newRecord.id });
  }

  function handleUpdateRecord(recordId: string, values: RecordFormValues) {
    setRecords((prev) =>
      prev.map((r) =>
        r.id === recordId
          ? { ...r, sweetsName: values.sweetsName, date: values.date, memo: values.memo || undefined, photoDataUrl: values.photoDataUrl }
          : r,
      ),
    );
    setScreen({ kind: "recordDetail", recordId });
  }

  function handleDeleteRecord(recordId: string) {
    setRecords((prev) => prev.filter((r) => r.id !== recordId));
    setScreen({ kind: "main", tab: "records" });
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
                suggestion={lastSuggestion}
                onSuggestionGenerated={handleSuggestionGenerated}
                onSaveAsRecord={(suggestion) =>
                  setScreen({ kind: "recordCreate", origin: "pairing", suggestion })
                }
              />
            ) : (
              <RecordsCalendarScreen
                records={records}
                onSelectRecord={(recordId) => setScreen({ kind: "recordDetail", recordId })}
              />
            )}
            <TabBar activeTab={screen.tab} onChange={(tab) => setScreen({ kind: "main", tab })} />
          </>
        );

      case "recordCreate":
        return (
          <RecordFormScreen
            title="記録を作成"
            initialValues={{
              sweetsName: screen.suggestion?.sweetsName ?? "",
              date: todayDateKey(),
              memo: "",
            }}
            initialSuggestion={screen.suggestion}
            usageRemaining={usageRemaining}
            onGenerateSuggestion={handleUsageConsumed}
            onBack={() => setScreen({ kind: "main", tab: screen.origin })}
            onSubmit={(values, suggestion) => handleCreateRecord(values, suggestion)}
          />
        );

      case "recordDetail": {
        const record = records.find((r) => r.id === screen.recordId);
        if (!record) {
          setScreen({ kind: "main", tab: "records" });
          return null;
        }
        return (
          <RecordDetailScreen
            record={record}
            onBack={() => setScreen({ kind: "main", tab: "records" })}
            onEdit={() => setScreen({ kind: "recordEdit", recordId: record.id })}
            onDelete={() => handleDeleteRecord(record.id)}
          />
        );
      }

      case "recordEdit": {
        const record = records.find((r) => r.id === screen.recordId);
        if (!record) {
          setScreen({ kind: "main", tab: "records" });
          return null;
        }
        return (
          <RecordFormScreen
            title="記録を編集"
            initialValues={{
              sweetsName: record.sweetsName,
              date: record.date,
              memo: record.memo ?? "",
              photoDataUrl: record.photoDataUrl,
            }}
            initialSuggestion={record.pairing}
            onBack={() => setScreen({ kind: "recordDetail", recordId: record.id })}
            onSubmit={(values) => handleUpdateRecord(record.id, values)}
          />
        );
      }

      default:
        return null;
    }
  }
}
