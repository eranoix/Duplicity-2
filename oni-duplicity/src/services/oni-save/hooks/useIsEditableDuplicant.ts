import { useSelector } from "react-redux";
import { AppState } from "@/state";
import { isEditableDuplicantSelector } from "../selectors/duplicants";

/**
 * Hook que verifica se um gameObjectId é um duplicante editável
 * (tem Accessorizer e MinionIdentity behaviors)
 */
export default function useIsEditableDuplicant(gameObjectId: number): boolean {
  return useSelector((state: AppState) => isEditableDuplicantSelector(state, gameObjectId));
}
