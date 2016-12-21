# README

## beaTunes plugin to derive moods from AcousticBrainz.org

This plugin requires [beaTunes](http://www.beatunes.com/) 4.6.9 or later,
but most likely won't work with beaTunes 5.


## Installation

To install, place the jar into beaTunes' plugin folder,
remove older versions of the plugin and restart beaTunes.

- Windows: `c:\Users\[username]\AppData\Local\tagtraum industries\beaTunes\plugins`
- OS X: `~/Library/Application Support/beaTunes/Plug-Ins`


## Building

To compile from source, install [Maven 3](http://maven.apache.org/) and execute

    mvn clean install

from the directory that the file `pom.xml` is located in.
You will find the resulting jar in the `target` subdirectory.


## More

For change notes and other plugin-specific infos, please see the plugin descriptor
[`src/main/resources/META-INF/plugin.xml`](https://raw.githubusercontent.com/beatunes/plugin-samples/master/acousticbrainzmood/src/main/resources/META-INF/plugin.xml).
For more info on developing plugins, please visit http://www.beatunes.com/beatunes-plugin-api.html

Enjoy.