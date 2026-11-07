# Key Macro (Fabric, Minecraft 1.21.4)

Client-side mod: press one trigger key and it fires a sequence of key presses
as fast as you like (several per tick if there is no `wait` between them).

## Build
Install JDK 21 and Gradle 8.10+, then in this folder:

    gradle wrapper      # once
    ./gradlew build     # Windows: gradlew.bat build

The jar is at `build/libs/keymacro-1.0.0.jar` (not the -sources one).
Drop it in `.minecraft/mods` with Fabric Loader + Fabric API.

## Config: `.minecraft/config/keymacro.json` (created on first launch)

    {
      "enabled": true,
      "macros": [
        { "name": "Quick swap", "trigger": "r",
          "steps": ["tap 2", "tap rmb", "tap 1"] },
        { "name": "Rapid use", "trigger": "g", "loop": true,
          "steps": ["tap rmb", "wait 1"] }
      ]
    }

Steps: `tap K`, `press K`, `release K`, `hold K ticks`, `wait ticks` (20 ticks = 1s).
Keys: `f`, `2`, `space`, `left_shift`, `lmb`, `rmb`, `mmb`, `mouse4`,
or full names like `key.keyboard.left.control`.

In game: `/keymacro reload` re-reads the config, `/keymacro stop` cancels running macros.
Macros only run with no screen open (so typing in chat won't trigger them).
Note: many multiplayer servers prohibit input automation. Check the rules first.
