const cloudbase = require("@cloudbase/node-sdk");

const app = cloudbase.init({
  env: cloudbase.SYMBOL_CURRENT_ENV
});

const db = app.database();
const collection = db.collection("care_cycles");

exports.main = async (event) => {
  try {
    const body = parseBody(event);
    const coupleId = requiredString(body.coupleId, "coupleId");
    const userId = requiredString(body.userId, "userId");
    const careCycles = Array.isArray(body.careCycles) ? body.careCycles : [];
    const now = Date.now();

    let uploaded = 0;
    for (const cycle of careCycles) {
      const remoteId = cycle.remoteId || collection.doc().id;
      const payload = {
        ...cycle,
        remoteId,
        coupleId,
        ownerUserId: cycle.ownerUserId || userId,
        remoteUpdatedAt: now,
        updatedAt: cycle.updatedAt || now
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
      careCycles: queryResult.data || []
    });
  } catch (error) {
    return jsonResponse(400, {
      ok: false,
      error: error.message || "syncCareCycles failed"
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
