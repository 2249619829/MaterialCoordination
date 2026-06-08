# 物资协同平台前端原型

这是一个零依赖企业级 SPA 原型，覆盖登录、用户工作台、推拉结合抢单大厅、我的任务和用户中心。

## 运行

```bash
cd web-frontend
npm run start
```

浏览器打开：

```text
http://localhost:5173
```

## 默认演示账号

任意用户名和密码都可以登录。推荐：

```text
用户类型：供应商
用户名：demo_supplier
密码：任意
```

## 后端接入点

当前前端以本地 mock 为主，已按后端接口模型组织登录态：

- `POST /auth/login`
- `GET /auth/me`
- `DELETE /auth/logout`

后续 Docker 和网关环境稳定后，可以把 `assets/app.js` 中的 `apiBase` 切到 `http://localhost:8080` 并替换 `mockLogin`。
