# Java 中国象棋 (Xiangqi) 项目 - 网络版 V1

这是一个使用 Java 语言从零开始构建的、支持网络对战的中国象棋项目。

本项目采用 **客户端/服务器 (Client/Server)** 架构，实现了核心游戏逻辑、基础 AI (Minimax)，并通过 WebSocket 实现了基础的双人在线对战功能。

## 📍 当前状态 (Client/Server V1)

* **`xiangqi-core` (核心引擎):** 包含所有象棋规则、状态判断（将军/将死/逼和）及基础 Minimax AI 的库。
* **`xiangqi-server` (服务器):** 使用 Spring Boot 和 WebSocket 构建的后端服务。
    * 管理单个游戏实例。
    * 处理 WebSocket 连接，为先连接者分配红方，后连接者分配黑方。
    * 接收客户端通过 JSON 发送的走法 (`SendMoveMessage`)。
    * 调用核心引擎验证并执行走法。
    * 将更新后的游戏状态 (`GameStateMessage`) 通过 JSON 广播给所有连接的客户端。
* **`xiangqi-client` (客户端):** 使用 JavaFX 构建的图形界面应用。
    * 连接到 WebSocket 服务器。
    * 接收服务器分配的角色 (`AssignPlayerMessage`)。
    * 接收并解析服务器广播的游戏状态 (`GameStateMessage`)，实时更新棋盘显示。
    * 支持鼠标点击棋子进行选中和移动。
    * 将玩家的走法打包成 JSON (`SendMoveMessage`) 发送给服务器。
    * 根据服务器状态禁用/启用本地操作，实现回合控制。
    * 带有棋子移动动画（网络版暂未完全重构）。

### 已实现功能

* **核心引擎:**
    * [✓] 所有棋子规则 (`车`, `马`, `炮`, `将`, `士`, `象`, `兵`)
    * [✓] 游戏状态判断 (将军, 将死, 逼和)
    * [✓] 合法走法生成 (包含防自杀逻辑)
    * [✓] 基础 Minimax AI (基于子力评估, 深度 3)
    * [✓] JSON 消息类定义 (`MessageType`, `BaseMessage`, `SendMoveMessage`, `GameStateMessage`, `ErrorMessage`, `AssignPlayerMessage`)
* **服务器:**
    * [✓] Spring Boot 基础框架
    * [✓] WebSocket 端点 (`/game`) 及连接管理 (简化版)
    * [✓] JSON 消息解析 (Jackson)
    * [✓] 调用核心引擎处理走法
    * [✓] 游戏状态广播
    * [✓] 玩家角色分配 (简化版)
* **客户端 (JavaFX):**
    * [✓] 绘制棋盘和棋子 (圆形+文字)
    * [✓] 棋子精确显示在交叉点
    * [✓] WebSocket 客户端连接 (`ServerConnector`)
    * [✓] JSON 消息发送与接收 (Jackson)
    * [✓] 根据服务器状态更新 GUI
    * [✓] 鼠标点击选中与移动请求发送
    * [✓] 基本的回合控制 (界面禁用/启用)
    * [✓] 基础移动动画 (待网络版重构)

---

## 🛠️ 技术栈

* **语言：** Java 17
* **构建：** Maven (多模块: `parent`, `core`, `client`, `server`)
* **后端：** Spring Boot (Web, WebSocket)
* **前端 GUI：** JavaFX 17+
* **通信协议：** WebSocket + JSON (Jackson)
* **版本控制：** Git & GitHub

---

## 🚀 如何运行 (网络对战)

1.  **克隆/下载本项目**
2.  **构建所有模块:**
    * 在项目根目录 (`xiangQi/`) 运行：
        ```bash
        mvn clean install
        ```
3.  **启动服务器:**
    * 在项目根目录运行：
        ```bash
        mvn -pl xiangqi-server spring-boot:run
        ```
    * 服务器将在 `localhost:8080` 启动并监听 `/game` 的 WebSocket 连接。
4.  **启动第一个客户端 (将扮演红方):**
    * 打开**第一个** WSL 终端窗口。
    * 确保 X Server (如 VcXsrv) 正在运行并配置正确。
    * 运行：
        ```bash
        export DISPLAY=:0 # 或 localhost:0.0
        mvn -pl xiangqi-client javafx:run
        ```
    * 客户端窗口标题应显示 `... - RED`。
5.  **启动第二个客户端 (将扮演黑方):**
    * 打开**第二个** WSL 终端窗口。
    * 运行：
        ```bash
        export DISPLAY=:0 # 或 localhost:0.0
        mvn -pl xiangqi-client javafx:run
        ```
    * 客户端窗口标题应显示 `... - BLACK`。
6.  **开始对战!**
    * 在红方客户端窗口点击棋子进行移动。
    * 棋盘状态将在两个客户端同步更新。
    * 轮到黑方时，红方客户端的操作将被禁用，反之亦然。

---

## 🗺️ 未来项目蓝图 (Roadmap)

* **[ ✓ ] 阶段一：核心游戏逻辑**
* **[ ✓ ] 阶段二：人工智能 (AI)** (基础 Minimax)
* **[ ✓ ] 阶段三：前端 UI (JavaFX)** (基础 GUI 与交互)
* **[ ✓ ] 阶段四：后端服务 (Spring Boot)** (基础 WebSocket 网络对战)
* **[ 进行中 ] 完善与扩展:**
    * **网络:** 匹配系统, 断线重连, 聊天
    * **人机:** 在服务器集成 AI 对战
    * **GUI:** 棋子图片, 美化提示, 效果动画 (吃子/将军/胜利)
    * **AI:** Alpha-Beta 剪枝, 改进评估函数
    * **3D:** (远期) 探索 3D 界面
* **[ 待办 ] 阶段五：打包与部署** (jpackage, 云部署)