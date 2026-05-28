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
    const spaceId = requiredString(body.spaceId, "spaceId");

    const spaceResult = await spaces.doc(spaceId).get();
    const space = spaceResult && (Array.isArray(spaceResult.data) ? spaceResult.data[0] : spaceResult.data);
    if (!space) throw new Error("情侣空间不存在");
    if (space.leftUserId !== userId && space.rightUserId !== userId) {
      throw new Error("你不属于这个情侣空间");
    }

    const deleted = {};
    for (const collectionName of BUSINESS_COLLECTIONS) {
      deleted[collectionName] = await deleteByCoupleId(collectionName, spaceId);
    }

    return jsonResponse(200, { ok: true, deleted });
  } catch (error) {
    return jsonResponse(400, { ok: false, error: error.message || "clearSpaceBusinessData failed" });
  }
};

async function deleteByCoupleId(collectionName, coupleId) {
  const collection = db.collection(collectionName);
  let deleted = 0;

  while (true) {
    const result = await collection.where({ coupleId }).limit(100).get();
    const rows = result.data || [];
    if (rows.length === 0) return deleted;

    for (const row of rows) {
      const id = row._id || row.remoteId;
      if (!id) continue;
      await collection.doc(id).remove();
      deleted += 1;
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
