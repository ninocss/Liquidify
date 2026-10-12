# Liquidify 1.0.0 Beta

Liquid Glass für **TIDAL 2.200.0 (10004)** auf Android 13+: Navigation,
Mini-Player, native Menüs und Gesten, Light/Dark/System-Themes sowie ein
verankertes Output-Menü mit Linn-OpenHome-Anbindung.

**Editor: [ninocss](https://github.com/ninocss).** Liquidify Manager ist ein Fork
von [Radiant Lyrics](https://github.com/meowarex/rl-mobile), entwickelt von
meowarex und den dortigen Contributors. Die ursprünglichen Radiant-Patches
behalten ihre Namen, Inhalte und Credits.

## Installation

1. Im [GitHub-Release](https://github.com/ninocss/liquidify/releases/tag/v1.0.0-beta)
   **Liquidify-Manager-1.0.0-beta.apk** installieren.
2. Im Liquidify Manager TIDAL patchen. Manager und Patch-Updates kommen aus
   `ninocss/liquidify`, einschließlich Beta-Releases.
3. Die voreingestellten Liquidify-Patches auswählen, installieren und bei TIDAL
   anmelden. Alternativ `patches.zip` unverändert als Custom-Patchset importieren.

Der Manager zeigt Liquidify-Patches standardmäßig an. Radiant-Lyrics-Patches sind
nur sichtbar, wenn ihr Manifest die Kompatibilität mit einem enthaltenen
Liquidify-Patch ausdrücklich erklärt und ihre Abhängigkeiten verfügbar sind.
Sie bleiben optional und standardmäßig ausgeschaltet. Ausgeblendete Patches und
ihre Erweiterungen werden auch beim Patchen ausgeschlossen.

Der Release-Manager verwendet einen eigenen dauerhaften Signierschlüssel.
Frühere lokal debug-signierte Manager benötigen gegebenenfalls eine separate
Neuinstallation. Die interne Paket-ID bleibt aus Kompatibilitätsgründen erhalten.

## Linn und geprüfter Umfang

Die echte Majik DSM wurde ausschließlich mit bestätigter Hardware-Stummschaltung
getestet: Queue, Transport, Seek, Lautstärketasten, Shuffle/Repeat,
Wiederverbinden und pausierte Rückgabe. Zwei automatische Titelwechsel liefen
bei beendeter Controller-App und ausgeschaltetem Emulatorbildschirm weiter.
Der ursprüngliche Anlagenzustand wurde anschließend vollständig wiederhergestellt.

Diese Hardware-Nachweise stammen aus Alpha22. Die Beta ergänzt insbesondere
Branding, Release-Verteilung und die strengere Patch-Auswahl. Akustische Qualität,
physisches Android-Doze, TalkBack und alternative Receiver sind nicht zertifiziert.
Beim Sleep Timer wurde eine Verzögerung von etwa zwei Sekunden in den nächsten
Titel beobachtet. Details und Einrichtung: [LINN_OUTPUT.md](LINN_OUTPUT.md).

## Quellen und Releases

Das Repository enthält Manager- und Patch-Quellen, ABI-Stubs, Tests, Lizenzen und
das vorbereitete, mit Prüfsummen versehene `patches/patches.zip`. Der gemeinsame
[Release-Workflow](.github/workflows/release.yml) testet und baut den Manager,
prüft das Patchset und veröffentlicht APK, `patches.zip`, `data.json` und
`SHA256SUMS` gemeinsam als Beta. TIDAL-APKs, Kontodaten, Forschungsdateien,
Build-Caches und private Schlüssel werden nicht eingecheckt.

Build-Anleitung: [BUILDING.md](BUILDING.md). Versions-Pins:
[upstream-lock.json](upstream-lock.json).

## Credits und Lizenz

- [Radiant Lyrics](https://github.com/meowarex/rl-mobile): Manager und originale optionale Patches.
- [Square](https://github.com/Lelonio/Square): Design-Referenz und Android-Adaption.
- [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass): Glass-Material.
- Inter: Schrift unter SIL Open Font License.

GPL-3.0 und Hinweise: [LICENSE](LICENSE), [licenses](licenses),
[Radiant Third-Party Notices](radiant-base/THIRD-PARTY.md).
