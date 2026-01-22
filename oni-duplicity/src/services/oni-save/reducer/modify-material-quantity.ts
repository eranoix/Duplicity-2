import { AnyAction } from "redux";
import {
  SaveGame,
  GameObjectGroup,
  getBehavior,
  PrimaryElementBehavior,
  StorageBehavior,
  getGameObjectId,
  SimHashName,
  KPrefabIDBehavior
} from "oni-save-parser";
import { findIndex, replace } from "lodash";

import { OniSaveState, defaultOniSaveState } from "../state";
import { isModifyMaterialQuantityAction } from "../actions/modify-material-quantity";

import {
  tryModifySaveGame,
  requireGameObject,
  changeStateBehaviorData,
  replaceGameObject
} from "./utils";

export default function modifyMaterialQuantityReducer(
  state: OniSaveState = defaultOniSaveState,
  action: AnyAction
): OniSaveState {
  if (!isModifyMaterialQuantityAction(action)) {
    return state;
  }

  const { materialType, isLoose, newTotalGrams } = action.payload;

  return tryModifySaveGame(state, saveGame =>
    performModifyMaterialQuantity(saveGame, materialType, isLoose, newTotalGrams)
  );
}

function performModifyMaterialQuantity(
  saveGame: SaveGame,
  materialType: SimHashName,
  isLoose: boolean,
  newTotalGrams: number
): SaveGame {
  if (isLoose) {
    return modifyLooseMaterialQuantity(saveGame, materialType, newTotalGrams);
  } else {
    return modifyStoredMaterialQuantity(saveGame, materialType, newTotalGrams);
  }
}

function modifyLooseMaterialQuantity(
  saveGame: SaveGame,
  materialType: SimHashName,
  newTotalGrams: number
): SaveGame {
  // Encontrar o grupo do material
  const materialGroupIndex = findIndex(
    saveGame.gameObjects,
    group => group.name === materialType
  );

  if (materialGroupIndex === -1) {
    return saveGame;
  }

  const materialGroup = saveGame.gameObjects[materialGroupIndex];
  
  // Calcular total atual
  let currentTotalGrams = 0;
  for (const gameObject of materialGroup.gameObjects) {
    const elementBehavior = getBehavior(gameObject, PrimaryElementBehavior);
    if (elementBehavior) {
      currentTotalGrams += elementBehavior.templateData.Units || 0;
    }
  }

  if (currentTotalGrams === 0) {
    return saveGame;
  }

  // Calcular fator de proporção
  const ratio = newTotalGrams / currentTotalGrams;

  // Modificar cada objeto
  const newGameObjects = materialGroup.gameObjects.map(gameObject => {
    const elementBehavior = getBehavior(gameObject, PrimaryElementBehavior);
    if (!elementBehavior) {
      return gameObject;
    }

    const currentUnits = elementBehavior.templateData.Units || 0;
    const newUnits = currentUnits * ratio;

    return changeStateBehaviorData(
      gameObject,
      PrimaryElementBehavior,
      "templateData",
      {
        Units: newUnits
      }
    );
  });

  // Substituir o grupo
  return {
    ...saveGame,
    gameObjects: replace(saveGame.gameObjects, materialGroupIndex, {
      ...materialGroup,
      gameObjects: newGameObjects
    })
  };
}

function modifyStoredMaterialQuantity(
  saveGame: SaveGame,
  materialType: SimHashName,
  newTotalGrams: number
): SaveGame {
  // Encontrar todos os objetos de storage e seus itens do material
  let currentTotalGrams = 0;
  const itemsToModify: Array<{
    gameObjectId: number;
    storageItemIndex: number;
    currentUnits: number;
  }> = [];

  for (const group of saveGame.gameObjects) {
    for (const gameObject of group.gameObjects) {
      const storageBehavior = getBehavior(gameObject, StorageBehavior);
      
      if (!storageBehavior) {
        continue;
      }

      const gameObjectId = getGameObjectId(gameObject);
      if (gameObjectId === null) {
        continue;
      }

      for (let itemIndex = 0; itemIndex < storageBehavior.extraData.length; itemIndex++) {
        const storageItem = storageBehavior.extraData[itemIndex];
        
        // Verificar se é o material desejado
        if (storageItem.name !== materialType) {
          continue;
        }

        const elementBehavior = getBehavior(storageItem, PrimaryElementBehavior);
        if (!elementBehavior) {
          continue;
        }

        const currentUnits = elementBehavior.templateData.Units || 0;
        currentTotalGrams += currentUnits;

        itemsToModify.push({
          gameObjectId,
          storageItemIndex: itemIndex,
          currentUnits
        });
      }
    }
  }

  if (currentTotalGrams === 0 || itemsToModify.length === 0) {
    return saveGame;
  }

  // Calcular fator de proporção
  const ratio = newTotalGrams / currentTotalGrams;

  // Modificar cada item de storage
  let newSaveGame = saveGame;
  for (const item of itemsToModify) {
    const gameObject = requireGameObject(newSaveGame, item.gameObjectId);
    const storageBehavior = getBehavior(gameObject, StorageBehavior);
    
    if (!storageBehavior) {
      continue;
    }

    const storageItem = storageBehavior.extraData[item.storageItemIndex];
    const newUnits = item.currentUnits * ratio;

    const modifiedStorageItem = changeStateBehaviorData(
      storageItem,
      PrimaryElementBehavior,
      "templateData",
      {
        Units: newUnits
      }
    );

    // Criar nova lista de extraData com o item modificado
    const newExtraData = [
      ...storageBehavior.extraData.slice(0, item.storageItemIndex),
      modifiedStorageItem,
      ...storageBehavior.extraData.slice(item.storageItemIndex + 1)
    ];

    // Modificar o storage behavior
    const modifiedGameObject = changeStateBehaviorData(
      gameObject,
      StorageBehavior,
      "extraData",
      newExtraData
    );

    // Substituir o gameObject
    newSaveGame = replaceGameObject(newSaveGame, modifiedGameObject);
  }

  return newSaveGame;
}
