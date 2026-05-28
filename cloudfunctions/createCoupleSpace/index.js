const crypto = require("crypto");
const cloudbase = require("@cloudbase/node-sdk");

const app = cloudbase.init({ env: cloudbase.SYMBOL_CURRENT_ENV });
const db = app.database();
const users = db.collection("users");
const spaces = db.collection("couple_spaces");

exports.main = async (event) => {
  try {
    const body = parseBody(event);
    const userId = requiredString(body.userId, "userId");
    const spaceName = requiredString(body.spaceName, "spaceName");
    const spaceCode = requiredString(body.spaceCode, "spaceCode");

    if (!/^[A-Za-z0-9_]{4,24}$/.test(spaceCode)) {
      throw new Error("情侣空间 ID 只能使用字母、数字、下划线，长度 4-24");
    }

    const userResult = await users.doc(userId).get();
    const user = userResult.data && userResult.data[0];
    if (!user) throw new Error("用户不存在");

    const existing = await spaces.where({ spaceCode }).limit(1).get();
    if ((existing.data || []).length > 0) {
      throw new Error("这个情侣空间 ID 已被占用");
    }

    const spaceId = createId("space");
    const now = Date.now();
    await spaces.doc(spaceId).set({
      spaceId,
      spaceName,
      spaceCode,
      ownerUserId: userId,
      leftUserId: userId,
      rightUserId: null,
      status: "WAITING_PARTNER",
      createdAt: now,
      updatedAt: now
    });

    await users.doc(userId).update({
      currentSpaceId: spaceId,
      updatedAt: now
    });

    return jsonResponse(200, {
      ok: true,
      session: {
        userId: user.userId,
        username: user.username,
        avatarUrl: user.avatarUrl || null,
        currentSpaceId: spaceId,
        spaceName,
        spaceCode,
        spaceStatus: "WAITING_PARTNER",
        partnerUsername: null,
        partnerAvatarUrl: null,
        isSpaceOwner: true
      }
    });
  } catch (error) {
    return jsonResponse(400, { ok: false, error: error.message || "createCoupleSpace failed" });
  }
};

function createId(prefix) {
  return `${prefix}_${crypto.randomBytes(8).toString("hex")}`;
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
