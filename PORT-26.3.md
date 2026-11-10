# Experimental Fabric 26.3 port — alpha.2

This branch targets Minecraft Java 26.3 and Java 25. The historical 26.2 alpha.2
checkpoint is `e5708a2`; the separate `codex/fabric-26.2-port` branch
contains the 26.2 performance backport (latest candidate: alpha.4). These are unofficial test builds.

## Build

Run `./gradlew :fabric:build` with a Java 25 JDK (`gradlew.bat` on Windows).
The production artifact is `fabric/build/libs/openysm-fabric-2.6.6.0+26.3-alpha.2.jar`.
Fabric Loader 0.20.1, Fabric API 0.166.0+26.3, Architectury 22.0.5 and Cardinal
Components 8.1.0 are used. Required runtime libraries are included in the JAR.
Install exactly one matching mod version on the server and every client.

## alpha.2 release-candidate regression results

The Java model backend is now the default: legacy YSM native libraries are not
loaded or extracted, and no `-DOYSM_DISABLE_SMID=1` argument is required. Declare
bundled Fabric API, Architectury, Forge Config API Port and Cardinal Components
as required runtime dependencies so Fabric does not silently omit them.

The exact production JAR (`SHA-256 bd801cecddf07d8f3d7f47a878b82aa98838701b35791d6375e609bfd62c0581`) passed the dedicated-server
matrix with two independent clients, 8 GiB heaps per client and 4 GiB on the server.
Neither client had custom models preinstalled. Both empty-cache catalogs loaded
all 1,146 entries and 1,116 unique encrypted files. Remote movement/swings,
extra0-extra7, bow/shield use, model/texture changes, previews/search input,
tracking-range return, asynchronous reload responsiveness and overlap rejection
were exercised. Final clients and server exited with code 0.

| Local observation | Alice | Bob |
| --- | ---: | ---: |
| Initial empty-cache complete catalog | 28 s | 28 s |
| Reconnect complete catalog (Alice cached, Bob empty) | 43 s | 25 s |

Bob's two priority avatars were ready within **1122 ms** of model synchronization
on the later empty-cache join. These are loopback results with warm server and
filesystem caches, not process-startup timings or Internet guarantees. Full
catalog timings use second-resolution logs; avatar probes use milliseconds.
The full catalog remains resident after loading.

The corrected Windows launcher preserves Mojang's required 26.3 JVM options,
including `-XX:StackShadowPages=32`. The same stack margin is also used explicitly
for the 26.2 Windows regression configuration. A prior 26.3 native JVM failure
was reproduced with an empty mods folder; ten baseline client startups passed
after correcting the stack margin. This does not patch HotSpot itself or prove
that every historical native crash had the same cause. See
[launcher diagnosis](verification/LAUNCHING.md) and
[machine-readable results](verification/RESULTS-26.3.json).

Focused checks passed, including the Java backend without an SMID flag, actual
Minecraft mixin target bindings, concurrent crypto/cache roundtrips, packet
bounds, callback-based transfer ordering and immutable render snapshots.

## 26.3 API changes

- Migrate rendering imports to RenderPearl and preserve UV3 in deferred vertices.
  Keep vanilla submit collectors as the rendering path; remove unreachable legacy
  native geometry/GL shader classes already disabled in the 26.2 port.
- Submit held/head items through ItemModelResolver and ItemStackRenderState.
- Adapt hand swings to SwingDescription and the current vanilla broadcast API.
  Restart animation controllers when a new swing description arrives, including
  repeated attacks whose first tick was not rendered by the observer.
- Use SDL keyboard/mouse constants in controls and GUI handling. Translate input
  codes for existing model expressions that expect legacy GLFW numeric values.
- Update URI, window refresh-rate and renderer-context APIs.

## Empty-cache loading

The previous sender polled write completion every 5 ms for every ~30 KiB chunk.
A catalog with almost 900 MB of encrypted unique contents therefore spent much
of its transfer waiting. The sender now waits for the write callback, with one
outstanding packet per producer and a bounded timeout. Failed writes stop the
producer rather than retransmitting packets that might already have been sent.

The manifest puts the joining player's model, nearby players' models and the
configured fallback first. The client preserves that order when requesting unique
content hashes. Other models continue loading asynchronously. The complete
catalog remains available; model assets and the wire format are unchanged.

A filled cache is parsed in an ordered single-worker background lane. It avoids
submitting the entire cached catalog to several parsers at world startup. Fresh
downloads retain the existing heap-aware parallel parser. Per-model diagnostics
are debug logs rather than thousands of console messages.

## Historical alpha.1 measured test scope

Tests use an independent Windows dedicated server bound to `127.0.0.1:25577`, two
independent clients with 8 GiB heaps, Java 25.0.5+8, OpenGL and an RTX 5090. The
server heap is 4 GiB. Offline identities are test fixtures, not public hosting.

The unchanged source catalog at `1749b46467acea11694891c01ede24914b22dc6c` contains
1,114 files. With built-ins and regression fixtures the test advertises 1,146
entries, representing 1,116 encrypted cache files totaling 897,623,061 bytes.
Neither client has custom models preinstalled. A cold client cache means that
its encrypted model-cache directory is absent; the server's binary catalog and
the operating system's filesystem cache are warm in both comparisons.

| Local 26.3 measurement | Previous sender | Callback sender + avatar priority |
| --- | ---: | ---: |
| Complete cold client catalog | 178 s, both clients | 21–23 s; repeat 22 s |
| Selected Ame/KAngel ready in render registry | Not separately measured | About 1 s |
| Warm server metadata reload | — | 1,268 ms, 1,146 cache hits |

Full-catalog times use second-resolution log timestamps. Ready probes have
millisecond timestamps but their sync-start log has second resolution; do not
interpret the avatar measurement as a precise sub-second benchmark. These are
loopback results, not an Internet latency or bandwidth guarantee. Every catalog
model is still assembled; this is priority loading, not visible-only residency.

Selected remote checks used Ame and KAngel: ordinary movement/crouch/jump,
repeated hand swings/mining, held bow/shield use, boat riding, swimming, creative
flight, extra0–extra7, alternative textures, a late join, tracking-range return
and cached resync. GUI model search/selection was also exercised. Not every model
or animation in the catalog was visually inspected. See `verification/README.md`
for focused protocol, render, input, transfer-order and cache-roundtrip checks.

## Limits and publication status

Early Windows attempts encountered native process crashes (`0xc0000005`).
The ordered cache parser improved startup behavior, but was not established as
a fix for native crashes. Later isolation reproduced a JVM failure without YSM
and identified an omitted Mojang stack-margin argument in the test launcher;
see the alpha.2 results and `verification/LAUNCHING.md`. Removing NMT, limiting
the compiler or changing the JDK path did not resolve the earlier attempts.

Vulkan, Linux hosts, proxies, authenticated public hosting, shaders and broad
third-party mod compatibility remain unverified. Native geometry acceleration
is disabled. The previous 26.2 memory measurements are historical and are not
claimed as a new 26.3 memory improvement.

The original README points to `OpenYSMDev/OpenYSM`, which was unavailable with a
[GitHub DMCA notice](https://github.com/github/dmca/blob/master/2026/06/2026-06-03-ysm.md).
The snapshot includes a MIT file, but this work does not resolve that publication
dispute or represent an official Yes Steve Model release. No new model assets are
included in the source proposal.
