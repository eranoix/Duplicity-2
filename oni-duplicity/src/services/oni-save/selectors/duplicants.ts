import { createSelector } from "reselect";
import createCachedSelector from "re-reselect";
import { getBehavior, AccessorizerBehavior, MinionIdentityBehavior } from "oni-save-parser";
import { isNotNull } from "@/utils";

import { gameObjectGroupsSelector, gameObjectsByIdSelector } from "./game-objects";
import { createServiceSelector } from "./utils";
import { getGameObjectId } from "../utils";

/**
 * Seletor que retorna IDs de todos os objetos que são duplicantes editáveis
 * (tanto humanos quanto bionics - ambos têm Accessorizer e MinionIdentity behaviors)
 */
export const editableDuplicantIdsSelector = createServiceSelector(
  createSelector(
    gameObjectGroupsSelector.local,
    groups => {
      if (!groups) {
        return [];
      }

      const duplicantIds: number[] = [];
      
      for (const group of groups) {
        for (const gameObject of group.gameObjects) {
          // Verificar se tem Accessorizer (aparência customizável)
          const accessorizer = getBehavior(gameObject, AccessorizerBehavior);
          if (!accessorizer) {
            continue;
          }
          
          // Verificar se tem MinionIdentity (identidade do duplicante)
          const minionIdentity = getBehavior(gameObject, MinionIdentityBehavior);
          if (!minionIdentity) {
            continue;
          }
          
          // Se tem ambos os behaviors, é um duplicante editável (humano ou bionic)
          const id = getGameObjectId(gameObject);
          if (isNotNull(id)) {
            duplicantIds.push(id);
          }
        }
      }
      
      return duplicantIds.sort((a, b) => a - b);
    }
  )
);

/**
 * Seletor que verifica se um gameObjectId é um duplicante editável
 * (tem Accessorizer e MinionIdentity behaviors)
 */
export const isEditableDuplicantSelector = createCachedSelector(
  gameObjectsByIdSelector,
  (_: any, gameObjectId: number) => gameObjectId,
  (gameObjectsById, gameObjectId) => {
    if (!gameObjectsById || !gameObjectsById[gameObjectId]) {
      return false;
    }
    
    const gameObject = gameObjectsById[gameObjectId];
    
    // Verificar se tem Accessorizer (aparência customizável)
    const accessorizer = getBehavior(gameObject, AccessorizerBehavior);
    if (!accessorizer) {
      return false;
    }
    
    // Verificar se tem MinionIdentity (identidade do duplicante)
    const minionIdentity = getBehavior(gameObject, MinionIdentityBehavior);
    if (!minionIdentity) {
      return false;
    }
    
    // Se tem ambos os behaviors, é um duplicante editável
    return true;
  }
)((_: any, gameObjectId: number) => gameObjectId);
