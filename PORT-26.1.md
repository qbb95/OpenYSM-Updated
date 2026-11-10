# Experimental Fabric ports for Minecraft 26.1, 26.1.1 and 26.1.2

These are unofficial alpha.1 builds based on `qbb95/OpenYSM-Updated` at
`bcfa3c355e6252618e68d7c61ef8ff03455ab64c`, retaining the rendering, archive-reader,
multiplayer and allocation fixes from the 26.2 checkpoint and its loading backport.
The separate 26.2 alpha.3 checkpoint is `6375ea8`. No 26.4 port is included.

## Build and install

Java 25 and Fabric Loader 0.20.1 are used. Build each pinned variant and run its
focused checks with:

```sh
python verification/build_matrix.py
# Include the optional metadata/animation-allocation checks with your own fixture:
python verification/build_matrix.py --legacy-model /path/to/legacy-model.ysm
```

The wrapper uses Gradle 9.5 and Loom no-remap 1.17.493. The checked artifacts are
copied to `fabric/build/verified/`. `verification/versions.json` records the
version-specific dependency coordinates.

| Minecraft | Fabric API | Architectury | Artifact suffix |
| --- | --- | --- | --- |
| 26.1 | 0.145.1+26.1 | 20.0.4 | `2.6.6.0+26.1-alpha.1.jar` |
| 26.1.1 | 0.145.4+26.1.1 | 20.0.4 | `2.6.6.0+26.1.1-alpha.1.jar` |
| 26.1.2 | 0.155.3+26.1.2 | 20.1.17 | `2.6.6.0+26.1.2-alpha.1.jar` |

Cardinal Components 8.0.1 and Forge Config API Port 26.1.4 are bundled with all
three variants. Forge Config API Port 26.1.5 requires Fabric API >=0.149.0,
which is newer than the available API builds for the exact 26.1/26.1.1 games.
Selecting it caused an optional nested library to be skipped at startup.
The compatible version is now pinned, and required runtime libraries are
explicitly declared so resolution failures are reported before game startup.
Iris is compile-only; shader compatibility is not established by these tests.

Use the JAR for the exact game version on both the server and every client.
The supported model parser and render path are Java/vanilla submit collectors.
Legacy native acceleration is deliberately disabled in the mod itself; no
`OYSM_DISABLE_SMID` launcher argument or custom native library is required.

Place original model files/category directories under
`config/yes_steve_model/custom/` on the server. Extract GitHub's repository ZIP
first: that outer archive is not an individual YSM model. Supported ZIP model
archives can remain zipped. Clients download the catalog from the server.

## Port changes

- Restore 26.1.x screen/overlay, camera, HUD visibility, color and texture-format
  APIs. Use the exact game-version constraint in each produced JAR.
- Adapt the world-render hook to `LevelRenderer.renderLevel` and the GUI preview
  hook to the two-argument `GuiEntityRenderer.renderToTexture` method. Preserve
  vanilla deferred collectors and immutable transformed vertex snapshots.
- Use 26.1.x entity/block identifiers for riding and sleeping preview scenery.
  Remove unreachable native geometry/shader classes already disabled in the port.
- Keep authored model scales, ZIP/wrapper/legacy-encoding support, content-alias
  deduplication, checked packet/chunk bounds, connection-pinned transfers, joining
  and tracking synchronization, and asynchronous server reloads.
- Prioritize active player models (including simultaneous joiners), then the
  configured fallback, before the remaining catalog. Wait for packet-write
  callbacks rather than polling every 5 ms per chunk. Keep one outstanding write
  per producer with a bounded timeout and stop on write failure.
- Keep fresh downloads parallel with a heap-aware worker count. Parse a filled
  cache in a separate ordered background lane to avoid a startup allocation burst.

## Verification

Each production JAR is tested on its own isolated Windows dedicated server and
two independent clients, with no custom models installed on the clients. Tests
use Java 25.0.5+8, an RTX 5090/OpenGL, 8 GiB client heap limits and a 4 GiB server
heap limit. Offline test identities are restricted to loopback.

The unchanged catalog at `1749b46467acea11694891c01ede24914b22dc6c` contains
1,114 original files: 906 `.ysm` files and 208 ZIP models. Including 27 built-ins
and five regression fixtures, the server advertises 1,146 entries and clients
download 1,116 unique contents. Source assets are not converted or rewritten.

Checks cover complete downloads from empty client caches, fresh-process cached
loading, a reconnect with one empty and one populated cache, remote Ame/KAngel
models, repeated swings, walking/running/crouching/jumping, bow/shield use,
extra0–extra7, client-originated model changes, alternative textures, GUI previews
and search, leaving/returning to tracking range, and server reload responsiveness.
The selected clients and server are shut down cleanly after testing.

Avatar readiness is measured when its assembled model enters the render registry,
using test-only millisecond timestamps. A complete initial catalog took about
27–28 seconds in the initial runs. With Ame/KAngel already selected on the server,
the later empty-cache client had both avatars ready in 292 ms (26.1),
309 ms (26.1.1) and 276 ms (26.1.2). These are local observations with a warm server binary
cache and filesystem cache, not Internet guarantees. The complete catalog is
still resident after loading; priority loading is not visible-only residency.
Full-cache startup uses a lower-concurrency lane and may finish after a cold
network transfer, while the selected avatars are displayed first.

| Local measurement | 26.1 | 26.1.1 | 26.1.2 |
| --- | ---: | ---: | ---: |
| First complete catalog, both clients with empty model caches | 28 s | 28 s | 27 s |
| Later empty-cache client, complete catalog | 24 s | 24 s | 23 s |
| Fresh process with a populated disk cache, complete background catalog | 41 s | 41 s | 40 s |

Full-catalog durations use second-resolution log timestamps. Avatar readiness
uses the millisecond probes; neither measurement includes game-process startup.

The focused checks cover malformed lengths, deferred render snapshots/scopes,
numeric/cache equivalence and invalidation, transfer ordering/write completion,
18 concurrent download/cache roundtrips, Java backend startup without a special
VM flag, and 25 string-bound mixin method/field targets for each game version.
See `verification/README.md`. Early dependency and render-hook startup failures
were reproduced and corrected before the final test sessions.

## Limits and publication status

Not every catalog model or animation was visually inspected. Sleeping, climbing,
swimming, riding, elytra/creative flight and third-party animation integrations
have not all been revalidated on this branch. Linux, authenticated public hosting,
proxies, shaders and broad compatibility with other mods remain unverified.

The upstream README points to `OpenYSMDev/OpenYSM`, which was unavailable with a
[GitHub DMCA notice](https://github.com/github/dmca/blob/master/2026/06/2026-06-03-ysm.md).
The snapshot includes a MIT file; this port does not resolve that publication
dispute or represent an official Yes Steve Model release. The source proposal
includes no new model assets or binary dependencies. Test helpers are separate
modules and are not included in the distributed mod JARs.
