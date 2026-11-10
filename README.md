<!-- markdownlint-disable -->
> [!WARNING]
> Starting from **September 1st 2026** old Spotify credentials will be revoked. Users on version **< 1.8.0** will no longer be able to stream audio.
> To keep streaming audio, update the app and follow [guides](https://github.com/iTomKo/Outify/blob/master/docs/MIGRATING.md).

<div align="center">
  <a href="https://github.com/iTomKo/Outify">
    <img src="./.github/logo.svg" alt="Outify" width="150">
  </a>
  <h1 align="center">
    Outify
  </h1>
  <p>
    <br />
    <strong>
      Implementation of 
      <a href="https://github.com/librespot-org/librespot/">librespot</a>
      for Spotify streaming 
    </strong>
  </p>

  <p>
    <a href="https://github.com/iTomKo/Outify/issues/new?assignees=&labels=bug&projects=&template=bug_report.yml">Report Bug</a>
    ·
    <a href="https://github.com/iTomKo/Outify/issues/new?template=feature_request.md">Request Feature</a>
    ·
    <a href="https://github.com/iTomKo/Outify/discussions/new?category=q-a">Ask Question</a>
  </p>

  <br />

  ![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?&style=for-the-badge&logo=kotlin&logoColor=white)
  ![Android](https://img.shields.io/badge/Android-34A853?style=for-the-badge&logo=android&logoColor=white)
  ![Rust](https://img.shields.io/badge/Rust-000000?style=for-the-badge&logo=rust&logoColor=white)

  [![GitHub License](https://img.shields.io/github/license/iTomKo/Outify?style=for-the-badge&label=%20)](https://www.gnu.org/licenses/gpl-3.0)
  [![GitHub Issues or Pull Requests](https://img.shields.io/github/issues/iTomKo/Outify?style=for-the-badge)](https://github.com/iTomKo/Outify/issues)
  [![Weblate project translated](https://img.shields.io/weblate/progress/outify?server=https%3A%2F%2Ftranslate.codeberg.org&style=for-the-badge)](https://translate.codeberg.org/engage/outify/)
</div>

### Outify Nightcore Mod
<img src="docs/images/nightcore-player.jpg" alt="Player with the speed slider on the album art" width="220" align="right">

This is a fork of [iTomKo/Outify](https://github.com/iTomKo/Outify), currently based on **Outify 2.0.0**. It changes the following:

- **Nightcore playback speed**: speed changes tempo *and* pitch together, like speeding up a record, instead of pitch-preserving time-stretch
- **Speed slider**: an always-visible 0.5x – 3x slider on the album art (tap the label to reset to 1x)
- **Speed is remembered** across app restarts
- **Outify is the default playback device**: tapping a song, or pressing play / next / previous / seek, plays on this phone, even if another Spotify device is still listed as active
- **Playback stability fixes**: no random stops from an audio-focus conflict, no ANR kill while waiting for playback, and no dropouts at high speeds

**[Download the latest release](https://github.com/danielwoww/Outify-Nightcore-Mod/releases/latest)**. It uses its own signing key, so uninstall the official Outify app first.

See [docs/NIGHTCORE-MOD.md](./docs/NIGHTCORE-MOD.md) for details, build and release instructions, and how to merge upstream updates. The links in the header and below go to the original Outify project. Please don't report problems that only happen in this mod there.

<br clear="right"/>

### Information
Third party open source Android Spotify client with Material 3 using librespot Rust

> [!WARNING]
> Outify is still in early development.
> Any contributions are welcomed.

> [!NOTE]
> Outify requires premium Spotify account!
> No support will be provided for non-premium users.

### Features
Outify is based on librespot backend allowing us to stream Spotify audio.

- Searching Spotify
- Streaming S16 audio
- Viewing playlists, albums, artists, your library
- Sleek Material 3 design
- Dynamic Material Theme

### Contributing
Please take a look at [CONTRIBUTING.md](https://github.com/iTomKo/Outify/blob/master/docs/CONTRIBUTING.md)

### Help & Support
Contact us through Github:
- via [issues](https://github.com/iTomKo/Outify/issues) for reports, feature requests, bug reports, ..
- via [discussions](https://github.com/iTomKo/Outify/discussions) for help with the application.

### Roadmap
- [x] raw PCM streaming
- [x] adding to queue
- [x] starting radio
- [x] playlist support
    - [x] playing and viewing playlist
    - [x] modifying playlist
- [x] interacting with spotify account
    - [x] login to Spotify Web API
- [ ] jams
    - [x] starting, ending and inviting
    - [ ] joining jams
- [ ] offline support
- [x] media notification
- [x] keep alive lifecycle

### Screenshots
<p>
    <img src="docs/images/playerscreen.png" alt="Player interface" width="200" hspace="10"/>
    <img src="docs/images/lyrics.png" alt="Player interface" width="200" hspace="10"/>
    <img src="docs/images/artist.png" alt="Artist view" width="200" hspace="10"/>
    <img src="docs/images/liked.png" alt="Liked view" width="200" hspace="10"/>
</p>

[View entire gallery](./docs/images/)

### Translation
We use Weblate to translate *Outify*. For more details or to submit translations, visit our [Weblate page](https://translate.codeberg.org/engage/outify/).
[![Translation status](https://translate.codeberg.org/widget/outify/multi-auto.svg)](https://translate.codeberg.org/engage/outify/)

### Attribution
[librespot-org/librespot](https://github.com/librespot-org/librespot) for providing the required backend

[PixelPlay](https://github.com/theovilardo/PixelPlayer/) for UI, UX inspiration

[OuterTune](https://github/OuterTune/OuterTune) for UI, UX inspiration

Google for Jetpack Compose, Material Components and Icons

### Disclaimer
Outify is not affiliated with Spotify, Google or librespot in any way. Usage of this app **can** be against Spotify ToS.
Use at your own risk.

Made with ❤️ by TomKo

