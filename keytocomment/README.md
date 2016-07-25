# README

## beaTunes plugin to copy Key to Comment

This plugin requires [beaTunes](http://www.beatunes.com/) 4.6 or later.
It will not work with earlier versions.

It is purely meant as a demo plugin, as its functionality is already built into beaTunes
(starting with [version 4.5](http://blog.beatunes.com/2015/08/looking-good-beatunes-45.html)).


## Installation

To install, place the jar (`keytocomment-3.0.2.jar`) into beaTunes'
plugin folder, remove older versions of the plugin and restart beaTunes.

- Windows: `c:\Users\[username]\AppData\Local\tagtraum industries\beaTunes\plugins`
- OS X: ``~/Library/Application Support/beaTunes/Plug-Ins`


## Building

To compile from source, install [Maven 3](http://maven.apache.org/) and execute

    mvn clean install

from the directory that the file `pom.xml` is located in.
You will find the resulting jar file in the `target` subdirectory.


## More

For change notes and other plugin-specific infos, please see the plugin descriptor
[`src/main/resources/META-INF/plugin.xml`](https://raw.githubusercontent.com/beatunes/plugin-samples/master/keytocomment/src/main/resources/META-INF/plugin.xml).
For more info on developing plugins, please visit http://www.beatunes.com/beatunes-plugin-api.html

Enjoy.
