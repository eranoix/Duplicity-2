import { useSelector } from "react-redux";
import { editableDuplicantIdsSelector } from "../selectors/duplicants";

/**
 * Hook que retorna IDs de todos os duplicantes editáveis
 * (tanto humanos quanto bionics - ambos têm Accessorizer e MinionIdentity behaviors)
 */
export default function useDuplicants(): number[] {
  return useSelector(editableDuplicantIdsSelector);
}
