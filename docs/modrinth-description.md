# UH-1 "Huey" Helicopter

A flyable **UH-1 "Huey"** for your squad. One pilot flies, two door gunners run the M60s, and the rest of the team rides in the back or sits in the open doors with their legs hanging out.

![The Huey](https://raw.githubusercontent.com/ronniebSimian/huey-helicopter/main/docs/images/huey-front.png)

## Features

- **Arcade-style flying.** The rotor spins up over about 4 seconds. Then you lift off, hover hands-free, and fly forward with the nose tilted down.
- **8 seats:** pilot, copilot, left and right door gunners, two troop-bench seats and two door seats. Press **G** to switch seats.
- **M60 door guns** with tracers. The guns follow where the gunner looks, and they can overheat.
- **The whop-whop.** The rotor thump carries about 80 blocks.
- **Solid multiplayer.** It uses Minecraft's built-in vehicle syncing, so there's no jitter and no "kicked for flying". Tested on dedicated servers.
- **Hull damage and repair** (right-click with iron ingots), plus a survival crafting recipe.
- **Only needs Fabric API.** Works with **Lunar Client's** Fabric add-on, including Lunar Hosted Worlds.

## Controls

| Who | Key | Action |
|---|---|---|
| Anyone | Right-click the Huey | Board |
| Anyone | **G** | Next free seat |
| Anyone | **Shift** | Get out |
| Pilot | **Space / Ctrl** | Climb / descend |
| Pilot | **W / S** | Forward / back |
| Pilot | **A / D** | Turn |
| Door gunner | **Hold left-click** | Fire the M60 |

## Install

Install it on the **server (or Hosted World host) and on every player's client**. Everyone needs the same Minecraft version.

📖 **Full step-by-step guide:** [GUIDE.md](https://github.com/ronniebSimian/huey-helicopter/blob/main/GUIDE.md). You can also hand it to an AI assistant to walk you through setup.

## How it was made

Built with [Claude Code](https://claude.com/claude-code) (Anthropic's AI), directed and play-tested by ronnieB. The AI wrote the Java code, the text and the guide. The 3D model, textures and sounds aren't from image or audio generators: they're produced by Python scripts in the GitHub repo, so you can read exactly how every asset is made. Automated in-game tests check the flying, door guns and dedicated-server play before each release.

## Open source, free forever

MIT licensed. Source code, issue tracker and the scripts that generate every model, texture and sound are all on [GitHub](https://github.com/ronniebSimian/huey-helicopter). Contributions welcome. Add a Chinook!
