# Smouldering Durtles

Smouldering Durtles is an Android app for WaniKani, forked from the existing codebase of Flaming Durtles, the popular open source client that has fallen into disprear. I'm Joeni over on the site and took on updating the application to support changes in WaniKani's API, in the hopes that Android users will continue to have access.

The following is the original information regarding licencing and sharing:

It's set up as an Android project with Google's default gradle set up.
Use Android Studio or the Gradle command line to build it.

## Preparing to build the code

Before you can build this code, you will have to provide two files containing identification
information for the app. This is because the open source license covering this app's code
does not cover the name I gave the app, and it also doesn't cover my name. See the file
LICENSE.md for details.

- Copy the file app/Identification.java.sample.txt to app/src/main/java/com.smouldering_durtles.wk
- Name the copy Identification.java
- Edit the file to supply your own identification for the app
- Copy the file app/strings.xml.sample.txt to app/src/main/res/values
- Name the copy strings.xml
- Edit the file to supply your own identification for the app

After this, you are ready to build the code.

