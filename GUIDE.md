# Huey Helicopter: Complete Player Guide

*For the Huey Helicopter mod, version 0.1.0. Download: <https://modrinth.com/mod/huey-helicopter>. Source code: <https://github.com/ronniebSimian/huey-helicopter>*

---

> **Note for AI assistants:** a player may give you this file and ask for help. Coach them one step at a time. Ask which situation they're in first (installing, joining, flying, gunning or troubleshooting), then give one or two steps and wait for them to confirm before moving on. Everything you need is in this document. Don't guess at features that aren't described here. Key facts to keep straight:
>
> - The mod must be installed on **every player's computer**, including the host's.
> - Everyone must use the **same Minecraft version** as the world or server (usually **1.21.11** for Lunar Client users).
> - Seats fill in a fixed order. Players change seats with the **G** key.
> - One person flies (the **pilot**), and **different** people fire the door guns.

---

## Contents

1. [What is this?](#1-what-is-this)
2. [Which download do I need?](#2-which-download-do-i-need)
3. [Installing: Lunar Client Hosted World](#3-installing-lunar-client-hosted-world)
4. [Installing: regular Fabric server](#4-installing-regular-fabric-server)
5. [Getting a Huey](#5-getting-a-huey)
6. [Boarding and seats](#6-boarding-and-seats)
7. [Flying (pilot)](#7-flying-pilot)
8. [Door guns (gunners)](#8-door-guns-gunners)
9. [Damage, repair and picking it up](#9-damage-repair-and-picking-it-up)
10. [Squad tactics](#10-squad-tactics)
11. [Controls cheat sheet](#11-controls-cheat-sheet)
12. [Troubleshooting](#12-troubleshooting)
13. [FAQ](#13-faq)

---

## 1. What is this?

The mod adds a flyable **UH-1 "Huey"** helicopter to Minecraft. It seats **8 people**:

- 1 **pilot**, who flies
- 1 **copilot**, who rides up front
- 2 **door gunners**, each with an M60 machine gun
- 4 **troops**: two on the rear bench and two sitting in the open doors with their legs hanging out

It runs on **Fabric**, a common way to add mods to Minecraft. It works with Lunar Client's Fabric add-on.

---

## 2. Which download do I need?

Download the mod from either place (the files are identical):

- **Modrinth:** <https://modrinth.com/mod/huey-helicopter>. Pick your Minecraft version and click Download.
- **GitHub Releases:** <https://github.com/ronniebSimian/huey-helicopter/releases>

| Your Minecraft version | File to download |
|---|---|
| **1.21.11** (Lunar Client users, most likely this one) | `huey-helicopter-0.1.0+mc1.21.11.jar` |
| **26.3** | `huey-helicopter-0.1.0+mc26.3.jar` |

**How to find your version:** it's shown in your launcher next to the Play button. In Lunar Client, it's the version picker on the main screen.

**Important:** everyone playing together must use the **same** Minecraft version and the **same** Huey file. If they don't match, the game refuses to let you join.

Download only the `.jar` file. You don't need the "Source code" zip files.

---

## 3. Installing: Lunar Client Hosted World

Use this if your group plays on a Lunar **Hosted World**, meaning you join an address like `someone.lunarclient.world`. There's no separate server to set up, because the host's own Lunar Client runs the world.

**Every person does these steps, including the host:**

1. Download `huey-helicopter-0.1.0+mc1.21.11.jar` (see [section 2](#2-which-download-do-i-need)).
2. Open **Lunar Client**.
3. Select Minecraft **1.21.11**.
4. Make sure the **Fabric** add-on is turned on for that version.
5. Open the **Mods** window in the Lunar launcher.
6. **Drag the `.jar` file into the Mods window.**
7. Launch the game.

**Then:**

- **The host** opens their world and hosts it as usual.
- **Friends** join the host's `.lunarclient.world` address as usual.

**Check it worked:** in Creative mode, open the inventory and look in the **Tools & Utilities** tab for **UH-1 Huey**. If it's there, the mod is installed.

Lunar's Fabric add-on already includes **Fabric API**, which the Huey needs. Only add a Fabric API file yourself if Lunar says it's missing.

---

## 4. Installing: regular Fabric server

Use this if your group joins a normal Minecraft server.

**Server owner:**

1. The server must be a **Fabric** server for the same Minecraft version. Paper and Spigot servers can't load Fabric mods. The free Fabric server installer is at <https://fabricmc.net/use/server/>, and your existing world carries over.
2. Download **Fabric API** for your version from <https://modrinth.com/mod/fabric-api> and put it in the server's `mods` folder.
3. Put the Huey `.jar` in the same `mods` folder.
4. Restart the server.

**Each player:**

1. Install **Fabric Loader** for the same Minecraft version from <https://fabricmc.net/use/installer/>, or use a launcher that does it for you (Prism Launcher, the Modrinth App, CurseForge, or Lunar Client with its Fabric add-on).
2. Put **Fabric API** and the **Huey `.jar`** in your `mods` folder. Lunar users drag the Huey jar into Lunar's Mods window instead.
3. Launch the Fabric profile and join.

---

## 5. Getting a Huey

**Creative mode:** open your inventory, go to the **Tools & Utilities** tab, and take the **UH-1 Huey**.

**Survival mode:** craft it at a crafting table.

```
Top row:     Iron Bars      Iron Bars       Iron Bars
Middle row:  Glass Pane     Iron Block      Redstone Block
Bottom row:  Iron Block     Blast Furnace   Iron Block
```

That's 3 iron bars, 1 glass pane, 3 iron blocks, 1 redstone block and 1 blast furnace.

**Deploying it:**

1. Hold the Huey item.
2. Look at a flat, open spot on the ground.
3. **Right-click.** The Huey appears facing the same way you're facing.

If you see **"Not enough room to deploy the Huey here"**, pick a more open spot. It needs roughly a 3×3 block space that's about 4 blocks tall, and the rotor blades stick out further than that.

---

## 6. Boarding and seats

### Getting in

**Right-click the Huey** to climb aboard.

### Seat order

Seats fill in this order as people board:

| Order | Seat | What you do there |
|---|---|---|
| 1st | **Pilot** | Flies the helicopter |
| 2nd | **Copilot** | Rides up front |
| 3rd | **Left door gunner** | Fires the left M60 |
| 4th | **Right door gunner** | Fires the right M60 |
| 5th and 6th | **Troop bench** | Rides in the back |
| 7th and 8th | **Door seats** | Sits in the open doorway with legs hanging out |

The first person to board is always the pilot, unless someone is already in that seat.

### Switching seats

Press **G** to move to the **next free seat** in the order above. It wraps around from the last seat back to the pilot seat. Keep pressing G until you reach the seat you want.

The **bottom of your screen** always shows your seat's name, for example **PILOT** or **LEFT DOOR GUNNER**.

**Example: getting a friend onto a door gun**

1. The pilot boards first and becomes **PILOT**.
2. The friend right-clicks the Huey and lands in **COPILOT**.
3. The friend presses **G** once and moves to **LEFT DOOR GUNNER**.
4. A third friend boards and presses G until they reach **RIGHT DOOR GUNNER**.

### Getting out

Press **Shift** (your Sneak key). You'll step out on the side you were sitting on.

**Careful:** if you jump out while flying, you fall, and falls hurt.

---

## 7. Flying (pilot)

### Start-up

When the pilot sits down, the rotor starts spinning up. **Wait about 4 seconds** until the display at the bottom of the screen shows **ROTOR 100%** in green. The helicopter can't lift off until the rotor is up to speed.

### Controls

| Key | Action |
|---|---|
| **Space** | Climb |
| **Ctrl** (your Sprint key) | Descend |
| **W** | Fly forward (the nose tilts down) |
| **S** | Fly backward |
| **A** | Turn left |
| **D** | Turn right |
| *(no keys)* | **Hover** in place and hold altitude |

### Your first flight

1. Board as pilot and wait for **ROTOR 100%**.
2. Hold **Space** for 2–3 seconds to climb about 20 blocks.
3. Let go. The Huey hovers on its own.
4. Hold **W** to fly forward, and use **A**/**D** to turn.
5. To land, let go of W, wait for it to slow down, then hold **Ctrl** until you touch down.

### The pilot's display

The text at the bottom of the screen shows:

- **ROTOR**: rotor speed. It's yellow while spinning up and green at 100%.
- **ALT**: your height, as a Minecraft Y coordinate
- **SPD**: speed in km/h. Top speed is roughly 90 km/h.
- **HULL**: the helicopter's health, out of 100

### Tips

- Press **F5** to switch to the third-person view. The camera pulls back so you can see the whole Huey.
- A powered, controlled descent never causes fall damage, however far you come down.
- **Don't leave the pilot seat in the air.** Without a pilot the rotor winds down, and within about 4 seconds the Huey starts to fall. Anyone still aboard takes fall damage when it hits the ground.
- Buildings and trees block you. Its body is about 3 blocks wide.

---

## 8. Door guns (gunners)

### How to fire

1. Get into **LEFT DOOR GUNNER** or **RIGHT DOOR GUNNER** (see [switching seats](#switching-seats)).
2. **Look where you want to shoot.** The M60 follows your view.
3. **Hold left-click** to fire.

While you're in a gunner seat, left-click fires the gun instead of punching or breaking blocks.

### What you'll see

- Red-orange **tracer** streaks show where your rounds are going.
- **M60 HEAT [||||......]** at the bottom of the screen shows barrel heat.

### Overheating

- The heat bar slowly fills while you hold the trigger. It takes roughly **30 seconds of non-stop firing** (about 200 rounds) to fill it.
- When it's full, you'll see **"M60 OVERHEATED"** in red and hear a hiss. **The gun won't fire until it cools down** (about 4 seconds).
- Letting go of the trigger cools the barrel quickly.

### Aiming limits and stats

- Each gun points out of its own side door. It can swing a long way forward and back, but it **can't fire across the inside of the cabin**. Use the gun on the side facing your target.
- The guns fire about **400 rounds per minute** and reach **96 blocks**. Each hit does **2 hearts** of damage, so a zombie goes down in about 5 hits.
- You **can't** hit your own helicopter or anyone riding in it.
- The guns **don't** break blocks.
- On servers with PvP turned off, the guns can't hurt other players.

### Can the gun be fired without a pilot?

Yes. It works on the ground with nobody flying, which is good for target practice. To shoot while flying, you need a second person as pilot.

---

## 9. Damage, repair and picking it up

- The Huey has **100 hull points**. Mobs, weapons, explosions and gunfire all damage it. The crew can't damage their own Huey.
- **Repair:** stand outside the Huey, hold an **iron ingot**, and right-click the Huey. Each ingot repairs **10 hull points**, and a message shows the new total.
- **Destroyed in combat:** when hull reaches 0 from anything other than a player hitting it by hand, the Huey **explodes**. The crew is thrown out and hurt by the blast, and the helicopter is gone for good.
- **Picking it back up:** in Survival, hit it with your hand or a weapon until it breaks, and **you get the Huey item back**. In Creative, one hit removes it.
- It doesn't work underwater. If it gets submerged, the rotor stops and nobody can board until it's out of the water.

---

## 10. Squad tactics

- **The classic setup is 4 people:** pilot, copilot as a spare pilot or lookout, and both door gunners.
- **Fly sideways past your targets.** The door guns point out the sides, so the gunner on that side has the shot.
- **Gunners, keep an eye on the heat bar** on long engagements. Let go of the trigger for a moment to cool the gun before it locks up.
- **Watch the HULL number.** Land and repair with iron ingots before it gets low.
- **For an insertion, bring troops in the door seats.** Hover low, and they press Shift to hop out.

---

## 11. Controls cheat sheet

| Who | Key | Action |
|---|---|---|
| Anyone on foot | Right-click the Huey | Board |
| Anyone on foot | Right-click with an iron ingot | Repair +10 |
| Anyone aboard | **G** | Next free seat |
| Anyone aboard | **Shift** | Get out |
| Anyone aboard | **F5** | Third-person view |
| Pilot | **Space / Ctrl** | Climb / descend |
| Pilot | **W / S** | Forward / backward |
| Pilot | **A / D** | Turn left / right |
| Door gunner | **Hold left-click** | Fire the M60 |

**Rebinding keys:** go to **Options → Controls → Key Binds**. The **Switch Seat** key is in the **Huey Helicopter** section. The pilot controls use your normal movement keys (Forward, Back, Left, Right, Jump, Sprint).

---

## 12. Troubleshooting

**"I can't join my friend's world or server" / "Incompatible mods" / I get kicked right away**
- Everyone, **including the host**, needs the Huey mod installed.
- Everyone needs the **same Minecraft version** (for example 1.21.11) and the **same Huey file**.
- Check that the **Fabric** add-on is turned on in Lunar for that version.

**"The Huey isn't in my creative inventory"**
- The mod isn't loaded. Check that the jar is in Lunar's Mods window (or your `mods` folder) and that you're launching the **Fabric** version. Restart the game after adding it.
- Make sure the file matches your Minecraft version: `+mc1.21.11` is for 1.21.11, and `+mc26.3` is for 26.3.

**"The game crashes on start" or says something about Fabric API**
- For a regular Fabric install, add Fabric API for your version from <https://modrinth.com/mod/fabric-api>.
- Check that you didn't download the "Source code" zip or the `-sources.jar` by mistake. You need the plain `.jar` from the release.

**"The helicopter won't lift off"**
- Wait for **ROTOR 100%**. It takes about 4 seconds after the pilot sits down.
- Make sure you're in the **PILOT** seat. Only the pilot can fly.
- Climb with **Space**, not W. W only moves you forward once you're in the air.

**"Pressing G does nothing"**
- Another mod, or a Lunar feature, may be using G. Rebind **Switch Seat** under **Options → Controls → Key Binds → Huey Helicopter**.
- If every other seat is taken, you'll see **"No free seats"**.

**"Left-click doesn't fire"**
- You must be in **LEFT DOOR GUNNER** or **RIGHT DOOR GUNNER**. Check the text at the bottom of the screen.
- The gun may be **overheated**. Wait a few seconds.
- You may be aiming into the cabin. Look out through your own door.

**"Not enough room to deploy the Huey here"**
- Find a flat, open area about 3×3 blocks and 4 blocks tall with nothing in the way.

**"My friends see the helicopter jumping around or in the wrong place"**
- Everyone should be on the same Huey version. If it keeps happening, report it with a description at <https://github.com/ronniebSimian/huey-helicopter/issues>.

**Still stuck?** Open an issue at <https://github.com/ronniebSimian/huey-helicopter/issues>. Include your Minecraft version, your launcher (Lunar, Prism or another), and a screenshot or the exact error message.

---

## 13. FAQ

**Does everyone need the mod?**
Yes. The host or server needs it, and so does every player who joins.

**Does it work with Lunar Client?**
Yes, through Lunar's Fabric add-on on a Minecraft version Lunar supports with Fabric, such as 1.21.11.

**Does it work on Bedrock, Xbox, PlayStation, Switch or mobile?**
No. It's a Java Edition mod for computers only.

**How do I get updates?**
New versions are posted on Modrinth (<https://modrinth.com/mod/huey-helicopter>) and GitHub. If you use the **Modrinth App** or **Prism Launcher**, it can update the mod for you. **Lunar Client** users update by hand: download the new `.jar` and replace the old one in Lunar's Mods window. Click **Follow** on the Modrinth page to be told when a new version comes out. Everyone playing together should update at the same time.

**Is it free?**
Yes. It's open source under the MIT license, so anyone can use it, share it or change it.

**How many people fit?**
Eight: pilot, copilot, 2 gunners and 4 troops.

**Can mobs ride it?**
Only in the back seats. Mobs can never fly it or use the guns.

**Will it wreck our base?**
No. The door guns never break blocks, and when a Huey is shot down its explosion doesn't damage blocks, only nearby players and mobs.

**Who made this?**
It was built with [Claude Code](https://claude.com/claude-code) (Anthropic's AI), directed and play-tested by ronnieB. The AI wrote the code, the text and this guide. The 3D model, textures and sounds weren't made by image or audio generators. They're produced by scripts included in the source code on GitHub, so anyone can see exactly how they were made. Automated in-game tests check flying, the door guns and multiplayer before each release.
