# ESL Pod Client

This is a hobby project to learn some concepts and tools, including:

 - Kotlin language
 - MVP pattern with RxJava (not RxKotlin)
 - Dagger2
 
The app uses jsoup to gather the podcasts of ESL Podcast website and allowing the user to store and listen any podcast with streaming or using local storage.

It uses a combination of MediaPlayer and MediaPlayback for streaming audio and DownloadManager to handle the download complexity.

# TODO

 - Find a way to proxy the MediaPlayer with a local webserver (NanoHTTPD) and store the content of the podcast while it is being played.
     - The implementation may be hard because the user can seek to any position of audio. So the proxy must handle gaps in the downloaded file and must be able to request correctly.

----

## Dependencies

To run this project you need to have:

 - JDK 7
 - Android Studio 2.0 or higher
 - Kotlin plugin for Android Studio
 

---

## Setup the project

1. Install the dependencies above
2. `$ git clone https://github.com/wakim/esl-pod-client.git` - Clone the project
3. `$ cd esl-pod-client` - Go into the project folder
4. Open Android Studio
5. Click "Import project (Eclipse ADT, Gradle, etc.)"
6. Build the project to see if everything is working fine
