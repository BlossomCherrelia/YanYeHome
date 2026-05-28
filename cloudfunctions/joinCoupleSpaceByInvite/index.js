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
    const inviteCode = requiredString(body.inviteCode, "inviteCode");

    const userResult = await users.doc(userId).get();
    const user = userResult.data && userResult.data[0];
    if (!user) throw new Error("用户不存在");

    const inviteResult = await invites.where({ inviteCode, status: "ACTIVE" }).limit(1).get();
    const invite = (inviteResult.data || [])[0];
    if (!invite) throw new Error("邀请码不存在或已失效");
    if (invite.expiresAt && invite.expiresAt < Date.now()) {
      throw new Error("邀请码已过期");
    }

    const spaceResult = await spaces.doc(invite.spaceId).get();
    const space = spaceResult.data && spaceResult.data[0];
    if (!space) throw new Error("情侣空间不存在");
    if (space.rightUserId && space.rightUserId !== userId) {
      throw new Error("这个情侣空间已经绑定了另一位成员");
    }

    const now = Date.now();
    await spaces.doc(space.spaceId).update({
      rightUserId: userId,
      status: "PAIRED",
      updatedAt: now
    });
    await users.doc(userId).update({
      currentSpaceId: space.spaceId,
      updatedAt: now
    });
    await invites.doc(invite.inviteId).update({
      status: "USED",
      usedByUserId: userId,
      updatedAt: now
    });

    const ownerResult = await users.doc(space.leftUserId).get();
    const owner = ownerResult.data && ownerResult.data[0];

    return jsonResponse(200, {
      ok: true,
      session: {
        userId: user.userId,
        username: user.username,
        avatarUrl: user.avatarUrl || null,
        currentSpaceId: space.spaceId,
        spaceName: space.spaceName,
        spaceCode: space.spaceCode,
        spaceStatus: "PAIRED",
        partnerUsername: owner ? owner.username : null,
        partnerAvatarUrl: owner && owner.avatarUrl ? owner.avatarUrl : null,
        isSpaceOwner: false
      }
    });
  } catch (error) {
    return jsonResponse(400, { ok: false, error: error.message || "joinCoupleSpaceByInvite failed" });
  }
};

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
