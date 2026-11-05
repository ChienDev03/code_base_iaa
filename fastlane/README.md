fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android firebase_distribution

```sh
[bundle exec] fastlane android firebase_distribution
```

Build a signed release APK, distribute it with Firebase, then notify Notion and Discord.

### android publish_current_release_notifications

```sh
[bundle exec] fastlane android publish_current_release_notifications
```

Re-publish Notion and Discord notifications for the latest Firebase release.

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
