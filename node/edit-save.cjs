#!/usr/bin/env node
/* eslint-disable no-console */

const fs = require("node:fs");
const path = require("node:path");

const { parseSaveGame, writeSaveGame } = require("./oni-save-parser/lib/index.js");
const {
  ACCESSORY_ID_PREFIX,
  AccessoriesByType,
  makeAccessoryID,
  Accessory,
  getAccessoryName,
  getAccessoryOfType,
  getIndexOfAccessoryType,
} = require("./oni-save-parser/lib/save-structure/const-data/accessories.js");

function castInt32(val) {
  const a = new Int32Array(1);
  a[0] = val;
  return a[0];
}

function sdbm32LowerHash(str) {
  if (str == null) return 0;
  str = String(str).toLowerCase();
  let num = 0;
  for (let i = 0; i < str.length; i++) {
    num = str.charCodeAt(i) + (num << 6) + (num << 16) - num;
  }
  return castInt32(num);
}

function normalizeAccessoryName(name) {
  if (name == null) return null;
  const s = String(name).trim();
  if (!s) return null;
  if (s.startsWith(ACCESSORY_ID_PREFIX)) return s.substring(ACCESSORY_ID_PREFIX.length);
  return s;
}

const KNOWN_ACCESSORY_NAMES = (() => {
  const out = {};
  for (const [type, names] of Object.entries(AccessoriesByType)) {
    if (Array.isArray(names)) out[type] = names.slice();
  }
  return out;
})();

const ACCESSORY_HASH_TO_NAME = (() => {
  const map = new Map();
  for (const names of Object.values(KNOWN_ACCESSORY_NAMES)) {
    for (const n of names) {
      map.set(sdbm32LowerHash(n), n);
      // some saves may hash the full Guid string instead of the short name
      map.set(sdbm32LowerHash(makeAccessoryID(n)), n);
    }
  }
  return map;
})();

function parseArgs(argv) {
  const args = {
    in: null,
    out: null,
    dupes: [],
    sets: [],
    list: false,
    listMode: "dupes", // dupes|geysers|materials|accessories
    filter: null,
    limit: 500,
    applyJson: null,
  };
  for (let i = 2; i < argv.length; i++) {
    const a = argv[i];
    if (a === "--in") args.in = argv[++i];
    else if (a === "--out") args.out = argv[++i];
    else if (a === "--dupe") args.dupes.push(argv[++i]);
    else if (a === "--set") args.sets.push(argv[++i]);
    else if (a === "--list") args.list = true;
    else if (a === "--mode") args.listMode = argv[++i];
    else if (a === "--filter") args.filter = argv[++i];
    else if (a === "--limit") args.limit = Number(argv[++i]);
    else if (a === "--apply-json") args.applyJson = argv[++i];
    else if (a === "--help" || a === "-h") args.help = true;
    else throw new Error(`Argumento desconhecido: ${a}`);
  }
  return args;
}

function bufferToArrayBuffer(buf) {
  return buf.buffer.slice(buf.byteOffset, buf.byteOffset + buf.byteLength);
}

function applyEditsToMinion(minionObj, edits) {
  const behaviors = minionObj.behaviors || [];

  const attr = behaviors.find((b) => b && b.name === "Klei.AI.AttributeLevels");
  if (attr && attr.templateData && Array.isArray(attr.templateData.saveLoadLevels)) {
    const levels = attr.templateData.saveLoadLevels;
    for (const [key, value] of Object.entries(edits)) {
      const lvl = levels.find((x) => x && x.attributeId === key);
      if (lvl) {
        lvl.level = value;
        if (typeof lvl.experience === "number") lvl.experience = 0;
      } else {
        // If the attribute isn't present (mods/newer builds), add it.
        levels.push({ attributeId: key, experience: 0, level: value });
      }
    }
  }

  const resume = behaviors.find((b) => b && b.name === "MinionResume");
  if (resume && resume.templateData) {
    // If a key exists in MasteryBySkillID, toggle it on.
    const mastery = resume.templateData.MasteryBySkillID;
    if (Array.isArray(mastery)) {
      for (let i = 0; i < mastery.length; i++) {
        const pair = mastery[i];
        if (!Array.isArray(pair) || pair.length < 2) continue;
        const skillId = pair[0];
        if (edits[skillId] != null) {
          mastery[i] = [skillId, true];
        }
      }
    }
  }
}

function isHumanOrBionic(obj, groupName) {
  // Humanos e bionicos têm Accessorizer (aparência customizável)
  // Robôs não têm Accessorizer
  // Além disso, verificamos se é do grupo "Minion" (prefab dos duplicantes)
  // Robôs geralmente estão em grupos diferentes como "FetchDrone", "PropGravitasCeilingRobot", etc.
  const behaviors = obj.behaviors || [];
  const hasAccessorizer = behaviors.some((b) => b && b.name === "Accessorizer");
  const hasMinionIdentity = behaviors.some((b) => b && b.name === "MinionIdentity");
  
  // Para ser humano/bionico, precisa ter tanto Accessorizer quanto MinionIdentity
  // E preferencialmente estar no grupo "Minion", mas aceitamos outros grupos se tiver os behaviors corretos
  if (!hasAccessorizer || !hasMinionIdentity) return false;
  
  // Se está no grupo Minion, é definitivamente um duplicante humano/bionico
  if (groupName === "Minion") return true;
  
  // Se tem Accessorizer e MinionIdentity, provavelmente é humano/bionico mesmo em outros grupos
  // (alguns mods podem criar grupos customizados)
  return true;
}

function getMinionName(minionObj) {
  const behaviors = minionObj.behaviors || [];
  const id = behaviors.find((b) => b && b.name === "MinionIdentity");
  return id?.templateData?.name ?? null;
}

function getMinionIdentity(minionObj) {
  const behaviors = minionObj.behaviors || [];
  const id = behaviors.find((b) => b && b.name === "MinionIdentity");
  const t = id?.templateData;
  if (!t) return null;
  return {
    gender: t.gender ?? null,
    voiceIdx: typeof t.voiceIdx === "number" ? t.voiceIdx : null,
  };
}

function getMinionAppearance(minionObj) {
  const behaviors = minionObj.behaviors || [];
  const acc = behaviors.find((b) => b && b.name === "Accessorizer");
  const accessories = acc?.templateData?.accessories;
  if (!Array.isArray(accessories)) return null;

  const types = ["hair", "headshape", "eyes", "body", "mouth", "arm", "neck", "hat", "hat_hair", "hair_always"];
  const out = {};
  for (const type of types) {
    const a = getAccessoryOfType(accessories, type);
    if (!a) continue;
    const name = getAccessoryName(a) || getAccessoryName(a.guid?.Guid) || null;
    if (name) out[type] = name;
  }
  return out;
}

function getMinionAttributes(minionObj) {
  const behaviors = minionObj.behaviors || [];
  const attr = behaviors.find((b) => b && b.name === "Klei.AI.AttributeLevels");
  const out = {};
  if (attr && attr.templateData && Array.isArray(attr.templateData.saveLoadLevels)) {
    for (const lvl of attr.templateData.saveLoadLevels) {
      if (!lvl || typeof lvl.attributeId !== "string") continue;
      if (typeof lvl.level !== "number") continue;
      out[lvl.attributeId] = lvl.level;
    }
  }
  return out;
}

function applyIdentityToMinion(minionObj, identityEdits) {
  if (!identityEdits || typeof identityEdits !== "object") return;
  const behaviors = minionObj.behaviors || [];
  const id = behaviors.find((b) => b && b.name === "MinionIdentity");
  if (!id || !id.templateData) return;
  const t = id.templateData;

  if (typeof identityEdits.gender === "string" && identityEdits.gender.length > 0) {
    t.gender = identityEdits.gender;
    // keep in sync if present
    if (typeof t.genderStringKey === "string") t.genderStringKey = identityEdits.gender;
  }
  if (typeof identityEdits.voiceIdx === "number" && Number.isFinite(identityEdits.voiceIdx)) {
    t.voiceIdx = identityEdits.voiceIdx;
  }
}

function applyAppearanceToMinion(minionObj, appearanceEdits) {
  if (!appearanceEdits || typeof appearanceEdits !== "object") return;
  const behaviors = minionObj.behaviors || [];
  const acc = behaviors.find((b) => b && b.name === "Accessorizer");
  if (!acc || !acc.templateData || !Array.isArray(acc.templateData.accessories)) return;
  const accessories = acc.templateData.accessories;

  function setAccessory(type, name) {
    const index = getIndexOfAccessoryType(accessories, type);
    if (index === -1) return;
    accessories[index] = Accessory(normalizeAccessoryName(name));
  }

  for (const [type, name] of Object.entries(appearanceEdits)) {
    if (typeof name !== "string" || name.trim() === "") continue;
    setAccessory(type, name);
  }
}

function getMinionSkills(minionObj) {
  const behaviors = minionObj.behaviors || [];
  const resume = behaviors.find((b) => b && b.name === "MinionResume");
  const mastery = resume?.templateData?.MasteryBySkillID;
  if (!Array.isArray(mastery)) return null;
  const out = {};
  for (const pair of mastery) {
    if (!Array.isArray(pair) || pair.length < 2) continue;
    const id = pair[0];
    const val = Boolean(pair[1]);
    if (typeof id === "string") out[id] = val;
  }
  return out;
}

function applySkillsToMinion(minionObj, skillsEdits) {
  if (!skillsEdits || typeof skillsEdits !== "object") return;
  const behaviors = minionObj.behaviors || [];
  const resume = behaviors.find((b) => b && b.name === "MinionResume");
  if (!resume || !resume.templateData) return;
  const mastery = resume.templateData.MasteryBySkillID;
  if (!Array.isArray(mastery)) return;

  const map = new Map();
  for (const [k, v] of Object.entries(skillsEdits)) {
    if (typeof v === "boolean") map.set(k, v);
    else if (typeof v === "number") map.set(k, Boolean(v));
  }
  if (map.size === 0) return;

  for (let i = 0; i < mastery.length; i++) {
    const pair = mastery[i];
    if (!Array.isArray(pair) || pair.length < 2) continue;
    const id = pair[0];
    if (!map.has(id)) continue;
    mastery[i] = [id, map.get(id)];
  }
}

function getBehavior(obj, name) {
  const behaviors = obj.behaviors || [];
  return behaviors.find((b) => b && b.name === name) || null;
}

function getObjPosition(obj) {
  const p = obj.position;
  if (!p) return null;
  // vector3 shape from parser: {x,y,z}
  const x = typeof p.x === "number" ? p.x : null;
  const y = typeof p.y === "number" ? p.y : null;
  const z = typeof p.z === "number" ? p.z : null;
  if (x == null || y == null || z == null) return null;
  return { x, y, z };
}

function main() {
  const args = parseArgs(process.argv);
  if (args.help) {
    console.log(
      [
        "Uso:",
        "  node edit-save.cjs --list --mode dupes|geysers|materials --in <arquivo.sav> [--filter <texto>] [--limit 500]",
        "  node edit-save.cjs --in <arquivo.sav> --out <novo.sav> [--dupe \"Nome\"] --set Athletics=20 --set Digging=10 ...",
        "  node edit-save.cjs --apply-json <patch.json> --in <arquivo.sav> --out <novo.sav>",
        "",
        "Notas:",
        "- Se você passar --dupe, edita só aqueles nomes; senão, edita todos os duplicantes.",
        "- As chaves de --set são aplicadas em AttributeLevels (attributeId) e também ativam MasteryBySkillID se existir.",
      ].join("\n")
    );
    process.exit(0);
  }

  if (!args.in) {
    throw new Error("Você precisa passar --in <arquivo.sav>");
  }

  const inPath = path.resolve(args.in);
  const buf = fs.readFileSync(inPath);
  // Newer ONI builds often bump only the minor version.
  // Allow unknown minor versions as long as the major matches.
  const save = parseSaveGame(bufferToArrayBuffer(buf), { versionStrictness: "major" });

  if (args.list) {
    const mode = args.listMode || "dupes";
    const filter = args.filter ? String(args.filter).toLowerCase() : null;
    const limit = Number.isFinite(args.limit) ? Math.max(1, Math.min(10000, args.limit)) : 500;

    if (mode === "dupes") {
      const dupes = [];
      for (const group of save.gameObjects || []) {
        // Pular grupos de robôs conhecidos
        const groupName = group.name || "";
        if (groupName.includes("Robot") || groupName.includes("Drone") || 
            groupName.includes("Sweepy") || groupName.includes("Rover") ||
            groupName.includes("Automated") || groupName === "FetchDrone" ||
            groupName === "PropGravitasCeilingRobot" || groupName === "PropGravitasFloorRobot") {
          continue;
        }
        
        for (const obj of group.gameObjects || []) {
          // Filtrar apenas humanos e bionicos (excluir robôs)
          if (!isHumanOrBionic(obj, groupName)) continue;
          const name = getMinionName(obj);
          if (!name) continue;
          // Pular nomes que são apenas números (IDs de robôs)
          if (/^#?\d+$/.test(name)) continue;
          if (filter && !name.toLowerCase().includes(filter)) continue;
          dupes.push({
            name,
            attrs: getMinionAttributes(obj),
            identity: getMinionIdentity(obj),
            appearance: getMinionAppearance(obj),
            skills: getMinionSkills(obj),
          });
        }
      }
      dupes.sort((a, b) => a.name.localeCompare(b.name, "pt-BR"));
      process.stdout.write(JSON.stringify({ dupes }) + "\n");
      return;
    }

    if (mode === "accessories") {
      const byType = {};
      for (const [type, names] of Object.entries(KNOWN_ACCESSORY_NAMES)) {
        byType[type] = names.map((name) => ({
          name,
          guid: makeAccessoryID(name),
          hash: sdbm32LowerHash(name),
        }));
      }
      process.stdout.write(JSON.stringify({ accessories: { byType } }) + "\n");
      return;
    }

    if (mode === "geysers") {
      const geysers = [];
      for (let gi = 0; gi < (save.gameObjects || []).length; gi++) {
        const group = save.gameObjects[gi];
        for (let oi = 0; oi < (group.gameObjects || []).length; oi++) {
          const obj = group.gameObjects[oi];
          const geyser = getBehavior(obj, "Geyser");
          if (!geyser) continue;
          const pos = getObjPosition(obj);
          const cfg = geyser.templateData?.configuration ?? null;
          const typeId = cfg?.typeId ?? null;
          const id = `${gi}:${oi}`;
          const label = `${group.name} @ ${pos ? `${pos.x},${pos.y},${pos.z}` : "?"}`;
          if (filter && !(String(typeId).toLowerCase().includes(filter) || label.toLowerCase().includes(filter)))
            continue;
          geysers.push({ id, group: group.name, position: pos, configuration: cfg, typeId });
          if (geysers.length >= limit) break;
        }
        if (geysers.length >= limit) break;
      }
      process.stdout.write(JSON.stringify({ geysers, truncated: geysers.length >= limit }) + "\n");
      return;
    }

    if (mode === "materials") {
      const materials = [];
      for (let gi = 0; gi < (save.gameObjects || []).length; gi++) {
        const group = save.gameObjects[gi];
        for (let oi = 0; oi < (group.gameObjects || []).length; oi++) {
          const obj = group.gameObjects[oi];
          const pe = getBehavior(obj, "PrimaryElement");
          if (!pe) continue;
          const pos = getObjPosition(obj);
          const t = pe.templateData || {};
          const element = t.ElementID ?? null;
          const id = `${gi}:${oi}`;
          const label = `${group.name} @ ${pos ? `${pos.x},${pos.y},${pos.z}` : "?"}`;
          if (filter && !(String(element).toLowerCase().includes(filter) || label.toLowerCase().includes(filter)))
            continue;
          materials.push({
            id,
            group: group.name,
            position: pos,
            elementId: element,
            units: typeof t.Units === "number" ? t.Units : null,
            temperature: typeof t._Temperature === "number" ? t._Temperature : null,
          });
          if (materials.length >= limit) break;
        }
        if (materials.length >= limit) break;
      }
      process.stdout.write(JSON.stringify({ materials, truncated: materials.length >= limit }) + "\n");
      return;
    }

    throw new Error(`Modo inválido em --mode: ${mode}`);
    return;
  }

  if (!args.out) {
    throw new Error("Você precisa passar --out <novo.sav> (a não ser que esteja usando --list)");
  }

  const outPath = path.resolve(args.out);

  // Apply per-dupe patch JSON if provided.
  if (args.applyJson) {
    const patchPath = path.resolve(args.applyJson);
    const patch = JSON.parse(fs.readFileSync(patchPath, "utf-8"));
    const dupesPatch = patch?.dupes && typeof patch.dupes === "object" ? patch.dupes : {};
    const objsPatch = patch?.objects && typeof patch.objects === "object" ? patch.objects : {};

    let edited = 0;
    for (const group of save.gameObjects || []) {
      // Pular grupos de robôs conhecidos
      const groupName = group.name || "";
      if (groupName.includes("Robot") || groupName.includes("Drone") || 
          groupName.includes("Sweepy") || groupName.includes("Rover") ||
          groupName.includes("Automated") || groupName === "FetchDrone" ||
          groupName === "PropGravitasCeilingRobot" || groupName === "PropGravitasFloorRobot") {
        continue;
      }
      
      for (const obj of group.gameObjects || []) {
        // Filtrar apenas humanos e bionicos (excluir robôs)
        if (!isHumanOrBionic(obj, groupName)) continue;
        const name = getMinionName(obj);
        if (!name) continue;
        // Pular nomes que são apenas números (IDs de robôs)
        if (/^#?\d+$/.test(name)) continue;
        const edits = dupesPatch[name];
        if (!edits || typeof edits !== "object") continue;
        if (edits.attrs && typeof edits.attrs === "object") applyEditsToMinion(obj, edits.attrs);
        if (edits.identity && typeof edits.identity === "object") applyIdentityToMinion(obj, edits.identity);
        if (edits.appearance && typeof edits.appearance === "object") applyAppearanceToMinion(obj, edits.appearance);
        if (edits.skills && typeof edits.skills === "object") applySkillsToMinion(obj, edits.skills);
        edited++;
      }
    }

    // Apply object patches by id = "gi:oi"
    for (let gi = 0; gi < (save.gameObjects || []).length; gi++) {
      const group = save.gameObjects[gi];
      for (let oi = 0; oi < (group.gameObjects || []).length; oi++) {
        const obj = group.gameObjects[oi];
        const id = `${gi}:${oi}`;
        const edits = objsPatch[id];
        if (!edits || typeof edits !== "object") continue;

        if (edits.geyser && typeof edits.geyser === "object") {
          const geyser = getBehavior(obj, "Geyser");
          if (geyser) {
            geyser.templateData = geyser.templateData || {};
            geyser.templateData.configuration = geyser.templateData.configuration || {};
            Object.assign(geyser.templateData.configuration, edits.geyser);
          }
        }

        if (edits.primaryElement && typeof edits.primaryElement === "object") {
          const pe = getBehavior(obj, "PrimaryElement");
          if (pe) {
            pe.templateData = pe.templateData || {};
            const t = pe.templateData;
            if (edits.primaryElement.elementId != null) t.ElementID = edits.primaryElement.elementId;
            if (typeof edits.primaryElement.units === "number") t.Units = edits.primaryElement.units;
            if (typeof edits.primaryElement.temperature === "number") t._Temperature = edits.primaryElement.temperature;
          }
        }
      }
    }

    const out = writeSaveGame(save);
    fs.writeFileSync(outPath, Buffer.from(out));
    console.log(`OK: ${edited} duplicante(s) editado(s). Salvo em: ${outPath}`);
    return;
  }

  const edits = {};
  for (const s of args.sets || []) {
    const eq = s.indexOf("=");
    if (eq <= 0) throw new Error(`Formato inválido em --set: ${s} (use chave=numero)`);
    const key = s.slice(0, eq).trim();
    const valStr = s.slice(eq + 1).trim();
    const value = Number(valStr);
    if (!Number.isFinite(value)) throw new Error(`Valor inválido em --set: ${s}`);
    edits[key] = value;
  }

  if (Object.keys(edits).length === 0) {
    throw new Error("Você precisa passar pelo menos um --set chave=valor, ou usar --apply-json.");
  }

  let edited = 0;
  const wanted = new Set(args.dupes);

  for (const group of save.gameObjects || []) {
    // Pular grupos de robôs conhecidos
    const groupName = group.name || "";
    if (groupName.includes("Robot") || groupName.includes("Drone") || 
        groupName.includes("Sweepy") || groupName.includes("Rover") ||
        groupName.includes("Automated") || groupName === "FetchDrone" ||
        groupName === "PropGravitasCeilingRobot" || groupName === "PropGravitasFloorRobot") {
      continue;
    }
    
    for (const obj of group.gameObjects || []) {
      // Filtrar apenas humanos e bionicos (excluir robôs)
      if (!isHumanOrBionic(obj, groupName)) continue;
      const name = getMinionName(obj);
      if (!name) continue;
      // Pular nomes que são apenas números (IDs de robôs)
      if (/^#?\d+$/.test(name)) continue;
      if (wanted.size > 0 && !wanted.has(name)) continue;
      applyEditsToMinion(obj, edits);
      edited++;
    }
  }

  const out = writeSaveGame(save);
  fs.writeFileSync(outPath, Buffer.from(out));
  console.log(`OK: ${edited} duplicante(s) editado(s). Salvo em: ${outPath}`);
}

main();
