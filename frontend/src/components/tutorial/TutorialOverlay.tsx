import { useEffect, useLayoutEffect, useRef, useState, type CSSProperties } from "react";
import type { MainTab } from "../../types";
import { useTutorialTargets } from "./useTutorialTargets";
import styles from "./TutorialOverlay.module.css";

interface TutorialStepConfig {
  // nullの場合はスポットライトなし（中央カードのみ）
  targetKey: string | null;
  requiredTab: MainTab;
  title: string;
  description: string;
}

const STEPS: TutorialStepConfig[] = [
  {
    targetKey: null,
    requiredTab: "pairing",
    title: "Chococoへようこそ🍫☕",
    description: "食べたいスイーツに合うコーヒーを、AIが理由つきで提案します。かんたんに使い方をご案内します。",
  },
  {
    targetKey: "tab-pairing",
    requiredTab: "pairing",
    title: "☕ 提案タブ",
    description: "スイーツ名を入力すると、AIが最適なコーヒー豆を選んで提案してくれます。",
  },
  {
    targetKey: "sweet-name-input",
    requiredTab: "pairing",
    title: "スイーツ名を入力",
    description: "食べたいスイーツの名前を入力して「AIに提案してもらう」を押すと提案が始まります。",
  },
  {
    targetKey: "tab-records",
    requiredTab: "pairing",
    title: "📅 記録タブ",
    description: "保存した記録はここから振り返ることができます。次に見てみましょう。",
  },
  {
    targetKey: null,
    requiredTab: "records",
    title: "記録カレンダー",
    description: "食べたスイーツの記録がカレンダー形式で並びます。日付をタップすると詳細を確認できます。",
  },
  {
    targetKey: "add-record-button",
    requiredTab: "records",
    title: "＋ 記録を作成",
    description: "AIの提案を使わず、直接記録を作成することもできます。",
  },
];

const SPOTLIGHT_PADDING = 8;
const CARD_WIDTH = 280;
const CARD_MARGIN = 16;
// カード実測前（初回描画）だけに使うフォールバック値。実際の高さはcardRefで計測してcardHeightに反映する
const FALLBACK_CARD_HEIGHT = 170;

interface TutorialOverlayProps {
  activeTab: MainTab;
  onNavigateTab: (tab: MainTab) => void;
  onFinish: () => void;
}

export function TutorialOverlay({ activeTab, onNavigateTab, onFinish }: TutorialOverlayProps) {
  const [stepIndex, setStepIndex] = useState(0);
  const [rect, setRect] = useState<DOMRect | null>(null);
  const [cardHeight, setCardHeight] = useState(FALLBACK_CARD_HEIGHT);
  const cardRef = useRef<HTMLDivElement>(null);
  const { getTarget } = useTutorialTargets();
  const step = STEPS[stepIndex];
  const isLastStep = stepIndex === STEPS.length - 1;

  // ステップが求めるタブと現在のタブが異なる場合、自動でタブを切り替える
  useEffect(() => {
    if (step.requiredTab !== activeTab) {
      onNavigateTab(step.requiredTab);
    }
  }, [step.requiredTab, activeTab, onNavigateTab]);

  // タブの切り替えが反映された後（activeTabがrequiredTabと一致した後）に対象要素を計測する
  useLayoutEffect(() => {
    if (step.requiredTab !== activeTab) {
      setRect(null);
      return;
    }
    function measure() {
      if (!step.targetKey) {
        setRect(null);
        return;
      }
      const node = getTarget(step.targetKey);
      setRect(node ? node.getBoundingClientRect() : null);
    }
    measure();
    window.addEventListener("resize", measure);
    window.addEventListener("scroll", measure, true);
    return () => {
      window.removeEventListener("resize", measure);
      window.removeEventListener("scroll", measure, true);
    };
  }, [step, activeTab, getTarget]);

  // カードの実際の高さを計測し、配置計算に反映する（説明文の長さで高さが変わるため、固定値では見積もりがずれてスポットライト対象と重なることがある）
  useLayoutEffect(() => {
    function measureCardHeight() {
      if (cardRef.current) {
        setCardHeight(cardRef.current.getBoundingClientRect().height);
      }
    }
    measureCardHeight();
    window.addEventListener("resize", measureCardHeight);
    return () => window.removeEventListener("resize", measureCardHeight);
  }, [step]);

  function handleNext() {
    if (isLastStep) {
      onFinish();
    } else {
      setStepIndex((i) => i + 1);
    }
  }

  const cutoutStyle = computeCutoutStyle(rect);
  const cardStyle = computeCardStyle(rect, cardHeight);

  return (
    <div className={styles.root} role="dialog" aria-modal="true" aria-label="使い方ガイド">
      <div className={styles.clickBlocker} />
      <div className={styles.cutout} style={cutoutStyle} />
      <div ref={cardRef} className={styles.card} style={cardStyle}>
        <div className={styles.stepCount}>
          {stepIndex + 1} / {STEPS.length}
        </div>
        <div className={styles.title}>{step.title}</div>
        <div className={styles.description}>{step.description}</div>
        <div className={styles.actions}>
          <button type="button" className={styles.skip} onClick={onFinish}>
            スキップ
          </button>
          <button type="button" className={styles.next} onClick={handleNext}>
            {isLastStep ? "はじめる" : "次へ"}
          </button>
        </div>
      </div>
    </div>
  );
}

function computeCutoutStyle(rect: DOMRect | null): CSSProperties {
  if (!rect) {
    // 対象なしのステップは、0サイズの矩形をbox-shadowで全画面に広げることで「穴のない」暗転にする
    return {
      top: window.innerHeight / 2,
      left: window.innerWidth / 2,
      width: 0,
      height: 0,
    };
  }
  return {
    top: rect.top - SPOTLIGHT_PADDING,
    left: rect.left - SPOTLIGHT_PADDING,
    width: rect.width + SPOTLIGHT_PADDING * 2,
    height: rect.height + SPOTLIGHT_PADDING * 2,
  };
}

function computeCardStyle(rect: DOMRect | null, cardHeight: number): CSSProperties {
  if (!rect) {
    return { top: "50%", left: "50%", transform: "translate(-50%, -50%)" };
  }

  const viewportWidth = window.innerWidth;
  const viewportHeight = window.innerHeight;

  let top: number;
  if (rect.bottom + cardHeight + CARD_MARGIN <= viewportHeight) {
    top = rect.bottom + CARD_MARGIN;
  } else if (rect.top - cardHeight - CARD_MARGIN >= 0) {
    top = rect.top - cardHeight - CARD_MARGIN;
  } else {
    // どちらにも収まらない場合は、対象と重ならない範囲でできるだけ収める（画面の空きが最大の側に寄せる）
    const spaceBelow = viewportHeight - rect.bottom;
    const spaceAbove = rect.top;
    top =
      spaceBelow >= spaceAbove
        ? Math.min(rect.bottom + CARD_MARGIN, viewportHeight - cardHeight - CARD_MARGIN)
        : Math.max(CARD_MARGIN, rect.top - cardHeight - CARD_MARGIN);
    top = Math.max(CARD_MARGIN, top);
  }

  const left = Math.min(
    Math.max(rect.left, CARD_MARGIN),
    viewportWidth - CARD_WIDTH - CARD_MARGIN,
  );

  return { top, left };
}
