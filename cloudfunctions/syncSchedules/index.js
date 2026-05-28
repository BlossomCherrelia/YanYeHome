const cloudbase = require("@cloudbase/node-sdk");

const app = cloudbase.init({
  env: cloudbase.SYMBOL_CURRENT_ENV
});

const db = app.database();
const collection = db.collection("schedules");

exports.main = async (event) => {
  try {
    const body = parseBody(event);
    const coupleId = requiredString(body.coupleId, "coupleId");
    const userId = requiredString(body.userId, "userId");
    const pendingSchedules = Array.isArray(body.pendingSchedules) ? body.pendingSchedules : [];

    let uploaded = 0;
    const now = Date.now();

    for (const schedule of pendingSchedules) {
      const remoteId = schedule.remoteId || db.collection("schedules").doc().id;
      const payload = {
        ...schedule,
        remoteId,
        coupleId,
        ownerUserId: schedule.ownerUserId || userId,
        visibility: schedule.visibility || "SHARED",
        remoteUpdatedAt: now,
        updatedAt: schedule.updatedAt || now
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

    const visibleSchedules = (queryResult.data || []).filter((schedule) => {
      if (schedule.isDeleted) return true;
      if (schedule.ownerUserId === userId) return true;
      if (schedule.visibility === "PRIVATE") return false;
      return true;
    });

    return jsonResponse(200, {
      ok: true,
      uploaded,
      schedules: visibleSchedules
    });
  } catch (error) {
    return jsonResponse(400, {
      ok: false,
      error: error.message || "syncSchedules failed"
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
