

# legacyysm_unofficial_fork

## 说明

这是一个非官方的对旧版 Yes Steve Model 1.1.5 版本(即 <https://github.com/YesSteveModel/LgeacyYSM/> ) 源代码的 Fork 和修改版本，目前主要是进行了对 Neoforge 1.21.1 的改造和支持，并增添了新功能和逻辑。

由于现时已出现对 YSM 的新版加密格式（1.20.1forge-2.6.4）的公开解析手段，本项目自版本 1.1.9 起已集成 YSMParser 并实现了对新版模型格式的加载支持。

Minecraft 版本支持状态：

- 1.21.1-Neoforge： **✔客户端已支持 ✔服务端已支持**

模组下载地址：

- [GitHub Releases](https://github.com/baiyuanneko/legacyysm_unofficial_fork/releases)

由于本人对模组开发还比较萌新，向 1.21.1 的代码迁移工作和新功能的添加主要由 AI 完成，可能存在很多问题。目前测试功能正常的功能包括：

客户端：
- 模型显示
- 模型列表（Alt + Y）、模型更换、模型展示
- 轮盘动画（Alt + Z）

服务端：

- 分发服务端模型

此外，本项目实现了一些实验性的功能，可以视情况打开或使用：

- 多客户端模型同步。启用该特性后，不需要提前将模型文件传到服务端，而是直接在客户端内部就能将客户端本地模型直接应用，就会自动将客户端本地的模型传递给其他客户端，被其他客户端所看到。逻辑就是客户端上传自己的模型到服务端，然后由服务端实时分发给其他客户端。如需要此功能，请在服务端中的 `config/legacyysm_byn-server.toml` 配置中将 allowModelSync 设置为 true。注意本功能可能造成一定的安全风险，建议仅在信任的环境下启用。

```
[model_sync]
	allowModelSync = true
```


## Why this project

### 与官方版 YSM 的比较

由于官方版 YSM 在 v2 版本之后，将核心部分使用 C++ 重写，导致新版本无法直接跨平台，因而暂时还未支持 OS X 系统，而本项目基于的旧版可以支持跨平台使用，可以弥补官方版 YSM 无法在 OS X 系统使用的不足。此外，本项目可以添加一些方便使用的新特性和新逻辑。

## 与 OpenYSM 的关系

为了使本项目支持新版 YSM 加密格式，本项目部分参考和使用了 OpenYSM 的源代码，感谢其对开源社区的贡献！

## 与 YSMParser 的关系

本项目基于的旧版 YSM 源码本身不支持新版 YSM 的加密方式。自版本 1.1.9 起，本项目将集成 YSMParser 并实现对新版 YSM 加密模型格式的加载支持。也感谢 YSMParser 开发团队开源了解析 YSM 模型的库。本项目同样部分参考和使用了 YSMParser 的源代码和构建产物，感谢其对开源社区的贡献！

## 致谢

- [YSM 官方开发团队](https://github.com/YesSteveModel/LgeacyYSM/)开发了原始模组并开源了旧版v1.1.5版本源码
- [YSMParser](https://github.com/OpenYSM/YSMParser/)实现了对新版加密格式的解析并开源
- [OpenYSM](https://github.com/OpenYSM/OpenYSM)作为本项目对新版加密格式支持的参考实现
- [YSM-Report](https://github.com/OpenYSM/YSM-Report/)公开了YSM不同格式的分析细节

## 联系方式、贡献、漏洞提交、许可证等

欢迎 issue、PR 等等！

联系邮箱：```baiyang-lzy@outlook.com```。如有安全漏洞，建议是发往本邮箱而不是提交到 issue 区。

本项目采用和原项目一样的许可证：BSD 3-Clause License，也感谢 YSM 原开发团队的开源！自带模型等模型文件可能有不同的许可证，具体请看下方附的原始 README.md 文档。部分源代码（如com.ysm.parser包下的源码）来自 YSMParser，它们归属于 YSMParser 开发团队并使用 MIT 许可证；resources/natives/ysmparser 下的文件来自 YSMParser 的源代码及官方构建产物，它们归属于 YSMParser 开发团队并使用 MIT 许可证。本项目同样包含了对 OpenYSM 模型的参考实现或集成，它们归属于 OpenYSM 团队并遵循 MIT 许可证。

---

# 以下为官方版LegacyYSM的说明文档

---

# LegacyYSM 1.1.5 旧版 YSM 源码仓库

![Minecraft](https://img.shields.io/badge/Minecraft-Java%20Edition-brightgreen)
![License](https://img.shields.io/badge/License-BSD-blue)

## 说明

本仓库包含了 YesSteveModel (YSM) 1.1.5 hotfix2（2023年8月）及以下版本的完整源代码及 git 记录。

包含 1.16.5/1.18.2/1.19.2/1.20.1 Forge 版本的全部源码。

## 为什么开源？

我们决定将旧版 YSM 源码开源，主要基于以下几个原因：

### 1. 新版本的完全重置
新版 YSM 已经经过完全重新设计和开发，采用了全新的架构和加密方式。

旧版 YSM (1.1.5及以下) 对新版本没有任何技术影响，两者在代码层面已经完全独立。

### 2. 旧版加密的现状
此前社区已经出现了大量破解 YSM 1.1.5 及以下版本加密的工具和方法，旧版的加密机制实际上已经失去了保护作用。

同时，目前社区中的大部分新模型都已经采用了新版的加密方式，旧版加密已经毫无实际意义。

### 3. 支持开放的游戏氛围
我们注意到社区中有不少开发者制作了去除加密功能的 YSM 版本，这表明了社区对开放性的需求。

YSM 开发组一直非常支持开放、自由的游戏开发氛围，我们希望通过开源旧版源码，为其他开发者的二次开发和学习提供便利。

## 开源协议

### 源代码协议
本项目的源代码采用 **BSD 3-Clause License** 开放，您可以自由地使用、修改和分发代码，仅需要保留原始的版权声明。

详细的许可证条款请参见 [LICENSE](LICENSE) 文件。

### 模型资源协议
仓库中自带的模型文件采用不同的协议：

- **默认模型**: 采用 **CC0 (Creative Commons Zero)** 协议，完全开放，无任何使用限制
- **酒狐 (Wine Fox) 模型**: 采用 **CC BY-NC-SA 4.0** 协议，允许非商业使用，需要署名，并且衍生作品需要采用相同协议

请在使用相应模型时严格遵守对应的协议要求。

## 使用建议

我们鼓励开发者基于此源码进行二次开发，创造出更加开放、易用的模型加载工具。
