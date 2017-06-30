# README

## beaTunes plugin that extracts acoustic features and submits them to AcousticBrainz

This plugin requires [beaTunes](http://www.beatunes.com/) 5.0.0 or later.
It will not work with earlier versions.


## Installation

To install, place the jar (`abzsubmit-x.y.z.jar`) into beaTunes'
plugin folder, remove older versions of the plugin and restart beaTunes.

- Windows: `c:\Users\[username]\AppData\Local\tagtraum industries\beaTunes\plugins`
- macOS: `~/Library/Application Support/beaTunes/Plug-Ins`

When installed, a new task is available in the Analysis Options dialog.


## Building

To compile from source, install [Maven 3](http://maven.apache.org/) and execute

    mvn clean install

from the directory that the file `pom.xml` is located in.
You will find the resulting jar file in the `target` subdirectory.


## License

This plugin bundles both a macOS and a Windows binary of the AcousticBrainz `streaming_extractor_music`.
The original binaries are available from the [AcousticBrainz website](https://acousticbrainz.org/download).
Source code is available on [GitHub](https://github.com/MTG/acousticbrainz-client).

Note that the software provided by AcousticBrainz contains open source code from a number
of other projects licensed under different licenses. Namely:

- [libav](https://libav.org/download/) (LGPL)
- [taglib](http://taglib.org) (LGPL)
- [FFTW](http://www.fftw.org) (GPL)
- [libyaml](http://pyyaml.org/wiki/LibYAML) (MIT)
- [libsamplerate](http://www.mega-nerd.com/SRC/) (BSD)

The plugin simply calls the AcousticBrainz executable, but does not incorporate its code
in any other way.

The plugin itself is in the [public domain](https://creativecommons.org/share-your-work/public-domain/cc0/).


## More

For change notes and other plugin-specific infos, please see the plugin descriptor
[`src/main/resources/META-INF/plugin.xml`](https://raw.githubusercontent.com/beatunes/plugin-samples/master/similarsongstable/src/main/resources/META-INF/plugin.xml).
For more info on developing plugins, please visit http://www.beatunes.com/beatunes-plugin-api.html

*Enjoy.*
