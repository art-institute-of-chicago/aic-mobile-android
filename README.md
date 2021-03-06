![Art Institute of Chicago](https://raw.githubusercontent.com/Art-Institute-of-Chicago/template/master/aic-logo.gif)

[![CircleCI](https://circleci.com/gh/art-institute-of-chicago/aic-mobile-android/tree/dev.svg?style=svg)](https://circleci.com/gh/art-institute-of-chicago/aic-mobile-android/tree/dev)

# Art Institute of Chicago Official Mobile App

> A digital guide to the Art Institute of Chicago. Built with Kotlin 1.x
> for Android.

*Please see
[this page](https://github.com/art-institute-of-chicago/aic-mobile-ios)
for the iOS counterpart.*

The Art Institute of Chicago app is your personal, pocket-sized guide to
our collection. The mobile experience offers high-quality audio
storytelling, letting the art speak to you. The Art Institute offers
nearly a million square feet to explore—the Official Mobile App will be
your guide.

Note that the codebase is young, and under heavy development. It might
change significantly before the first official release. Nevertheless, we
welcome suggestions and contributions from outside parties.

## License

This codebase is licensed under the GNU Affero General Public License v3
as defined in the [top-level LICENSE file](LICENSE). We make heavy use
of the Android support libraries and the Kotlin programming language -
our modifications to those, if any, are also licensed under the AGPL,
 unless otherwise indicated.

The following modules use proprietary APIs:
* The `map` module includes Google's "`GoogleMap`" and "`LatLng`"
  classes.
* The `analytics` module connects to Google Analytics

## Project architecture

The source code is organized into multiple
[Gradle](https://docs.gradle.org) modules, each of which is contained
within a top-level directory. A full list of such modules can be found
in [the `settings.gradle` config file](settings.gradle).

The project maintains an overview of the more interesting modules in
[MODULES.md](MODULES.md).

## How to Build

This project should build perfectly as is using the standard build
commands, but is unlikely to function properly without key attributes
defined in the build process.

Details below:

In .circleci/ you will find a `config.yml` which controls our circle ci
build process.

```aidl
./gradlew assembleDebug assembleRelease
```

### Environment Variables

There is one hidden aspect of the build process not available for public
consumption which is the keystore and keys used in the app. Due to
infrastructure difference between our public build server and release of
the app we handle injecting these variables into our build process by
using the method `envVariable(key, isRelease)` within the gradle files.

This function loads all the appropriate values for the keys from one of
three places depending on where you define them. You can see the full
set of values needed in `env.sample`.

These values are loaded from one of the following sources, listed here
in decreasing order of priority:

* release.env or debug.env (Deployments use this model)
* local.properties (Development uses this model primarily)
* environment variables (Circle CI, uses this model)

Sample (added for reference as well)
```aidl
blob_url=
default_buy_url=
member_validation_url=
member_validation_token=
fb_project_id=
fb_application_id=
fb_api_key=
fb_storage_bucket=
ga_tracking_id=
gcm_sender_id=
google_maps_api_key=
keystore=
keystore_password=
keystore_alias=
keystore_alias_password=
```

### Bundling App Data as JSON (_debug builds only_)

As a special case, if `blob_url` is not defined or is not a string
starting with `https://`, the app can run in a reduced capacity. This
feature is intended for `debug` configurations only and thus does
nothing in release builds.

If a JSON file
1. was present in either `db/src/main/assets/` or `db/src/debug/assets/`
at build time
2. had the name `app-data-v3.json`
and
3. conforms to the expected format of `ArticAppData`

then the content of that JSON file will overwrite whatever content is
currently in the database.
