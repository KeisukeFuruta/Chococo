import { useCallback, useRef, type ReactNode } from "react";
import { TutorialTargetsContext } from "./tutorialTargetsRegistry";

// タブボタン・「＋」ボタン等、各コンポーネントが自身のDOM要素を key で登録する。
// TutorialOverlayはこのkeyを頼りにgetBoundingClientRect()で位置を計測する（複数コンポーネントをまたぐスポットライトのため、
// props経由でrefを親から渡すのではなくcontext経由の登録方式にしている）
export function TutorialTargetsProvider({ children }: { children: ReactNode }) {
  const targetsRef = useRef(new Map<string, HTMLElement>());

  const registerTarget = useCallback((key: string, node: HTMLElement | null) => {
    if (node) {
      targetsRef.current.set(key, node);
    } else {
      targetsRef.current.delete(key);
    }
  }, []);

  const getTarget = useCallback((key: string) => targetsRef.current.get(key) ?? null, []);

  return (
    <TutorialTargetsContext.Provider value={{ registerTarget, getTarget }}>
      {children}
    </TutorialTargetsContext.Provider>
  );
}
