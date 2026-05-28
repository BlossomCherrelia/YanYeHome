const cloudbase = require("@cloudbase/node-sdk");

const app = cloudbase.init({ env: cloudbase.SYMBOL_CURRENT_ENV });
const db = app.database();
const spaces = db.collection("couple_spaces");

const BUSINESS_COLLECTIONS = [
  "wishes",
  "schedules",
  "memories",
  "memos",
  "anniversaries",
  "restaurants",
  "footprints",
  "care_cycles"
];

exports.main = async (event) => {
  try {
    const body = parseBody(event);
    const userId = requiredString(body.userId, "userId");
    const fromCoupleId = requiredString(body.fromCoupleId, "fromCoupleId");
    const targetCoupleId = requiredString(body.targetCoupleId, "targetCoupleId");
    const dryRun = body.dryRun !== false;

    await assertSpaceMemberIfSpaceId(userId, fromCoupleId);

    const result = {};
    for (const collectionName of BUSINESS_COLLECTIONS) {
      result[collectionName] = await restoreCollection(collectionName, fromCoupleId, targetCoupleId, dryRun);
    }

    return jsonResponse(200, {
      ok: true,
      dryRun,
      fromCoupleId,
      targetCoupleId,
      result
    });
  } catch (error) {
    return jsonResponse(400, { ok: false, error: error.message || "restoreMovedLegacyData failed" });
  }
};

async function assertSpaceMemberIfSpaceId(userId, coupleId) {
  if (!coupleId.startsWith("space_")) return;
  const spaceResult = await spaces.doc(coupleId).get();
  const space = spaceResult && (Array.isArray(spaceResult.data) ? spaceResult.data[0] : spaceResult.data);
  if (!space) throw new Error("来源情侣空间不存在");
  if (space.leftUserId !== userId && space.rightUserId !== userId) {
    throw new Error("你不属于来源情侣空间");
  }
}

async function restoreCollection(collectionName, fromCoupleId, targetCoupleId, dryRun) {
  const collection = db.collection(collectionName);
  let scanned = 0;
  let updated = 0;

  while (true) {
    const result = await collection.where({ coupleId: fromCoupleId }).limit(100).get();
    const rows = result.data || [];
    if (rows.length === 0) return { scanned, updated };

    scanned += rows.length;
    if (dryRun) return { scanned, updated: 0 };

    for (const row of rows) {
      const id = row._id || row.remoteId;
      if (!id) continue;
      await collection.doc(id).update({
        coupleId: targetCoupleId,
        remoteUpdatedAt: Date.now(),
        updatedAt: row.updatedAt || Date.now()
      });
      updated += 1;
    }
  }
}

function parseBody(event) {
  if (!event) return {};
  if (typeof event.body === "string" && event.body.length > 0) return JSON.parse(event.body);
  if (typeof event === "object") return event;
  return {};
}

function requiredString(value, name) {
  if (typeof value !== "string" || value.trim().length === 0) throw new Error(`${name} is required`);
  return value.trim();
}

function jsonResponse(statusCode, body) {
  return {
    statusCode,
    headers: { "content-type": "application/json; charset=utf-8" },
    body: JSON.stringify(body)
  };
}
