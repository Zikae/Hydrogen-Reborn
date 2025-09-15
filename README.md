![alt text](https://raw.githubusercontent.com/zPeanut/Resources/master/hydrogen.png)


Hydrogen Reborn

An open-source, mixin-based ghost client for Minecraft 1.8.9, built on Minecraft Forge.
This is the Hydrogen Reborn version.

Developed by zPeanut, UltramoxX, and Zikae.
Contributions are highly welcome!

Current stable release: 1.12.5

Join our Discord:
https://discord.gg/dmw5N5X9p6

Full Changelog:
https://zpeanut.github.io/main/changelog

⚠️ Disclaimer:
I'm are not responsible for any repercussions you face using this client.
We are merely providing it. Use at your own risk.


---

Features

Hydrogen Reborn includes 50+ modules, such as:

Fully customizable in-game GUI

Render, combat, and utility-focused modules


Go ahead and try them out!


---

User Installation

Before you install Hydrogen Reborn, make sure you have Minecraft Forge for 1.8.9 installed.

Automatic Installation:

1. Download the Installer (src code)


2. Select your version


3. Enjoy the ride 🎉



Manual Installation:

1. Download the latest Hydrogen Reborn release


2. Drag and drop hydrogen-x.x.x.jar into your Forge mods directory

Default (Windows): %appdata%/.minecraft/mods



3. Select your launcher profile with the respective Forge version


4. Enjoy the ride 🎉




---

Setup with Forge MDK

Hydrogen Reborn runs on Gradle. Ensure it’s installed correctly before setup.

1. Clone the repository:
https://github.com/zPeanut/Hydrogen.git


2. Open Command Prompt and navigate (cd) into the directory.


3. Run the correct command for your IDE:

IntelliJ:

gradlew setupDecompWorkspace idea genIntellijRuns build

Eclipse:

gradlew setupDecompWorkspace eclipse build


4. Open the folder in your preferred IDE.




---

Exporting the Client with Forge MDK

After making changes, export Hydrogen Reborn with:

.\gradlew clean build

You’ll find the compiled JAR in:

\build\libs

Move it into your .minecraft/mods folder, and you’re good to go!


---

Credits

Dependencies:

DarkMagician6's EventAPI – An open-source system for handling events across Java projects.

Semver4j – A semantic versioning library used to check if Hydrogen Reborn is outdated.


Special Thanks:

superblaubeere27 – Provided most of the initial client base

HeroCode – ClickGUI API

Lemon – Settings system used in the client

zPeanut, UltramoxX, Zikae – Core development

All contributors on Discord for bug reports and module suggestions


Community Contributors:

ProfKambing

QianHeJ

neyled

qaql

S4rnth1l

perry

