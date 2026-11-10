# Windows startup regression and launcher requirements

The isolated 26.3 test launcher initially copied a hand-written JVM argument
list from the earlier port. It omitted the standalone
`-XX:StackShadowPages=32` argument and `--add-exports
java.base/jdk.internal.misc=ALL-UNNAMED` in Mojang's 26.3 version metadata.
That launcher omission has been corrected; `launcher_jvm.py` resolves required
JVM arguments from the exact version metadata, including standalone options,
OS rules, and the argument following `--add-exports`.

The native failure was reproduced with Fabric Loader and an **empty mods
directory**, before joining a server or downloading any custom model. Windows
reported `0xc0000005`, `jvm.dll+0xa7dc97` in Temurin 25.0.5+8. The matching official
debug symbols resolve that instruction to `__chkstk+0x37` on `Worker-Main-24`.
The dump's stack pointer was below the TEB StackLimit while substantial reserved
stack remained. This is consistent with the Windows stack-commit defect described
in [OpenJDK JDK-8394484](https://github.com/openjdk/jdk/pull/33729), whose fix was
still under review when checked. A mod code change does not fix HotSpot itself.

The reproduction without the mod had one crash among six launched clients.
The corrected baseline startup series uses the same Java binaries, graphics
backend, heap sizes and libraries, changing the stack margin to 32 pages.
Detailed counts and subsequent full multiplayer runs are in `RESULTS-26.2.json`
and `RESULTS-26.3.json`. Startup isolation processes are deliberately terminated
after their observation interval; clean exits are assessed separately by the
complete dedicated-server scenarios.

On Windows/HotSpot, the verified client launch configuration uses:

```
-XX:StackShadowPages=32
```

Minecraft 26.3 already specifies this in its official launch metadata. A custom
launcher must preserve it. The 26.2 Windows regression configuration also uses
this setting as a precaution; it is an explicit addition to 26.2, whose official
metadata does not contain it. This is a Java launch setting, not a model format
change. The Gradle client runs set the same Windows stack margin. Java model
loading is the default, so `-DOYSM_DISABLE_SMID=1` is unnecessary.

Metadata sources:

- [Minecraft 26.2](https://piston-meta.mojang.com/v1/packages/09165f570b16394790c4e37c93b0918729615d17/26.2.json)
- [Minecraft 26.3](https://piston-meta.mojang.com/v1/packages/80608ff6753705b6a82e2cb86a5937d4d233abd4/26.3.json)

This does not prove that every historical native crash shared this cause. Earlier
26.2 GLFW crashes during inactive-window focusing/capture are distinct records.
No claim of universal crash freedom is made.
