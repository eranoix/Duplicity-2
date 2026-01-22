import { reduceReducers } from "@/store/utils";
import { OniSaveState, defaultOniSaveState, LoadingStatus } from "../state";
import { AnyAction } from "redux";

import changeGeyserParameter from "./change-geyser-parameter";
import changeGeyserTypeReducer from "./change-geyser-type";
import cloneDuplicantReducer from "./clone-duplicant";
import copyBehaviorsReducer from "./copy-behaviors";
import deleteLooseMaterial from "./delete-looe-material";
import importWarnChecksumReducer from "./import-warn-checksum";
import loadExampleSaveReducer from "./load-example";
import loadOniSaveReducer from "./load-onisave";
import mergeBehaviorsReducer from "./merge-behaviors";
import modifyBehaviorPathReducer from "./modify-behavior-path";
import modifyBehaviorReducer from "./modify-behavior";
import modifyDifficultyReducer from "./modify-difficulty";
import modifyMaterialQuantityReducer from "./modify-material-quantity";
import modifyPlanetReducer from "./modify-planet";
import modifyRawReducer from "./modify-raw";
import parseProgressReducer from "./parse-progress";
import pasteBehaviorsReducer from "./paste-behaviors";
import receiveOniSaveReducer from "./receive-onisave";

const combinedReducer = reduceReducers(
  changeGeyserParameter,
  changeGeyserTypeReducer,
  cloneDuplicantReducer,
  copyBehaviorsReducer,
  deleteLooseMaterial,
  importWarnChecksumReducer,
  loadExampleSaveReducer,
  loadOniSaveReducer,
  mergeBehaviorsReducer,
  modifyBehaviorPathReducer,
  modifyBehaviorReducer,
  modifyDifficultyReducer,
  modifyMaterialQuantityReducer,
  modifyPlanetReducer,
  modifyRawReducer,
  parseProgressReducer,
  pasteBehaviorsReducer,
  receiveOniSaveReducer
);

export default function oniSaveReducer(
  state: OniSaveState | undefined = defaultOniSaveState,
  action: AnyAction
): OniSaveState {
  const result = combinedReducer(state, action);
  // Safety check: ensure loadingStatus is never Loading or Saving when there's no active operation
  if ((result.loadingStatus === LoadingStatus.Loading || result.loadingStatus === LoadingStatus.Saving) && 
      !result.loadingFile && 
      !result.saveGame && 
      action.type !== "oni-save/receive:begin") {
    return {
      ...result,
      loadingStatus: LoadingStatus.Idle
    };
  }
  return result;
}
