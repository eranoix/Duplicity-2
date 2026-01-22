import { combineReducers, AnyAction } from "redux";

import { connectRouter } from "connected-react-router";

import { AppState, defaultAppState } from "@/state";

import i18nReducer from "@/services/i18n/reducer";
import oniSaveReducer from "@/services/oni-save/reducer";
import offlineModeReducer from "@/services/offline-mode/reducer";
import { OniSaveState, defaultOniSaveState } from "@/services/oni-save/state";

import history from "@/history";

const routerReducer = connectRouter(history);

// Wrapper to ensure oniSave always starts with default state
const oniSaveReducerWithDefault = (
  state: OniSaveState | undefined,
  action: AnyAction
) => {
  if (state === undefined) {
    return defaultOniSaveState;
  }
  return oniSaveReducer(state, action);
};

const servicesReducer = combineReducers({
  i18n: i18nReducer,
  oniSave: oniSaveReducerWithDefault,
  offlineMode: offlineModeReducer
});

export default function reducer(
  state: AppState = defaultAppState,
  action: AnyAction
): AppState {
  return {
    router: routerReducer(state.router, action as any),
    services: servicesReducer(state.services, action)
  };
}
