# Application Modules

Each of these corresponds 1:1 with a Gradle module. The full list of
Gradle modules is maintained separately in
[the `settings.gradle` config file](settings.gradle).

## High-level ("consuming") modules:

### The top-level project folder itself

This is the container wherein all other modules are defined. Check out
the default module configuration in [build.gradle](build.gradle).

### `app`

This aggregates all of the bits and pieces from other modules that we
want to include in the build. It also contains our [Dagger 2]() injector
and most of the build-oriented config (application version, minimum
supported android SDK, etc.)

## Mid-level modules (the "Features"):

### `welcome` (occasionally referred to as "home")

The front page of the app. Here we show high-level info about Tours and
what's On View. This module makes good use of RX and the androidX team's
Navigation library - a decent place to get acquainted with codebase
conventions.

### `audio`

This allows entry of those numbers found throughout the museum; if an
objet d'art is found matching that number, we offer up

1. a picture of the object
2. a long, in-depth audio description of the object
3. a transcript of that description

This module depends on `media` and `media_ui`.

### `map`

Map of the museum, with all the buildings, floors, galleries, and
amenities that implies. `MapViewModel` is a good starting point for
understanding the code.

NB: the `MapFragment` class in this module makes extensive use of
Google's `GoogleMap` class to display the museum, but we do not depend
on the Google Map API for map tiles. All floor-plans should be provided
by the `AppData` `DAO` in the `db` module.

There is a testing branch named `mapTesting` which will load up a Debugging `MapTiler` 
so you can see the tiles being rendered. Secondly it also contains an `AssetMapTileProvider`
so that you load your tiles locally and test them prior to uploading them to a server
and using the `GlideMapTileProvider`. 

To help in using the `TileProvider` we have included a script in this module named `Tiler.sh`
This script with take a PDF image and cut it up into the right sizes for google maps at various 
zoom levels

To run this script simply do so as follows:

`sh tiler.sh PDF_FILE FLOOR_NUMBER`

This will generate a folder named `floorFLOOR_NUMBER` where all the zoom level can be found for 
that floor.

### `tours`

Provides custom tours with unique audio content that work in tandem with
the map and guide users on a narrated journey. The raw tour info is
stored separately, in the `db` module.

### `info`

Museum information (like hours and directions), digital membership card,
global settings. This where users configure e.g. their preferred
language and turn location tracking on and off.

## Low-level modules:

### `base`

This contains common resources (colors, images, fonts, styles,
dimensions, etc.) and most of our Kotlin Extensions. Extensions that are
only used in the context of a specific module should be kept in that
module.

### `db`

The database. Much of the dynamic info shown in this application is
defined in a single (somewhat large) JSON file called the `AppData`. It
is downloaded during startup by `AppDataManager` and stored via the
[Room](https://developer.android.com/topic/libraries/architecture/room)
persistence api. `BaseActivity`s and `BaseFragment`s can access this
with implementations of the specialized `DAO` interfaces defined in the
`db` module.

At build time, an annotation processor uses both the `DAO` files and
the directory of model classes to create implementations of these
interfaces. As explained further under the heading for the `viewmodel`
module, these models reflect the `M` in the `M-V-VM` acronym.

`DAO`s (that stands for
[Data Access Objects](https://en.wikipedia.org/wiki/Data_access_object))
can run SQL queries against this content on demand.

### `ui`

This contains our `BaseActivity` and `BaseFragment` classes, essential
building blocks for any new visual component. Any Activity or Fragment
derived from these will have access to affordances for

1. auto-injecting `Dagger 2` dependencies from other modules
2. hooking up toolbars
3. changing the Status Bar color
4. disposing of `RX` resources
5. one or two other minor things

These classes are only to be used in the simplest of scenarios; for
anything remotely interesting the `viewmodel` module builds upon them to
create the more capable `BaseViewModelFragment`/`BaseViewModelActivity`.

### `viewmodel`

Responsibility for the different parts of the app is handled according
to the well-respected
[M-V-VM](https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93viewmodel)
pattern. This module contains useful code for the `VM`, or `ViewModel`
component.

For each Activity, we typically create a primary subclass of
`BaseViewModelFragment` with its own dedicated `BaseViewModel`. That
fragment is added directly to the activity, which in this simplicity is
is now defined as a direct subclass of `BaseActivity`. Thus, as much of
the logic as possible is then kept far away from the Activity.

The responsibilities are split between `ViewModel` and
`BaseViewModelFragment` as follows:

* The `ViewModel` contains `Observable`s and `Observer`s to pull data
  from disparate sources, such as our database (see `db` module for more
  on that)
* The `BaseViewModelFragment` ensures that all of the data actually gets
  into our View layer

### `media`

This contains the `AudioPlayerService`, which allows background playback
of audio guides and other such sound.

### `media_ui`

Basic visual extensions to the `media` module. This contains a prebuilt
`NarrowAudioPlayerFragment` class; most screens in the app will be fine
using that class and not referencing `AudioPlayerService` directly.
