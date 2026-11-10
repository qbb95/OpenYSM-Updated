# Focused port verification

This branch builds separate Minecraft 26.1, 26.1.1 and 26.1.2 artifacts. With a
Java 25 `JAVA_HOME`, run the pinned build/check matrix:

```sh
python verification/build_matrix.py
```

Use `--games 26.1.2` for one variant and `--legacy-model /path/to/model.ysm` for
the optional legacy fixture checks. Each variant is checked immediately after
compilation against its own resolved Minecraft classes. Only then is its JAR
copied into `fabric/build/verified/`. Iris is a compile-only optional integration;
the build matrix does not establish shader compatibility.

Prerequisites: Java 25 JDK, Python 3, and the port's resolved Gradle dependencies.
From the repository root, with `JAVA_HOME` pointing to that JDK:

```sh
./gradlew :fabric:build
./gradlew -I verification/classpath.gradle :fabric:writeVerificationClasspath
python verification/run_smoke.py
```

On Windows use `gradlew.bat`. The classpath and test outputs are generated under
`fabric/build/verification/`; no installed Minecraft profile is changed.

- `WireBoundsSmoke` checks valid UTF-8/binary roundtrips and six malformed lengths.
- `ModelTransferSmoke` checks player-model priority, complete catalog ordering,
  successful/failed asynchronous writes, timeouts and interruption.
- `ModelCacheSmoke` compares the server payload, initial download and subsequent
  cache read over 18 concurrent roundtrips.
- `ModelBackendSmoke` checks that the supported Java backend works without the
  old launcher workaround or a native library.
- `MixinTargetsSmoke` checks string-bound methods/fields against the selected
  game classes. A live launch remains necessary to validate injection anchors,
  callback signatures, GPU rendering and actual multiplayer behavior.
- `RenderSmoke` checks immutable transformed vertex snapshots, single application
  of transforms, repeated flush behavior, nested/thread-local scopes, and scope
  recovery after collector failure. The collector is a test double; no GPU or
  in-game rendering is exercised by this check.
- `OptimizationSmoke` checks float edge cases, independent reusable animation
  vectors, metadata/property/statistics/animation/texture roundtrips, alias and
  authorization identity, and invalidation of changed or damaged cache inputs.
  Supply a local legacy v1/v2 `.ysm` model to run it:

```sh
python verification/run_smoke.py --legacy-model /path/to/legacy-model.ysm
```

No model fixture is redistributed here. The optional fixture must be a model you
are allowed to use; the verification run used the original Ame legacy model.
`--java-home` and `--classpath-file` can override the default JDK and classpath.

See [PORT-26.1.md](../PORT-26.1.md) for this branch's production-JAR dedicated-server
test, two-client animation checks, measured loading/reload behavior, and limits.
These focused checks do not establish Linux hosting, shader/mod compatibility,
authenticated multiplayer, or visual correctness of every catalog entry.
