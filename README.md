## Experimental Fabric 26.2 — alpha.4

This branch builds Minecraft **26.2 / Java 25**. See [PORT-26.2.md](PORT-26.2.md)
for the exact dedicated-server test scope, performance measurements and limits.
Two independent clients were tested against a separate server with the complete
model catalog. These are unofficial alpha builds. On Windows, preserve the
verified Java launch options in [verification/LAUNCHING.md](verification/LAUNCHING.md).

本分支构建 Minecraft 26.2，已完成独立服务器与两个客户端的完整模型目录验证。
这是非官方实验版；测试范围、Windows 启动参数与限制请参阅以上文档。

Other maintained ports: [26.1 / 26.1.1 / 26.1.2](https://github.com/IsSlashy/OpenYSM-Updated/tree/codex/fabric-26.1-port),
[26.2](https://github.com/IsSlashy/OpenYSM-Updated/tree/codex/fabric-26.2-port),
[26.3](https://github.com/IsSlashy/OpenYSM-Updated/tree/codex/fabric-26.3-port).

上游项目已迁移至 https://github.com/OpenYSMDev/OpenYSM

增加了对高版本ysm加密模型的加载能力
---

<div align="center">
  <img src="images/banner2.png" alt="logo"/>
  <h1>OpenYSM-Updated</h1>
  <p>OpenYSM 的 fabric 高版本移植</p>
</div>

___

## 注意
### 该项目仅作为高版本移植可行性验证，_**对可用性没有任何保证，使用过程中可能会出现大量bug**_，如果发现问题请提交 issue。
### 本分支的 26.2 已验证独立服务器、两个客户端及模型同步；其他环境与第三方模组兼容性尚未保证。

## 版本支持

| 版本      | 支持状态                                        |
|---------|---------------------------------------------|
| 1.20.1  | ✅ (原生)                                      |
| 1.21.1  | ✅ (见 commit history)                        |
| 1.21.4  | ✅ (见 commit history, 有的类忘了交了可能无法编译，从后面的提交里找 |
| 1.21.8  | ✅ (见 commit history)                        |
| 1.21.9  | ✅ (见 commit history)                        |
| 1.21.10 | ✅ (见 commit history)                        |
| 1.21.11 | ✅                                           |
| 26.2 | 🧪 本分支的非官方实验移植；详见 PORT-26.2.md |
| 26.1.x | 🧪 单独分支支持 26.1 / 26.1.1 / 26.1.2 |

## 已知问题
请参阅 PORT-26.2.md 中的已验证行为与限制；未逐一目视检查所有模型和动画。
