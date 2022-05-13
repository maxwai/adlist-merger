![Java](https://badgen.net/badge/language/Java/green)
![Java](https://badgen.net/badge/Java/JDK-17/green)
[![GitHub license](https://badgen.net/github/license/maxwai/adlist-merger)](LICENSE)

# Ad List Merger Tool

Tool to merge a lot of Ad Lists for pihole together while removing duplicates

## Getting Started

The Tool needs a `config.xml` in the appdata folder. An example config file will be created if none
is present.

The Tool expects a `git_folder` folder in the project root where the files will be created and
updated. Be sure to already have a working git folder there were a push works and doesn't need any
username/password input from the user.

### Prerequisites

You will need Java Version 17 or later to make it work. It may work with lower Java versions, but it
was programmed using the Java 17 JDK.