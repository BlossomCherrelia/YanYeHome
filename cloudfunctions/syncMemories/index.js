const cloudbase = require("@cloudbase/node-sdk");

const app = cloudbase.init({
  env: cloudbase.SYMBOL_CURRENT_ENV
});

const db = app.database();
const collection = db.collection("memories");

exports.main = async (event) => {
  try {
    const body = parseBody(event);
    const coupleId = requiredString(body.coupleId, "coupleId");
    const userId = requiredString(body.userId, "userId");
    const pendingMemories = Array.isArray(body.pendingMemories) ? body.pendingMemories : [];

    let uploaded = 0;
    const now = Date.now();

    for (const memory of pendingMemories) {
      const remoteId = memory.remoteId || db.collection("memories").doc().id;
      const payload = {
        ...memory,
        remoteId,
        coupleId,
        ownerUserId: memory.ownerUserId || userId,
        locationName: memory.locationName || "",
        photoUris: memory.photoUris || "",
        foodNotes: memory.foodNotes || "",
        mood: memory.mood || "HAPPY",
        note: memory.note || "",
        remoteUpdatedAt: now,
        updatedAt: memory.updatedAt || now
      };

      await assertRemoteIdWritable(remoteId, coupleId);
      await collection.doc(remoteId).set(payload);
      uploaded += 1;
    }

    const queryResult = await collection
      .where({
        coupleId
      })
      .limit(1000)
      .get();

    const visibleMemories = (queryResult.data || []).filter((memory) => {
      if (memory.isDeleted) return true;
      return true;
    });

    return jsonResponse(200, {
      ok: true,
      uploaded,
      memories: visibleMemories
    });
  } catch (error) {
    return jsonResponse(400, {
      ok: false,
      error: error.message || "syncMemories failed"
    });
  }
};

async function assertRemoteIdWritable(remoteId, coupleId) {
  const existing = await collection.doc(remoteId).get();
  const data = existing && (Array.isArray(existing.data) ? existing.data[0] : existing.data);
  if (data && data.coupleId && data.coupleId !== coupleId) {
    throw new Error(`REMOTE_ID_COUPLE_CONFLICT:${remoteId}`);
  }
}

function parseBody(event) {
  if (!event) return {};
  if (typeof event.body === "string" && event.body.length > 0) {
    return JSON.parse(event.body);
  }
  if (typeof event === "object") {
    return event;
  }
  return {};
}

function requiredString(value, name) {
  if (typeof value !== "string" || value.trim().length === 0) {
    throw new Error(`${name} is required`);
  }
  return value.trim();
}

function jsonResponse(statusCode, body) {
  return {
    statusCode,
    headers: {
      "content-type": "application/json; charset=utf-8"
    },
    body: JSON.stringify(body)
  };
}
