# Focused port verification

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

See [PORT-26.2.md](../PORT-26.2.md) for the separate production-JAR dedicated-server
test, two-client animation checks, measured loading/reload behavior, and limits.
These focused checks do not establish Linux hosting, shader/mod compatibility,
authenticated multiplayer, or visual correctness of every catalog entry.

- `ModelBackendSmoke` verifies Java backend initialization without an SMID flag.
- `MixinTargetsSmoke` resolves the configured mixin targets against this exact
  Minecraft runtime rather than relying on stale method names.
- `launcher_jvm.py` resolves required JVM options from exact Mojang metadata;
  standalone arguments and OS rules are retained. See [LAUNCHING.md](LAUNCHING.md)
  for the Windows startup regression, its baseline without YSM and the corrected
  launch configuration. This helper is not included in the production mod JAR.
- `ModelTransferSmoke` covers prioritized manifest ordering and packet write
  completion, errors, timeout, interruption and one outstanding producer write.
- `ModelCacheSmoke` verifies 18 concurrent encrypted download/cache roundtrips.
