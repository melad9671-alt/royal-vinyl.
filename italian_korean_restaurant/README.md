# Seoul × Roma — Restaurant Simulator

A mobile-first 3D restaurant management game made with Godot 4.

## Current playable slice
- A 3D restaurant floor with dining tables, chairs, service counter and five kitchen station areas.
- Mobile movement controls plus WASD keyboard controls for desktop testing.
- Customer queue and a simple order → preparation timer → serving loop.
- A recipe catalog with 40 dishes: 20 Italian and 20 Korean.
- Separate Italian euro and Korean won-style reward values (prototype economy).

## Project structure
- `project.godot` — Godot project settings.
- `scenes/main.tscn` — main scene.
- `scripts/game.gd` — 3D environment, player, HUD and gameplay loop.
- `scripts/recipe_catalog.gd` — recipe data.

## Run
Open this folder as a Godot 4.4 project and run the main scene.

## Roadmap
1. Replace prototype geometry with detailed original restaurant assets and animations.
2. Add ingredient inventory, individual preparation steps, cooking stations and plating.
3. Add customer patience, table seating, staff, cleaning, dishwashing and daily accounting.
4. Add upgrades, unlockable recipes, restaurant expansion, sound and save data.
5. Polish touch controls and Android performance.

This is an evolving prototype, not a finished commercial game. The 3D environment is currently generated from primitives so it can run without external art downloads.
