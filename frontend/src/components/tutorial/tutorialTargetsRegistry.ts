import { createContext } from "react";

export interface TutorialTargetsApi {
  registerTarget: (key: string, node: HTMLElement | null) => void;
  getTarget: (key: string) => HTMLElement | null;
}

export const TutorialTargetsContext = createContext<TutorialTargetsApi | null>(null);
