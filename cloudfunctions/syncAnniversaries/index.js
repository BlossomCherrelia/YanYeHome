const cloudbase = require("@cloudbase/node-sdk");

const app = cloudbase.init({
  env: cloudbase.SYMBOL_CURRENT_ENV
});

const db = app.database();
const collection = db.collection("anniversaries");

exports.main = async (event) => {
  try {
    const body = parseBody(event);
    const coupleId = requiredString(body.coupleId, "coupleId");
    const userId = requiredString(body.userId, "userId");
    const pendingAnniversaries = Array.isArray(body.pendingAnniversaries)
      ? body.pendingAnniversaries
      : [];

    let uploaded = 0;
    const now = Date.now();

    for (const anniversary of pendingAnniversaries) {
      const remoteId = anniversary.remoteId || db.collection("anniversaries").doc().id;
      const payload = {
        ...anniversary,
        remoteId,
        coupleId,
        ownerUserId: anniversary.ownerUserId || userId,
        createdBy: anniversary.createdBy || userId,
        visibility: anniversary.visibility || "SHARED",
        remoteUpdatedAt: now,
        updatedAt: anniversary.updatedAt || now
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

    return jsonResponse(200, {
      ok: true,
      uploaded,
      anniversaries: queryResult.data || []
    });
  } catch (error) {
    return jsonResponse(400, {
      ok: false,
      error: error.message || "syncAnniversaries failed"
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
