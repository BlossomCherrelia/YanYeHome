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
    const username = optionalString(body.username);
    const avatarUrl = optionalString(body.avatarUrl);
    const password = optionalString(body.password);
    const spaceName = optionalString(body.spaceName);

    const userResult = await users.doc(userId).get();
    const user = userResult.data && userResult.data[0];
    if (!user) throw new Error("用户不存在");
    if (username && username !== user.username) {
      const duplicateResult = await users.where({ username }).limit(1).get();
      const duplicate = (duplicateResult.data || [])[0];
      if (duplicate && duplicate.userId !== userId) throw new Error("这个用户名已被使用");
    }

    const now = Date.now();
    const userPatch = { updatedAt: now };
    if (username) userPatch.username = username;
    if (avatarUrl) userPatch.avatarUrl = avatarUrl;
    if (password) {
      if (password.length < 6) throw new Error("密码至少 6 位");
      userPatch.passwordHash = hashPassword(password);
    }
    await users.doc(userId).update(userPatch);

    let space = null;
    if (user.currentSpaceId) {
      const spaceResult = await spaces.doc(user.currentSpaceId).get();
      space = spaceResult.data && spaceResult.data[0];
      if (space && spaceName) {
        const spacePatch = {
          spaceName,
          updatedAt: now
        };
        if (username) {
          if (space.leftUserId === userId) spacePatch.leftUsername = username;
          if (space.rightUserId === userId) spacePatch.rightUsername = username;
        }
        await spaces.doc(user.currentSpaceId).update(spacePatch);
        space = { ...space, ...spacePatch };
      }
    }

    const latestUser = {
      ...user,
      ...userPatch,
      username: userPatch.username || user.username,
      avatarUrl: userPatch.avatarUrl || user.avatarUrl
    };

    return jsonResponse(200, {
      ok: true,
      session: await buildSession(latestUser, space)
    });
  } catch (error) {
    return jsonResponse(400, { ok: false, error: error.message || "updateSessionProfile failed" });
  }
};

async function buildSession(user, space) {
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
  const isLeft = space.leftUserId === user.userId;
  const partnerUserId = isLeft ? space.rightUserId : space.leftUserId;
  const partner = partnerUserId ? await findUser(partnerUserId) : null;
  return {
    userId: user.userId,
    username: user.username,
    avatarUrl: user.avatarUrl || null,
    currentSpaceId: space.spaceId,
    spaceName: space.spaceName,
    spaceCode: space.spaceCode,
    spaceStatus: space.status,
    partnerUsername: isLeft ? space.rightUsername || null : space.leftUsername || null,
    partnerAvatarUrl: partner && partner.avatarUrl ? partner.avatarUrl : null,
    isSpaceOwner: space.ownerUserId === user.userId
  };
}

async function findUser(userId) {
  const result = await users.doc(userId).get();
  return result.data && result.data[0] ? result.data[0] : null;
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

function optionalString(value) {
  if (typeof value !== "string") return null;
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}

function hashPassword(password) {
  return crypto.createHash("sha256").update(password).digest("hex");
}

function jsonResponse(statusCode, body) {
  return {
    statusCode,
    headers: { "content-type": "application/json; charset=utf-8" },
    body: JSON.stringify(body)
  };
}
