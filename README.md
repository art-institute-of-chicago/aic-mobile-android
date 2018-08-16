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
our modifications to those, if any, are licensed under the AGPL as well
License unless otherwise indicated.

The following modules use proprietary APIs:
* The `map` module includes Google's "`GoogleMap`" and "`LatLng`"
  classes.
* The `analytics` module connects to Google Analytics
