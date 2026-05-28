const cloudbase = require("@cloudbase/node-sdk");

const app = cloudbase.init({
  env: cloudbase.SYMBOL_CURRENT_ENV
});

const db = app.database();
const collection = db.collection("footprints");

exports.main = async (event) => {
  try {
    const body = parseBody(event);
    const coupleId = requiredString(body.coupleId, "coupleId");
    const userId = requiredString(body.userId, "userId");
    const provinceLights = Array.isArray(body.provinceLights) ? body.provinceLights : [];
    const cityLights = Array.isArray(body.cityLights) ? body.cityLights : [];
    const cityMemories = Array.isArray(body.cityMemories) ? body.cityMemories : [];
    const now = Date.now();

    const uploaded =
      (await writeItems(provinceLights, "PROVINCE_LIGHT", coupleId, userId, now)) +
      (await writeItems(cityLights, "CITY_LIGHT", coupleId, userId, now)) +
      (await writeItems(cityMemories, "CITY_MEMORY", coupleId, userId, now));

    const queryResult = await collection
      .where({
        coupleId
      })
      .limit(1000)
      .get();
    const data = queryResult.data || [];

    return jsonResponse(200, {
      ok: true,
      uploaded,
      provinceLights: data.filter((item) => item.type === "PROVINCE_LIGHT"),
      cityLights: data.filter((item) => item.type === "CITY_LIGHT"),
      cityMemories: data.filter((item) => item.type === "CITY_MEMORY")
    });
  } catch (error) {
    return jsonResponse(400, {
      ok: false,
      error: error.message || "syncFootprints failed"
    });
  }
};

async function writeItems(items, type, coupleId, userId, now) {
  let uploaded = 0;

  for (const item of items) {
    const remoteId = item.remoteId || collection.doc().id;
    const payload = {
      ...item,
      remoteId,
      type,
      coupleId,
      ownerUserId: item.ownerUserId || userId,
      remoteUpdatedAt: now,
      updatedAt: item.updatedAt || now
    };

    await assertRemoteIdWritable(remoteId, coupleId);
    await collection.doc(remoteId).set(payload);
    uploaded += 1;
  }

  return uploaded;
}

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
