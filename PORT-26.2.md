# Experimental Fabric 26.2 port — alpha.4

Local port of snapshot `bcfa3c355e6252618e68d7c61ef8ff03455ab64c` from
`qbb95/OpenYSM-Updated`. Fabric only; Java 25, Minecraft 26.2, Fabric Loader 0.20.1.
Build with `./gradlew :fabric:build`. The production artifact is
`fabric/build/libs/openysm-fabric-2.6.6.0+26.2-alpha.4.jar`.

## alpha.4 release-candidate regression results

The Java model backend is now the default: legacy YSM native libraries are not
loaded or extracted, and no `-DOYSM_DISABLE_SMID=1` argument is required. Declare
bundled Fabric API, Architectury, Forge Config API Port and Cardinal Components
as required runtime dependencies so Fabric does not silently omit them.

The exact production JAR (`SHA-256 e08d26faa0176cbaafe191b9a2c79b073e7f6f70d357d435faf181f5b33c4d1f`) passed the dedicated-server
matrix with two independent clients, 8 GiB heaps per client and 4 GiB on the server.
Neither client had custom models preinstalled. Both empty-cache catalogs loaded
all 1,146 entries and 1,116 unique encrypted files. Remote movement/swings,
extra0-extra7, bow/shield use, model/texture changes, previews/search input,
tracking-range return, asynchronous reload responsiveness and overlap rejection
were exercised. Final clients and server exited with code 0.

| Local observation | Alice | Bob |
| --- | ---: | ---: |
| Initial empty-cache complete catalog | 27 s | 27 s |
| Reconnect complete catalog (Alice cached, Bob empty) | 41 s | 25 s |

Bob's two priority avatars were ready within **209 ms** of model synchronization
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
[machine-readable results](verification/RESULTS-26.2.json).

Focused checks passed, including the Java backend without an SMID flag, actual
Minecraft mixin target bindings, concurrent crypto/cache roundtrips, packet
bounds, callback-based transfer ordering and immutable render snapshots.

## Alpha.3 loading changes

Remove the sender's 5 ms polling delay per model chunk and wait on the actual
write-completion callback. Prioritize the joining player's model, nearby players
(including simultaneous joiners), and the configured fallback before the remaining
catalog. Keep fresh downloads parallel; parse a full local cache in a separate
ordered background lane to avoid startup allocation bursts.

The alpha.3 production JAR completed a separate Windows dedicated-server test
with two clients, both starting with empty encrypted model caches. Selected Ame
and KAngel models were ready in about one second; the complete 1,146-entry
catalog took 28 seconds on each client. These are loopback measurements with a
warm server cache, 8 GiB client heaps, and 1,116 unique downloaded files.
They are not Internet performance guarantees. Other detailed measurements below
refer to the historical alpha.2 checkpoint `e5708a2`.

Focused write-completion, transfer-order and 18 concurrent download/cache
roundtrip checks were added and passed alongside the existing checks.

## Publication status

The upstream README points to `OpenYSMDev/OpenYSM`. GitHub returned HTTP 451 for
that repository during verification on 2026-11-10, identifying a DMCA takedown:
https://github.com/github/dmca/blob/master/2026/06/2026-06-03-ysm.md .
The snapshot contains a MIT license file, but the reported copyright dispute has
not been resolved here. This branch is an experimental draft review proposal for
`qbb95/OpenYSM-Updated`, not an official YSM release. Maintainers should review the
technical changes and publication status before accepting or releasing the port.

## Changes

- Migrate the Fabric build and Minecraft APIs to 26.2, including render extraction,
  submit collectors, GUI strata/blur, identifier and entity interfaces.
- Capture transformed model vertices for deferred vanilla GPU submission; isolate
  nested rendering scopes and apply transforms once. Native geometry acceleration
  is disabled in this port.
- Respect authored model width/height scales; fix preview entity IDs and riding
  and sleeping preview alignment.
- Load standalone model ZIPs on the server, single-wrapper archives, and legacy
  filename-encoded ZIPs. Preserve all original model files.
- Deduplicate transfers for content aliases, validate complete chunk ranges, pin
  downloads to their originating connection, and reject invalid buffer lengths.
- Synchronize existing players' models on join and on tracking start.
- Reload catalog data off the server thread and publish immutable pack snapshots.
- Persist small server metadata entries keyed by source content fingerprints.
  Reuse only entries with a validated binary cache. Source changes, invalid JSON,
  damaged binaries and mismatched keys fall back to parsing.
- Share a bounded set of immutable numeric constants, avoid retaining boxed
  numeric values, and allocate per-frame animation scratch vectors on first use.

## Validation and measured performance

The production JAR was run on an independent Windows dedicated server and two
independent Minecraft clients, with no custom models installed on the clients.
The source catalog has 1,114 original models at revision
`1749b46467acea11694891c01ede24914b22dc6c`. With built-ins and extra fixtures the
server advertised 1,146 entries (1,116 unique content hashes).

| Local measurement | Before allocation/cache changes | After |
| --- | ---: | ---: |
| Retained client Java objects, full GC histogram | 9,586,934,328 bytes | 6,918,187,024 bytes |
| Retained client object count | 354,829,048 | 231,131,263 |
| Full catalog with an 8 GiB client heap limit | Out of memory | Both clients completed |
| Warm `/ysm model reload` | 16,331.426 ms | 1,281.825 ms; 1,247.259 ms |

These measurements use the same local catalog and test machine. Warm reloads
benefit from the OS filesystem cache. Heap histograms count retained Java objects,
not total process memory; they are not a general minimum-memory guarantee.

Verified model/texture changes, automatic downloads, cached resynchronization,
late joins, reconnects, and leaving/re-entering tracking range. Remote animation
checks covered walk/run, crouch/crouch-walk, jump, swing/mining, bow/shield use,
boat riding, sleeping, swimming, creative flight, elytra flight, and extra0–extra7.
Tests used normal player state updates plus a separate test-only input/trace helper;
that helper is not packaged in the production mod. Both clients and the server
exited with code 0 after the final run.

Additional checks cover extraction fidelity, model geometry/scales/textures,
render snapshot immutability and scope recovery, packet length rejection, numeric
edge cases, independent animation scratch vectors, and metadata-cache round trips
and invalidation. All 1,114 catalog files were parsed; not every model was visually
inspected. No new swing-specific production fix was needed: Alice's animation was
observed from Bob's POV during the repeat attack/mining tests.

The focused headless checks are included under `verification/`; see its README
for the classpath setup and optional legacy-model fixture. They complement the
in-game tests and do not substitute for a real client or dedicated server run.

## Limits

- Experimental unofficial build; no Linux, authenticated public hosting, proxy,
  shader, or broad third-party mod validation.
- Earlier attempts to focus/capture an inactive client encountered native GLFW
  crashes. Their cause is unresolved; the final runs are recorded separately.
- Loading still assembles the full catalog. Scratch allocation is deferred, but
  this is not full model streaming or a promise of instant first-time loading.
