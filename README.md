<p align="center">
  <img width="500" height="500" alt="1001321498" src="https://github.com/user-attachments/assets/b9934fa6-d929-402e-b3a6-36eeb733f2b7" />  
</p>

<h1 align="center">Winlator AnTuTu</h1>

**Winlator AnTuTu** is an open-source Android application designed to run PC games and benchmark workloads with optimal performance. Built on top of the Star Bionic lineage, it incorporates native frame generation, custom Vulkan/Mali wrappers, and performance-oriented optimizations for mobile hardware.

**Information:**
- **Package:** `com.antutu.ABenchMark`

- **Android SDK:** `compileSdk 34`, `targetSdk 28`, `minSdk 26` (Android 8.0+)

- **Upstream Lineage:** Winlator → cmod → Bionic Nightly → Star Marcescence → WinHub / Star Bionic → Winlator AnTuTu

---

## Building 🛠️

This project is configured for automated builds via **GitHub Actions**.

Artifacts are generated as workflow artifacts upon completed builds.

---

## Credits 📃

This project builds upon extensive open-source work across the Android translation ecosystem:

| Contributor | Contribution |
|---|---|
| **brunodev85** | Original [Winlator](https://github.com/brunodev85/winlator) — Wine + Box64 + Turnip on Android. Foundation of every fork below. Serves standard `input_controls` profiles: <https://raw.githubusercontent.com/brunodev85/winlator/main/input_controls/> |
| **coffincolors** | [`cmod` Winlator fork](https://github.com/coffincolors/winlator) — Customization layer and base package architecture. |
| **Pipetto-crypto** | [Winlator Bionic fork](https://github.com/Pipetto-crypto/winlator) and upstream [Box64 fix branch](https://github.com/Pipetto-crypto/box64). |
| **jacojayy** | Maintainer of [WinHub](https://github.com/winhub-emul/winhub). Timeline Semaphore patches, in-game drawer redesign, file manager, imagefs / proton optimizations, and UI enhancements. |
| **vivsi** | Controller support and input bindings. |
| **StevenMX** | [Winlator-Ludashi](https://github.com/StevenMXZ/Winlator-Ludashi) — Vulkan rendering pipeline integrations and cherry-picked performance enhancements. |

### Upstream Stack

The translation and rendering stack powering this project:

- **Wine** — [WineHQ](https://www.winehq.org/)
- **Box64 / Box86** — [ptitSeb](https://github.com/ptitSeb)
- **FEXCore** — [FEX-Emu](https://github.com/FEX-Emu)
- **DXVK** — [doitsujin / Philip Rebohle](https://github.com/doitsujin)
- **DXVK-GPLAsync patch** — [Ph42oN](https://gitlab.com/Ph42oN)
- **DXVK-Sarek** — [pythonlover02](https://github.com/pythonlover02)
- **VKD3D-Proton** — [Hans-Kristian Arntzen](https://github.com/HansKristian-Work)
- **Turnip / Mesa** — [Freedreno team @ Mesa](https://gitlab.freedesktop.org/mesa/mesa)
- **Proton layers (bionic)** — [GameNative](https://github.com/utkarshdalal/GameNative)

---

## Disclaimer ⚠️

Winlator AnTuTu and its parent forks are experimental community projects. They are not affiliated with or endorsed by Microsoft, Wine, the Mesa project, Qualcomm, or any game publisher. Performance and compatibility vary by device hardware, GPU drivers, and Android version.

---

## License ⚖️

Inherits the license of the upstream Winlator project (GPL-3.0). See `LICENSE` for the full text.
