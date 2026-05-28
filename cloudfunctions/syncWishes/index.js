const cloudbase = require("@cloudbase/node-sdk");

const app = cloudbase.init({
  env: cloudbase.SYMBOL_CURRENT_ENV
});

const db = app.database();
const collection = db.collection("wishes");

exports.main = async (event) => {
  try {
    const body = parseBody(event);
    const coupleId = requiredString(body.coupleId, "coupleId");
    const userId = requiredString(body.userId, "userId");
    const todayEpochDay = Number(body.todayEpochDay || 0);
    const pendingWishes = Array.isArray(body.pendingWishes) ? body.pendingWishes : [];

    let uploaded = 0;
    const now = Date.now();

    for (const wish of pendingWishes) {
      const remoteId = wish.remoteId || db.collection("wishes").doc().id;
      const payload = {
        ...wish,
        remoteId,
        coupleId,
        ownerUserId: wish.ownerUserId || userId,
        createdBy: wish.createdBy || userId,
        visibility: wish.visibility || "SHARED",
        remoteUpdatedAt: now,
        updatedAt: wish.updatedAt || now
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

    const visibleWishes = (queryResult.data || []).filter((wish) => {
      if (wish.isDeleted) return true;
      if (wish.ownerUserId === userId) return true;
      if (wish.visibility === "PRIVATE") return false;
      if (wish.visibility === "REVEAL_AFTER_DATE") {
        return Number(wish.revealAfterEpochDay || 0) <= todayEpochDay;
      }
      return true;
    });

    return jsonResponse(200, {
      ok: true,
      uploaded,
      wishes: visibleWishes
    });
  } catch (error) {
    return jsonResponse(400, {
      ok: false,
      error: error.message || "syncWishes failed"
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
