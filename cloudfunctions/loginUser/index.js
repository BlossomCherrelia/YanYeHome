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

    const query = await users.where({ username }).limit(1).get();
    const user = (query.data || [])[0];
    if (!user || user.passwordHash !== hashPassword(password)) {
      throw new Error("用户名或密码不正确");
    }

    const session = await buildSession(user, spaces, users);
    return jsonResponse(200, { ok: true, session });
  } catch (error) {
    return jsonResponse(400, { ok: false, error: error.message || "loginUser failed" });
  }
};

function hashPassword(password) {
  return crypto.createHash("sha256").update(password).digest("hex");
}

async function buildSession(user, spacesCollection, usersCollection) {
  const currentSpaceId = user.currentSpaceId || null;
  if (!currentSpaceId) {
    return {
      userId: user.userId,
      username: user.username,
      avatarUrl: user.avatarUrl || null,
      currentSpaceId: null,
      spaceName: null,
      spaceCode: null,
      spaceStatus: null,
      partnerUsername: null,
      partnerAvatarUrl: null,
      isSpaceOwner: false
    };
  }

  const spaceResult = await spacesCollection.doc(currentSpaceId).get();
  const space = spaceResult.data && spaceResult.data[0];
  if (!space) {
    return {
      userId: user.userId,
      username: user.username,
      avatarUrl: user.avatarUrl || null,
      currentSpaceId: null,
      spaceName: null,
      spaceCode: null,
      spaceStatus: null,
      partnerUsername: null,
      partnerAvatarUrl: null,
      isSpaceOwner: false
    };
  }

  const partnerId = space.leftUserId === user.userId ? space.rightUserId : space.leftUserId;
  let partnerUsername = null;
  let partnerAvatarUrl = null;
  if (partnerId) {
    const partnerResult = await usersCollection.doc(partnerId).get();
    const partner = partnerResult.data && partnerResult.data[0];
    partnerUsername = partner ? partner.username : null;
    partnerAvatarUrl = partner && partner.avatarUrl ? partner.avatarUrl : null;
  }

  return {
    userId: user.userId,
    username: user.username,
    avatarUrl: user.avatarUrl || null,
    currentSpaceId: space.spaceId,
    spaceName: space.spaceName,
    spaceCode: space.spaceCode,
    spaceStatus: space.status,
    partnerUsername,
    partnerAvatarUrl,
    isSpaceOwner: space.ownerUserId === user.userId
  };
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
