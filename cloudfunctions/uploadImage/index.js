const crypto = require("crypto");
const cloudbase = require("@cloudbase/node-sdk");

const app = cloudbase.init({
  env: cloudbase.SYMBOL_CURRENT_ENV
});

exports.main = async (event) => {
  try {
    if (event && event.httpMethod === "OPTIONS") {
      return jsonResponse(204, {});
    }

    const body = parseBody(event);
    const coupleId = requiredString(body.coupleId, "coupleId");
    const userId = requiredString(body.userId, "userId");
    const moduleName = safePathPart(body.module || "images");
    const mimeType = normalizeMimeType(body.mimeType || "image/jpeg");
    const base64 = requiredString(body.base64, "base64");
    const fileContent = Buffer.from(base64, "base64");

    if (fileContent.length <= 0) {
      throw new Error("empty image");
    }
    if (fileContent.length > 5 * 1024 * 1024) {
      throw new Error("image too large, max 5MB after compression");
    }

    const ext = mimeType === "image/png" ? "png" : "jpg";
    const now = Date.now();
    const token = crypto.randomBytes(8).toString("hex");
    const cloudPath = `yanye-home/${safePathPart(coupleId)}/${moduleName}/${safePathPart(userId)}/${now}-${token}.${ext}`;

    const uploadResult = await app.uploadFile({
      cloudPath,
      fileContent
    });
    const fileID = uploadResult.fileID || uploadResult.fileId || uploadResult.fileid;
    if (!fileID) {
      throw new Error("upload succeeded but no fileID returned");
    }

    const tempUrl = await resolveTempUrl(fileID);

    return jsonResponse(200, {
      ok: true,
      fileID,
      url: tempUrl || fileID,
      cloudPath,
      mimeType,
      size: fileContent.length
    });
  } catch (error) {
    return jsonResponse(400, {
      ok: false,
      error: error.message || "uploadImage failed"
    });
  }
};

async function resolveTempUrl(fileID) {
  const result = await app.getTempFileURL({
    fileList: [fileID]
  });
  const item = (result.fileList || [])[0] || {};
  return item.tempFileURL || item.download_url || item.url || "";
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

function safePathPart(value) {
  return String(value || "unknown")
    .trim()
    .replace(/[^a-zA-Z0-9_-]/g, "-")
    .replace(/-+/g, "-")
    .replace(/^-|-$/g, "")
    .slice(0, 64) || "unknown";
}

function normalizeMimeType(value) {
  return value === "image/png" ? "image/png" : "image/jpeg";
}

function jsonResponse(statusCode, body) {
  return {
    statusCode,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "access-control-allow-origin": "*",
      "access-control-allow-methods": "POST,OPTIONS",
      "access-control-allow-headers": "content-type"
    },
    body: JSON.stringify(body)
  };
}
