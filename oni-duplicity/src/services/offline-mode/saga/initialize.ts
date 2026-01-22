import { takeEvery, call, put } from "redux-saga/effects";

import { ACTION_INITIALIZE } from "@/actions/initialize";
import { offlineProbeCompleted } from "../actions/offline-probe-completed";
import { offlineSwitchCompleted } from "../actions/offline-switch-completed";

export default function* initializeSaga() {
  yield takeEvery(ACTION_INITIALIZE, handleInitialize);
}

async function enableOfflineMode() {
  if (!("serviceWorker" in navigator)) {
    return false;
  }
  try {
    // Unregister any existing service worker first
    const existingRegistration = await navigator.serviceWorker.getRegistration();
    if (existingRegistration) {
      await existingRegistration.unregister();
    }
    // Register the service worker for offline mode
    await navigator.serviceWorker.register("./service-worker.js");
    return true;
  } catch (e) {
    console.error("Failed to register service worker:", e);
    return false;
  }
}

function* handleInitialize() {
  if ("serviceWorker" in navigator) {
    // Automatically enable offline mode by registering the service worker
    const success: boolean = yield call(enableOfflineMode);
    yield put(offlineProbeCompleted(true, success));
    if (success) {
      yield put(offlineSwitchCompleted(true));
    }
  } else {
    yield put(offlineProbeCompleted(false, false));
  }
}
