import { useCallback, useContext } from "react";
import { TutorialTargetsContext, type TutorialTargetsApi } from "./tutorialTargetsRegistry";

function useTutorialTargetsApi(): TutorialTargetsApi {
  const ctx = useContext(TutorialTargetsContext);
  if (!ctx) throw new Error("TutorialTargetsProvider の外で使用されています");
  return ctx;
}

// 対象コンポーネントの要素に `ref={useTutorialTargetRef("tab-pairing")}` のように渡す
export function useTutorialTargetRef(key: string) {
  const { registerTarget } = useTutorialTargetsApi();
  return useCallback((node: HTMLElement | null) => registerTarget(key, node), [registerTarget, key]);
}

export function useTutorialTargets() {
  return useTutorialTargetsApi();
}
