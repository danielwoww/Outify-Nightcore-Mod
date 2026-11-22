<div align="center">
  <img src="./.github/logo.svg" alt="Outify Nightcore Mod" width="150">
  <h1>Outify Nightcore Mod</h1>
  <p>
    <strong>Spotify on Android with nightcore-style playback speed</strong><br />
    A fork of <a href="https://github.com/iTomKo/Outify">Outify</a>, the open source Material 3 Spotify client built on <a href="https://github.com/librespot-org/librespot">librespot</a>
  </p>

  [![Latest release](https://img.shields.io/github/v/release/danielwoww/Outify-Nightcore-Mod?style=for-the-badge&label=release)](https://github.com/danielwoww/Outify-Nightcore-Mod/releases/latest)
  [![Downloads](https://img.shields.io/github/downloads/danielwoww/Outify-Nightcore-Mod/total?style=for-the-badge)](https://github.com/danielwoww/Outify-Nightcore-Mod/releases)
  [![License](https://img.shields.io/github/license/danielwoww/Outify-Nightcore-Mod?style=for-the-badge)](./LICENSE)

  <p>
    <a href="https://github.com/danielwoww/Outify-Nightcore-Mod/releases/latest"><strong>Download the latest APK</strong></a>
    ·
    <a href="https://github.com/danielwoww/Outify-Nightcore-Mod/issues/new/choose">Report a problem</a>
    ·
    <a href="./docs/NIGHTCORE-MOD.md">Technical details</a>
  </p>
</div>

<p align="center">
  <img src="docs/images/nightcore-player.jpg" alt="Player with the speed slider on the album art" width="280">
</p>

## Features

- **Nightcore playback speed**: speeding up raises tempo *and* pitch together, like speeding up a record. Slowing down lowers the pitch the same way.
- **Speed slider on the album art**: always visible, from 0.5x to 3x in 0.05 steps. The speed changes live while you drag. Tap the label to reset to 1x.
- **Speed is remembered** across restarts, track changes and app updates.
- **Clean audio at high speed**: playback is resampled by Android's own audio pipeline, with no dropouts or clicks even at 3x.
- **Reliable playback**: song taps and play / next / previous / seek are never silently ignored because Spotify thinks another device is still active. The app also no longer gets killed while waiting for playback to start.

Everything else from Outify is included and kept up to date: search, playlists, albums, artists and your library, lyrics, queue and radio, Spotify Connect remote control, Jams, a media notification and a dynamic Material 3 theme.

## Install

1. Download the APK from the [latest release](https://github.com/danielwoww/Outify-Nightcore-Mod/releases/latest). `arm64-v8a` fits most phones; `universal` works everywhere.
2. If you have the official Outify app installed, uninstall it first. This fork is signed with its own key, so it can't update Outify in place.
3. Log in with your Spotify account.

Requires **Spotify Premium**.

Speed applies to music playing on your phone. When another device (PC, speaker) is playing, Outify acts as a remote for it. Pick Outify under *Playback devices* to play on your phone.

## Screenshots

<p>
  <img src="docs/images/playerscreen.png" alt="Player" width="200" hspace="10"/>
  <img src="docs/images/lyrics.png" alt="Lyrics" width="200" hspace="10"/>
  <img src="docs/images/artist.png" alt="Artist view" width="200" hspace="10"/>
  <img src="docs/images/liked.png" alt="Liked songs" width="200" hspace="10"/>
</p>

## Building

See [docs/NIGHTCORE-MOD.md](./docs/NIGHTCORE-MOD.md) for build and release instructions, how each feature works, and how upstream updates are merged.

## Credits

- [iTomKo/Outify](https://github.com/iTomKo/Outify) by TomKo, the app this fork is built on. Please report problems with this mod [here](https://github.com/danielwoww/Outify-Nightcore-Mod/issues/new/choose), not to the Outify project.
- [librespot](https://github.com/librespot-org/librespot) for the Spotify streaming backend
- Outify's translators on [Weblate](https://translate.codeberg.org/engage/outify/)

## License and disclaimer

Licensed under [GPL-3.0](./LICENSE), like Outify. Not affiliated with Spotify, Google, librespot or the Outify project. Using this app **can** be against Spotify's Terms of Service. Use at your own risk.
