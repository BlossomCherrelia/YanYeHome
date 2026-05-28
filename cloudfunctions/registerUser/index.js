const crypto = require("crypto");
const cloudbase = require("@cloudbase/node-sdk");

const app = cloudbase.init({ env: cloudbase.SYMBOL_CURRENT_ENV });
const db = app.database();
const users = db.collection("users");
const spaces = db.collection("couple_spaces");

exports.main = async (event) => {
  try {
    const body = parseBody(event);
    const username = requiredString(body.username, "username");
    const password = requiredString(body.password, "password");

    if (password.length < 6) {
      throw new Error("密码至少 6 位");
    }

    const existing = await users.where({ username }).limit(1).get();
    if ((existing.data || []).length > 0) {
      throw new Error("这个用户名已经注册过了");
    }

    const userId = createId("user");
    const now = Date.now();
    await users.doc(userId).set({
      userId,
      username,
      passwordHash: hashPassword(password),
      avatarUrl: "",
      currentSpaceId: null,
      createdAt: now,
      updatedAt: now
    });

    return jsonResponse(200, {
      ok: true,
      session: {
        userId,
        username,
        avatarUrl: null,
        currentSpaceId: null,
        spaceName: null,
        spaceCode: null,
        spaceStatus: null,
        partnerUsername: null,
        isSpaceOwner: false
      }
    });
  } catch (error) {
    return jsonResponse(400, { ok: false, error: error.message || "registerUser failed" });
  }
};

function hashPassword(password) {
  return crypto.createHash("sha256").update(password).digest("hex");
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
