# plugin-samples

Plugin code samples for [beaTunes](https://www.beatunes.com/) 6 in Java.

Each folder is a plugin of its own, with its own `pom.xml` and `README.md`.

To install, build the plugin with [Maven](https://maven.apache.org/) and place the resulting jar in the
beaTunes [plugins directory](https://help.beatunes.com/kb/plugins/how-to-install-a-plugin).

## Building

beaTunes 6 runs on Java 25, so building a plugin needs a JDK 25 or later:

    cd keytocomment
    mvn clean install

The plugins compile against the beaTunes version named by the property `beatunes.version` in
each `pom.xml`. To compile against another version, pass it on the command line, for example
`mvn clean install -Dbeatunes.version=6.0.1`.

## beaTunes 5

These samples work with beaTunes 6.0.0 or later. If you still use beaTunes 5, you can find the
samples as they were for that version under the tag
[beaTunes-5](https://github.com/beatunes/plugin-samples/tree/beaTunes-5).

## Moving a plugin from beaTunes 5 to 6

- **Java 25.** Compile with `<release>25</release>`.
- **`plugin.xml`** has to allow the new version in `<beatunes-version>`, for example
  `<min>6.0.0</min>` and `<max>6.9.9</max>`.
- **`@Entity` is `jakarta.persistence.Entity`**, not `javax.persistence.Entity`. Analysis tasks
  need that annotation.
- **The AppleScript session** is `com.tagtraum.japlscript.execution.Session`, started with
  `Session.startSession()`.
- **Jython is gone**, and with it `org.python.core`. beaTlets are written in Groovy or JRuby.

For more info about beaTunes plugin development in general, please see
[here](https://www.beatunes.com/en/beatunes-plugin-api.html).

Help and support is available at https://help.beatunes.com/

Enjoy.
