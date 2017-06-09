# README

## beaTunes inspection that finds songs with the same artist, but a different grouping

This plugin requires [beaTunes](http://www.beatunes.com/) 5.0.0 or later.
It will not work with earlier versions.

If you'd like to understand the code, start with studying the class
`SameArtistDifferentGroupingInspector` in `src/main/java/...`.

## Installation

To install, place the jar (`sameartistdifferentgrouping-x.y.z.jar`) into beaTunes'
plugin folder, remove older versions of the plugin and restart beaTunes.

- Windows: `c:\Users\[username]\AppData\Local\tagtraum industries\beaTunes\plugins`
- macOS: `~/Library/Application Support/beaTunes/Plug-Ins`


## Building

To compile from source, install [Maven 3](http://maven.apache.org/) and execute

    mvn clean install

from the directory that the file `pom.xml` is located in.
You will find the resulting jar file in the `target` subdirectory.


## More

For change notes and other plugin-specific infos, please see the plugin descriptor
[`src/main/resources/META-INF/plugin.xml`](https://raw.githubusercontent.com/beatunes/plugin-samples/master/sameartistdifferentgrouping/src/main/resources/META-INF/plugin.xml).
For more info on developing plugins, please visit http://www.beatunes.com/beatunes-plugin-api.html

Enjoy.
