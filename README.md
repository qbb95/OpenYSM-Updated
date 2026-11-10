上游项目已迁移至 https://github.com/OpenYSMDev/OpenYSM

增加了对高版本ysm加密模型的加载能力
---

<div align="center">
  <img src="images/banner2.png" alt="logo"/>
  <h1>OpenYSM-Updated</h1>
  <p>OpenYSM 的 fabric 高版本移植</p>
</div>

___

## Experimental Fabric 26.1.x builds

Use the JAR matching **26.1**, **26.1.1** or **26.1.2** on the server and every
client. Dependencies are bundled. These unofficial alpha builds use the Java
model backend and need no special launcher workaround. Models are unchanged.

[Build/test details](PORT-26.1.md) · [Unofficial test Releases](https://github.com/IsSlashy/OpenYSM-Updated/releases)

## 注意
### 该项目仅作为高版本移植可行性验证，_**对可用性没有任何保证，使用过程中可能会出现大量bug**_，如果发现问题请提交 issue。
### 本分支已在 26.1、26.1.1、26.1.2 的 Windows 本地独立服务器和两个客户端上测试模型同步及常用远端动画。仍为非官方 alpha，详细测试范围和限制见 [PORT-26.1.md](PORT-26.1.md)。

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
| 26.1 | ✅ Fabric 非官方 alpha.1；独立服务器 + 双客户端测试 |
| 26.1.1 | ✅ Fabric 非官方 alpha.1；独立服务器 + 双客户端测试 |
| 26.1.2 | ✅ Fabric 非官方 alpha.1；独立服务器 + 双客户端测试 |

本分支仅构建 26.1、26.1.1、26.1.2；更早版本请查阅上游提交历史。

## 已知问题
并非所有模型动画、第三方模组集成或平台都已重新验证；详见 [PORT-26.1.md](PORT-26.1.md)。
