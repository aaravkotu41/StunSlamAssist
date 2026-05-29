# Stun Slam Assist

Client-side Fabric mod for Minecraft **1.21.11** that automates the mace
follow-up after your axe disables a shield. The classic stun slam, on autopilot.

## Trigger conditions

The mod auto-fires the mace hit only when ALL of the following hold at the
moment you swing your axe:

- Mod is enabled (toggle: `B`)
- Mod isn't already mid-slam (no double-fire)
- Main-hand item is an **axe**
- A **mace** exists somewhere in hotbar slots 1–9
- You are **airborne**
- You are **falling** (vertical velocity ≤ 0)        — configurable
- Fall distance ≥ configured minimum                 — default 1.0 blocks
- Target is a LivingEntity actively **raising a shield** — configurable
- Random chance roll passes (default 90%)

When it fires: schedule a mace swap + attack on a randomized 1–2 tick delay
(configurable), then restore your axe slot on the following tick.

The chance roll + random delay range make the timing non-deterministic — it
won't always look like a bot hitting frame-perfect 1-tick slams.

## Configuration

Press `O` in-game (rebindable in Controls) to open the settings screen:

- **Mod Enabled** — master switch
- **Trigger Chance** — 0–100% roll for whether the slam fires
- **Min Delay (ticks)** — lower bound on axe→mace delay (1 tick = 50 ms)
- **Max Delay (ticks)** — upper bound
- **Require Falling** — only fire when velocity is downward
- **Min Fall Distance** — only fire after this many blocks of falling
- **Require Active Shield** — only fire when target is shielding
- **Action-Bar Messages** — show "slammed" / "ON/OFF" feedback

Config saves to `.minecraft/config/stunslamassist.json` automatically. You
can also edit that file by hand while MC is closed.

## Build

Requires **JDK 21**.

```bash
unzip StunSlamAssist-source.zip
cd StunSlamAssist
gradle wrapper        # first time only — generates gradle-wrapper.jar
./gradlew build       # downloads MC + Fabric, compiles, produces the jar
```

The built mod lands at `build/libs/stunslamassist-1.0.0.jar`.

## Install

1. Install **Fabric Loader 0.16.14+** for Minecraft 1.21.11
2. Put **Fabric API** for 1.21.11 in your `mods` folder
3. Drop `stunslamassist-1.0.0.jar` into the same folder
4. Launch

## Testing it in-game

Hardest part of any combat mod is verifying it actually fires. Quick recipe:

1. Create a creative singleplayer world, get a mace + an axe
2. Install Carpet mod (https://www.curseforge.com/minecraft/mc-mods/carpet)
3. Spawn a bot holding a shield:
   ```
   /player Dummy spawn
   /effect give Dummy minecraft:resistance infinite 100 true
   /player Dummy use continuous       # holds shield up
   ```
4. Jump up, equip axe, fall, hit the bot
5. With action-bar messages enabled you'll see `[StunSlam] slammed` confirm it
6. Tune `Trigger Chance` down to 30% if you want to see the roll-fail path

If it isn't firing, common causes:
- No mace in hotbar slots 1–9
- Bot isn't actually blocking (re-run `/player Dummy use continuous`)
- You jumped up instead of falling — disable **Require Falling** to test
- Fall distance below threshold — drop **Min Fall Distance** to 0

## State-machine tests

The timing/state logic has a headless mirror class (`SlamExecutorLogic`)
that's unit-tested without Minecraft. From the project root:

```bash
javac -d out \
  src/main/java/com/stunslamassist/SlamExecutorLogic.java \
  src/test/java/com/stunslamassist/SlamExecutorLogicTest.java
java -cp out com.stunslamassist.SlamExecutorLogicTest
```

21 assertions cover delay countdown, abort on target loss, double-schedule
protection, and slot validation. These verify the state machine — they
don't substitute for in-game testing.

## Server rules warning

This is combat automation, same category as autoclicker/macro mods. Many
PvP servers ban these (especially anything that touches the 1-tick stun
slam window). Singleplayer / friend SMP: fine. Public competitive server:
read the rules first or expect a ban.

## License

MIT
