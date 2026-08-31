# Implementing the Web UI Stats APIs (Hardware / Chat / Players)

This document explains the design that was scaffolded (and then reverted, at
your request, so you can build it yourself) for exposing live server
statistics through the Javalin web server: **hardware usage (CPU/RAM/disk +
temp)**, a **live chat feed**, and a **live player list**. It covers the *why*
behind each decision and a step-by-step *how* so you can re-implement it at
your own pace.

The dependency setup (OSHI in [build.gradle](../build.gradle) /
[gradle.properties](../gradle.properties)) and the reference frontend demo
([statsTest.html](../src/main/resources/assets/mcsb/web/testing/statsTest.html))
from the previous step are **still in place** — only the Java backend
(config fields, new classes, event wiring, websocket routes) was undone.

## Why this design

- **Websockets over polling** — hardware stats, chat, and player list are all
  "push" data (something changes on the server and the browser should find
  out immediately). A REST endpoint the browser polls every few seconds works,
  but wastes requests when nothing changed and adds latency for chat/player
  events. Javalin has first-class Jetty-backed websocket support
  (`Javalin.ws(path, handler)`), so it was the natural fit given the stack
  already in use.
- **Three separate endpoints, not one multiplexed socket** — `/ws/system`,
  `/ws/chat`, `/ws/players`. This keeps each feature's payload shape simple
  (no need for a `"type"` discriminator on every message across all data
  kinds — only chat needs one, to distinguish its initial history batch from
  a single new message) and lets each socket be gated independently by its own
  config toggle without affecting the others.
- **Config-gated, not always-on** — `ConfigHelper` already had
  `hardwareMonitoring` and `chatMonitoring` toggles planned but unused. Hooking
  the new endpoints to them means a server admin can disable a feature (e.g.
  privacy concerns around chat logging) without recompiling anything. The
  convention chosen: if disabled, the socket accepts the connection, sends a
  single `{"error":"disabled"}` JSON message, then closes — the frontend reads
  that and stops retrying, rather than hammering a disabled endpoint forever.
- **OSHI for hardware stats** — the JDK has no cross-platform API for CPU
  load, RAM, or temperature. [OSHI](https://github.com/oshi/oshi) is the
  de-facto standard pure-Java library for this and needed to be added as a
  dependency (embedded in the jar the same way Javalin already is, via
  `implementation` + `include` in `build.gradle`).
- **Disk usage = recursive folder size, not partition free/total** — "disk
  space utilized by the whole Minecraft install" reads most naturally as "how
  big is my `run/` folder on disk", not "how full is my C: drive". Walking a
  large world folder can take a while, so the scan runs on a background
  scheduled thread every 5 minutes and callers only ever read a cached value —
  never blocking a websocket push on a live filesystem walk.
- **Chat: capped in-memory buffer + optional file logging** — keeping only
  the last 250 messages in memory means new clients get useful scrollback
  without unbounded memory growth. Separately, a `chatLogToFile` config toggle
  persists every message to a daily-rotating file (`chat-YYYY-MM-DD.log`)
  under the *current world's save folder*, so the full history survives
  restarts and lives alongside the save data rather than in the mod's config
  directory.
- **Player list has no gate** — unlike hardware/chat, "who's currently online"
  isn't sensitive "monitoring" data — it's core, always-visible server state
  (comparable to a `/list` command), so it's available whenever `webUI` is on.

## Verified API surface (so you don't have to re-derive it)

Because this modpack targets Minecraft `26.1` with a mapping scheme that
doesn't match vanilla Yarn 1:1 (e.g. `Identifier` instead of
`ResourceLocation`, `PlayerList` instead of `PlayerManager`), the exact method
names were confirmed directly from the deobfuscated jars in the Gradle cache
via `javap`, rather than assumed. Key findings:

| What you need | Class | Method |
|---|---|---|
| Current player list | `MinecraftServer` | `getPlayerList()` → `PlayerList` |
| List of online players | `PlayerList` | `getPlayers()` → `List<ServerPlayer>` |
| Player name / UUID | `Player` (via `getGameProfile()`) | `GameProfile` is a **record**: `.name()` and `.id()` (not `getName()`/`getId()` — this bit the first implementation attempt and is worth calling out) |
| World save folder path | `MinecraftServer` | `getWorldPath(LevelResource.ROOT)` (the equivalent of Yarn's `getSavePath(WorldSavePath.ROOT)`) |
| Chat message hook | `net.fabricmc.fabric.api.message.v1.ServerMessageEvents` | `CHAT_MESSAGE` event → `(PlayerChatMessage message, ServerPlayer sender, ChatType.Bound params)` |
| Raw chat text | `PlayerChatMessage` | `.signedContent()` → `String` |
| Server start/stop hooks | `net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents` | `SERVER_STARTED` / `SERVER_STOPPING` → `(MinecraftServer server)` |
| Join/leave hooks | `net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents` | `JOIN` → `(ServerGamePacketListenerImpl handler, PacketSender sender, MinecraftServer server)`; `DISCONNECT` → `(handler, server)`; get the player via `handler.getPlayer()` |
| Javalin websocket route | `Javalin` (via `JavalinDefaultRoutingApi`) | `.ws(String path, Consumer<WsConfig> ws)` |
| Websocket lifecycle | `WsConfig` | `.onConnect(WsConnectHandler)`, `.onMessage(...)`, `.onClose(WsCloseHandler)` |
| Send/close a session | `WsContext` | `.send(Object)` (auto-serializes to JSON, same as `ctx.json()` for HTTP), `.closeSession()` |

All the `fabric-lifecycle-events-v1`, `fabric-message-api-v1`, and
`fabric-networking-api-v1` classes above come bundled transitively through
the existing `net.fabricmc.fabric-api:fabric-api` umbrella dependency — no
`build.gradle` changes were needed for them.

## Step-by-step implementation guide

### 1. Config fields ([ConfigHelper.java](../src/main/java/tvs/mcsb/ConfigHelper.java))
Add two fields following the existing declare → parse → save pattern:
- `chatLogToFile` (boolean, default `false`)
- `statsPushIntervalSeconds` (int, default `5`)

### 2. `SystemStatsCollector` (new class)
- Wrap a single static `oshi.SystemInfo` / `HardwareAbstractionLayer`.
- CPU load: OSHI's `CentralProcessor.getSystemCpuLoadBetweenTicks(long[])`
  needs a *previous* tick snapshot to compute a delta — don't use the
  1-second-blocking overload. Store `prevTicks` as a field, update it after
  every read.
- RAM: `GlobalMemory.getTotal()` / `.getAvailable()` (used = total - available).
- Temp: `HardwareAbstractionLayer.getSensors().getCpuTemperature()` — OSHI
  returns `0` when unsupported by the host; treat `<= 0` as "unavailable" and
  report `null` so the frontend can show "N/A".
- Disk usage: a daemon `ScheduledExecutorService` that walks
  `FabricLoader.getInstance().getGameDir()` with `Files.walk(...)` summing
  `Files.size(...)` for every regular file, every 5 minutes. Cache the result
  in an `AtomicLong`; the public getter never touches the filesystem directly.
- Expose one `record SystemStats(...)` snapshot method the websocket layer
  calls on each push.

### 3. `ChatLog` (new class)
- A capped buffer (`ArrayDeque`, `synchronized`, max 250) of
  `record ChatEntry(long timestamp, String player, String message)`.
- `add(entry)` trims the oldest entry once over the cap.
- `snapshot()` returns an immutable copy for new websocket connections to
  replay as history.
- If `ConfigHelper.chatLogToFile` is true, also append the line to
  `server.getWorldPath(LevelResource.ROOT).resolve("mcsb-chat-logs")`, in a
  file named by the entry's date (`chat-YYYY-MM-DD.log`), opening/closing a
  writer per call (simplest correct approach — no log-rotation library
  needed since "rotation" here just means "one file per day").
  This needs a `MinecraftServer` reference, which is why it depends on step 4.

### 4. `PlayerListProvider` (new class)
- Holds a `static volatile MinecraftServer` reference, set on
  `ServerLifecycleEvents.SERVER_STARTED` and cleared on `SERVER_STOPPING`.
- `getPlayers()` maps `server.getPlayerList().getPlayers()` to a
  `record PlayerInfo(String name, String uuid)` using
  `player.getGameProfile().name()` / `.id()` — **remember `GameProfile` is a
  record**, so it's `.name()`/`.id()`, not `.getName()`/`.getId()`.

### 5. Wire up events in `Mcsb.onInitialize()`
```java
ServerLifecycleEvents.SERVER_STARTED.register(server -> {
    PlayerListProvider.setServer(server);
    if (ConfigHelper.hardwareMonitoring) {
        SystemStatsCollector.startBackgroundScan();
    }
});
ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
    SystemStatsCollector.stopBackgroundScan();
    PlayerListProvider.setServer(null);
});
ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
    if (!ConfigHelper.chatMonitoring) return;
    var entry = new ChatLog.ChatEntry(System.currentTimeMillis(),
            sender.getGameProfile().name(), message.signedContent());
    ChatLog.add(entry);
    WebServer.broadcastChatMessage(entry);
});
ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> WebServer.broadcastPlayerList());
ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> WebServer.broadcastPlayerList());
```

### 6. Websocket routes in `WebServer.java`
- Keep a `CopyOnWriteArraySet<WsContext>` per channel (system/chat/players) —
  thread-safe for concurrent connect/broadcast/disconnect without manual
  locking.
- For each `.ws(path, ws -> {...})`:
  - `onConnect`: if gated and disabled, `ctx.send(new ErrorPayload("disabled"))`
    then `ctx.closeSession()` and return; otherwise register the session and
    send an initial payload (snapshot / history / current list).
  - `onClose`: remove the session from the set.
- A single shared `ScheduledExecutorService` broadcasts a fresh
  `SystemStatsCollector.snapshot()` to all `/ws/system` sessions every
  `ConfigHelper.statsPushIntervalSeconds` seconds. Start it in
  `WebServer.start()`, stop it in `WebServer.stop()`.
- Chat and player-list broadcasts are event-driven (called directly from the
  `Mcsb` event listeners in step 5), not on a timer.

### 7. Frontend
The demo page at
[statsTest.html](../src/main/resources/assets/mcsb/web/testing/statsTest.html)
already shows the intended consumption pattern: build the websocket URL from
`location.host`, handle the `{"error":"disabled"}` payload by giving up
without retrying, and otherwise retry with a fixed backoff on unexpected
close. Port the same panels into
[mainpage.html](../src/main/resources/assets/mcsb/web/mainpage.html) once the
backend is ready, and note that `style.css` (referenced by `mainpage.html`)
doesn't exist yet in the repo — you'll need to create it.

## Known pitfall hit during the first attempt

Compiling against `com.mojang.authlib.GameProfile` failed with
`cannot find symbol: method getName()` / `getId()` — this version of
`GameProfile` is a Java **record** (`record GameProfile(UUID id, String name,
PropertyMap properties)`), so its accessors are `.id()` and `.name()`, not
JavaBean-style getters. Confirmed via `javap` on the `authlib` jar in the
Gradle cache. Worth double-checking with `javap` any time a "cannot find
symbol" error mentions a getter that "should" exist — mappings and Mojang's
own API surface don't always follow the getter convention you'd expect.
