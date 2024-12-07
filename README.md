# Anytype

Forked Customized Anytype client for Android.

## WARNING: My changes to this Client are highly experimental and dangerous!!

I am not responsible, if the usage of this forked client corrupts your data in any way. I never do any QC before making commits here!! So do be careful and carefully read the changes I've made in the codebase itself. Spoilers (It's badly done). If you still want to try it, make sure to regularly back up your data.

Several changes includesL
- Forced support for screenOrientation="fullSensor" in manifest value. (Meaning it can now allow landscape mode, hurray for Tablet users!)
- Latex Embed Block sort of (some of the existing code base, actually had Latex built in already, so I lazily copy/pasted some code, and added some more lines of code and files here and there. It seems to work and sync properly, but the encoding from plain text to Latex doesn't work 100% of the time)
- That's all for now. I am not an experienced Android Developer by any means. So expect some things to break and crash when building or running this client. Thanks for reading.

## Build project

### Prerequisites

- Clone this repository
  
- Create `github.properties` file in root project folder:

    ```
    gpr.usr=GITHUB_USER_ID
    gpr.key=GITHUB_PERSONAL_ACCESS_TOKEN
    ```

Your Github ID (starting with '#' character) can be found [here](https://caius.github.io/github_id/). As to your personal Github access token, you can generate it in `Developer settings` in your profile settings on Github. 

- Create `apikeys.properties` file in root project folder:

    ```
    amplitude.debug="AMPLITUDE_DEBUG_KEY"
    amplitude.release="AMPLITUDE_RELEASE_KEY"
    sentry_dsn="SENTRY_DSN_KEY"
    ```

Then build project.

### IDE setup (optional) 

Based on your IDE setup, you might experience problems while accessing/importing auto-generated Kotlin classes from `protocol` module. These classes are currently declared in very large files, which IDE might not process as expected. If this is your case, go to your Android Studio _Help_ section, select _Edit Custom Properties_ option. There you should set `idea.max.intellisense.filesize` property as follows:

```
idea.max.intellisense.filesize=3500
```

## Android Emulator setup (optional)

After a successful build, and being able to launch the client. There might be a crash after trying to create a new vault for the first time or when trying to enter an existing one. This might be because of a SELinux error. Open the terminal and enter the following:
```
adb shell
su 0 setenforce 0
setprop ctl.restart zygote
```

## Useful links

[Our tech change log](https://github.com/anyproto/anytype-kotlin/blob/main/CHANGELOG.md)

[Anytype Android app dependencies](https://github.com/anyproto/anytype-kotlin/blob/main/gradle/libs.versions.toml)

## Conventions

### PR naming
```
{TASK-ID} {APP AREA} | {NATURE OF CHANGE: Fix, Enhancement, Feature, Design, Documentation} | {CONCISE DESCRIPTION OF WHAT HAS BEEN DONE}
```

App area can be `App`, `Editor`, `Sets`, `Relations`, `Auth`, `Settings`, `Analytics`, `Tech` (CI,
DI, scripting, etc.), etc.

Example: *Editor | Fix | Show meaningful message when failed to open file by an existing
application*

### Git branch naming

```
{TASK_ID or TASK_NUMBER}-ConciseDescription
```

### Flags

```
com.anytype.ci=true - for CI/CD pipeline
```

## Updating anytype-heart (basics)

Prerequisite: `brew install jq`

1. Run the following command in Terminal or Makefile:

    ```
    make update_mw
    ```

2. Make sure your proto files located in `protocol/main/proto/` compile.
3. Make sure to update `middlewareVersion` version in `libs.versions.toml`.

## Contribution
Thank you for your desire to develop Anytype together!

❤️ This project and everyone involved in it is governed by the [Code of Conduct](docs/CODE_OF_CONDUCT.md).

🧑‍💻 Check out our [contributing guide](docs/CONTRIBUTING.md) to learn about asking questions, creating issues, or submitting pull requests.

🫢 For security findings, please email [security@anytype.io](mailto:security@anytype.io) and refer to our [security guide](docs/SECURITY.md) for more information.

🤝 Follow us on [Github](https://github.com/anyproto) and join the [Contributors Community](https://github.com/orgs/anyproto/discussions).

---
Made by Any — a Swiss association 🇨🇭

Licensed under [Any Source Available License 1.0](./LICENSE.md).
