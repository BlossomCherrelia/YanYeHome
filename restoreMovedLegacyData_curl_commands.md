# restoreMovedLegacyData curl commands

复制下面任意一种方式到终端执行。

## 方式一：一行版

这一整行必须一次性复制，不要手动换行。

```bash
curl -s -X POST "https://yanyehome-d9grtwqrlc809509f-1434875599.ap-shanghai.app.tcloudbase.com/restoreMovedLegacyData" -H "content-type: application/json" -d '{"userId":"user_3910e5e27a3dff05e","fromCoupleId":"space_a1c0130793670450","targetCoupleId":"yanyehome","dryRun":true}'
```

## 方式二：变量版

可以分两次复制执行。

第一步：

```bash
URL="https://yanyehome-d9grtwqrlc809509f-1434875599.ap-shanghai.app.tcloudbase.com/restoreMovedLegacyData"
```

第二步：

```bash
curl -s -X POST "$URL" -H "content-type: application/json" -d '{"userId":"user_3910e5e27a3dff05e","fromCoupleId":"space_a1c0130793670450","targetCoupleId":"yanyehome","dryRun":true}'
```

## 正式执行版

只有 dryRun 返回数量确认合理后，才执行这个。

```bash
curl -s -X POST "$URL" -H "content-type: application/json" -d '{"userId":"user_3910e5e27a3dff05e","fromCoupleId":"space_a1c0130793670450","targetCoupleId":"yanyehome","dryRun":false}'
```
