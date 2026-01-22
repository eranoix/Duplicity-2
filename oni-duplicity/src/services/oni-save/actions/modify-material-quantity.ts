import { AnyAction } from "redux";
import { SimHashName } from "oni-save-parser";

export const ACTION_ONISAVE_MODIFY_MATERIAL_QUANTITY =
  "oni-save/modify-material-quantity";

export const modifyMaterialQuantity = (
  materialType: SimHashName,
  isLoose: boolean,
  newTotalGrams: number
) => ({
  type: ACTION_ONISAVE_MODIFY_MATERIAL_QUANTITY as typeof ACTION_ONISAVE_MODIFY_MATERIAL_QUANTITY,
  payload: { materialType, isLoose, newTotalGrams }
});

export type ModifyMaterialQuantityAction = ReturnType<
  typeof modifyMaterialQuantity
>;

export function isModifyMaterialQuantityAction(
  action: AnyAction
): action is ModifyMaterialQuantityAction {
  return action.type === ACTION_ONISAVE_MODIFY_MATERIAL_QUANTITY;
}
