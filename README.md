# 校园组队通 (Campus Team)

基于 **uniapp 微信小程序 + Spring Boot + MySQL 8.0.33 + Redis + Vue3/Element Plus 管理后台** 的校园活动组队平台。

## 项目结构

```
xyzd-Project/
├── sql/init.sql                 # 数据库建表及初始数据
├── src/main/java/com/campus/team/   # Spring Boot 后端（五层架构）
├── miniprogram/                 # uniapp 微信小程序
└── admin-web/                   # Vue3 + Element Plus 管理后台
```

## 架构分层

| 层级 | 目录/模块 | 职责 |
|------|-----------|------|
| 展现层 | `miniprogram/`、`admin-web/` | 页面渲染、交互、微信能力 |
| 应用接口层 | `api/`、`security/` | RESTful 接口、鉴权、参数校验 |
| 核心业务层 | `core/` | 活动、报名、私信、打卡、评价等业务规则 |
| 基础设施层 | `infrastructure/`、`config/` | Redis、微信、敏感词、定时任务 |
| 数据层 | `data/entity/`、`data/mapper/` | MyBatis-Plus CRUD |

## 快速启动

### 1. 数据库

```bash
mysql -u root -p < sql/init.sql
```

修改 `src/main/resources/application.yml` 中的数据库与 Redis 连接信息。

### 2. 后端

```bash
mvn spring-boot:run
```

- API 地址：`http://localhost:8080/api`
- 微信登录默认开启 Mock 模式（`campus.wechat.mock-enabled: true`），无需真实 AppID

### 3. 管理后台

```bash
cd admin-web
npm install
npm run dev
```

- 地址：`http://localhost:5173`
- 默认账号：`admin` / `admin123`（校园管理员）
- 运维账号：`ops` / `admin123`

### 4. 小程序

使用 HBuilderX 或 CLI 打开 `miniprogram` 目录，运行到微信开发者工具。

修改 `miniprogram/utils/api.js` 中的 `BASE_URL` 为后端地址。

## 功能模块

- **用户账号**：微信登录、资料完善（敏感词过滤）、隐私设置、RBAC 角色
- **活动发布**：创建/编辑/下架、分类检索、关键词搜索、多条件筛选
- **报名组队**：报名/取消、审核、满员校验、成员移除、招募状态
- **私信沟通**：同活动参与者私聊、未读提醒、频率限制、敏感词过滤
- **打卡评价**：进行中打卡防重、发起人查看统计、结束后 1-5 星评价

## 权限角色

| 角色 | 说明 |
|------|------|
| USER | 普通师生：浏览、报名、私信、打卡、评价 |
| CREATOR | 活动发起者：继承 USER + 管理自己发布的活动 |
| CAMPUS_ADMIN | 校园管理员：内容监管、用户管理、投诉处理、数据统计 |
| OPS_ADMIN | 运维管理员：系统运维（无业务编辑权限） |

## 核心 API

| 模块 | 路径前缀 |
|------|----------|
| 认证 | `/auth/*` |
| 活动 | `/activities/*`、`/categories` |
| 报名 | `/registrations/*` |
| 私信 | `/chat/*` |
| 打卡 | `/checkin/*` |
| 评价 | `/reviews/*` |
| 投诉 | `/complaints` |
| 管理 | `/admin/*` |

## 业务流程

1. 微信登录 → 新用户强制完善资料 → 获取 Token
2. 首页检索活动（自动过滤已下架）→ 查看详情
3. 登录用户发布活动（敏感词过滤）→ 默认「招募中」
4. 报名 → 待审核 → 发起人审核 → 人数同步更新
5. 同活动审核通过用户可私信（频率限制 + 敏感词）
6. 活动进行中打卡 → 结束后提交评价

## 注意事项

- 管理后台默认限制内网 IP 访问（可在 `application.yml` 的 `campus.admin.allowed-ips` 配置）
- 生产环境请关闭微信 Mock 模式并配置真实 `WECHAT_APP_ID`、`WECHAT_APP_SECRET`
- 生产环境请更换 JWT Secret
- 图片本地上传：存储于 `./uploads/`，访问路径 `/api/files/**`，小程序通过 `POST /files/upload` 上传
