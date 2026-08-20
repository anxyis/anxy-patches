# [1.2.0](https://github.com/anxyis/anxy-patches/compare/v1.1.0...v1.2.0) (2026-08-20)


### Features

* **daemon:** integrate PopupDismisser background runtime daemon to suppress all dynamic dialogs ([fbe3edb](https://github.com/anxyis/anxy-patches/commit/fbe3edbc27d5e6a4801671ed9f3d76649831be67))

# [1.1.0](https://github.com/anxyis/anxy-patches/compare/v1.0.1...v1.1.0) (2026-08-20)


### Features

* **alightmotion:** add dedicated AM Pro 5.0.273.1028420 suite and graceful native guards ([ccd7637](https://github.com/anxyis/anxy-patches/commit/ccd7637941c8187879246e2f30265548636704a8))

## [1.0.1](https://github.com/anxyis/anxy-patches/compare/v1.0.0...v1.0.1) (2026-08-20)


### Bug Fixes

* **build:** embed classes.dex and Morphe manifest attributes into MPP bundle ([17c8e66](https://github.com/anxyis/anxy-patches/commit/17c8e6621853ba5f93056b5afec688f2464beb63))

# 1.0.0 (2026-08-20)


### Bug Fixes

* **build:** add GitHubPackages Morphe registry repository ([b1e32c7](https://github.com/anxyis/anxy-patches/commit/b1e32c7c629556e48c3221d50fa38fa868bcfdcf))
* **ci:** add package-lock.json and configure Node 22 for semantic-release ([df0eb54](https://github.com/anxyis/anxy-patches/commit/df0eb54cc8bc4092f7dca373a694bd4d8c8bb57d))
* **ci:** make gradlew executable on Linux runners ([8f4c421](https://github.com/anxyis/anxy-patches/commit/8f4c421b65b968b92df6296c3e1dc2f410b0dbe4))


### Features

* Initial release of anxy-patches for Morphe Manager ([9d6a198](https://github.com/anxyis/anxy-patches/commit/9d6a198c4e429f37d3df68bbad298c5317201420))

# Changelog

All notable changes to this project will be documented in this file.

## 1.0.0 (2026-08-20)

### Features
- Initial release of `anxy-patches` for Morphe Manager.
- Comprehensive popup suppression patch suite for **After Motion Z+ / Alight Motion** (`5.0.273`).
- Injected `NoPopupSeedProvider` and `PopupDismisser` runtime background daemon extension.
- Native AArch64 binary NOP patch for `libsatriyaid.so`.
- Smali bytecode suppression for `fq.ab`, `zzzb.vbd`, `zzzb.vwp`, `zzzb.uio`, and `zzw.xyz`.
