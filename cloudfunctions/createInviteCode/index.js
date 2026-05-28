const crypto = require("crypto");
const cloudbase = require("@cloudbase/node-sdk");

const app = cloudbase.init({ env: cloudbase.SYMBOL_CURRENT_ENV });
const db = app.database();
const users = db.collection("users");
const spaces = db.collection("couple_spaces");
const invites = db.collection("space_invites");

exports.main = async (event) => {
  try {
    const body = parseBody(event);
    const userId = requiredString(body.userId, "userId");
    const spaceId = requiredString(body.spaceId, "spaceId");

    const userResult = await users.doc(userId).get();
    const user = userResult.data && userResult.data[0];
    if (!user) throw new Error("用户不存在");

    const spaceResult = await spaces.doc(spaceId).get();
    const space = spaceResult.data && spaceResult.data[0];
    if (!space) throw new Error("情侣空间不存在");
    if (space.leftUserId !== userId && space.rightUserId !== userId) {
      throw new Error("你不属于这个情侣空间");
    }

    const inviteCode = await generateUniqueInviteCode(invites);
    const now = Date.now();
    const expiresAt = now + 7 * 24 * 60 * 60 * 1000;
    const inviteId = createId("invite");
    await invites.doc(inviteId).set({
      inviteId,
      inviteCode,
      spaceId,
      createdByUserId: userId,
      status: "ACTIVE",
      expiresAt,
      usedByUserId: null,
      createdAt: now,
      updatedAt: now
    });

    return jsonResponse(200, { ok: true, inviteCode, expiresAt });
  } catch (error) {
    return jsonResponse(400, { ok: false, error: error.message || "createInviteCode failed" });
  }
};

async function generateUniqueInviteCode(invitesCollection) {
  for (let attempt = 0; attempt < 10; attempt += 1) {
    const inviteCode = crypto.randomBytes(4).toString("hex").slice(0, 8).toUpperCase();
    const existing = await invitesCollection.where({ inviteCode, status: "ACTIVE" }).limit(1).get();
    if ((existing.data || []).length === 0) {
      return inviteCode;
    }
  }
  throw new Error("邀请码生成失败，请稍后重试");
}

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
