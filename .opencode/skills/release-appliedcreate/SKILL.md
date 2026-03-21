---
name: release-appliedcreate
description: Release Applied Create on both maintained branches by bumping mod_version, building with Java 17, tagging as mcVersion-modVersion, pushing branch and tag, and publishing to CurseForge and Modrinth via publishMods.
user-invocable: true
---

# Applied Create Dual-Branch Release

Use this skill when releasing Applied Create on both active branches:

- `main` -> Minecraft `1.20.1` / Forge
- `1.21.1-neoforge` -> Minecraft `1.21.1` / NeoForge

## Required inputs

- Target mod version, e.g. `1.1.3`
- Short English changelog, 2-4 bullets max

## Preflight

For each branch before making changes:

1. Checkout the target branch.
2. Confirm `git status` is clean except known unrelated untracked scratch files that must stay untracked.
3. Confirm branch is up to date with origin.
4. Read `gradle.properties` and verify:
   - `mod_version`
   - `minecraft_version`
5. Check whether the release tag already exists:
   - `1.20.1-<modVersion>` on `main`
   - `1.21.1-<modVersion>` on `1.21.1-neoforge`
6. Use Java 17 for all Gradle commands:
   - `export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`
   - `export PATH=$JAVA_HOME/bin:$PATH`

## Release procedure per branch

### 1. Bump version

Edit only this source of truth:

- `gradle.properties` -> `mod_version=<targetVersion>`

### 2. Verify locally

Run:

```bash
./gradlew build -x test
```

If tests exist and are meaningful in the future, prefer running them before publish instead of skipping them.

### 3. Commit

Use the repo's existing release style:

```bash
git add gradle.properties
git commit -m "release: <targetVersion>"
```

### 4. Tag

Create tag using `minecraft_version-mod_version`:

```bash
git tag "<minecraftVersion>-<targetVersion>"
```

Examples:

- `1.20.1-1.1.3`
- `1.21.1-1.1.3`

### 5. Push

Push branch first, then tag:

```bash
git push origin <branch>
git push origin "<minecraftVersion>-<targetVersion>"
```

### 6. Publish

Set concise English changelog through `CHANGELOG` and publish:

```bash
export CHANGELOG=$'Version <targetVersion>\n\n- Bullet 1\n- Bullet 2'
./gradlew publishMods
```

`publishMods` publishes to both:

- CurseForge
- Modrinth

## Post-publish verification

After each branch publish:

1. Confirm `publishMods` finished successfully.
2. Confirm branch and tag were pushed.
3. Confirm the tag points at the intended release commit.
4. Confirm the published entry uses the correct Minecraft version and loader for that branch.
5. Confirm the visible changelog matches the passed English release note.

## Branch order

Recommended order:

1. `main`
2. `1.21.1-neoforge`

This keeps the stable Forge branch release completed first.

## Notes

- Do not commit unrelated untracked helper files.
- Do not reuse the same tag name across branches.
- Do not publish before the branch commit and tag are pushed.
- Tokens for CurseForge/Modrinth should come from environment variables or untracked local Gradle properties, never newly committed secrets.
