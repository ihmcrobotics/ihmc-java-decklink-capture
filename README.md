# IHMC Java Decklink Capture

A simple JNI library that uses the Blackmagic Decklink SDK to capture video only to a MJPEG file. 
capture control is provided trough a JNI interface

## Requirements

Get "Desktop Video 10.8.5" from [https://www.blackmagicdesign.com/support/family/capture-and-playback](https://www.blackmagicdesign.com/support/family/capture-and-playback).

Install avcodec dependencies. On ubuntu run
'''
apt-get install libavformat-ffmpeg56 libavcodec-ffmpeg56 libswscale-ffmpeg3
'''

Native libraries are provided for
- Ubuntu 16.04
- Ubuntu 16.10

Libraries are tested one by one in the runtime till one loads. 


## Compilation

The Blackmagic SDK is included in the distribution and gets  unzipped by the build file.
```
apt-get install libavformat-dev libavcodec-dev libswscale-dev libboost-1.58-all-dev openjdk-8-jdk
mkdir build
cd build
cmake ..
make 
make install
```

if cmake complains about Java include directories make sure openjdk-8-jdk is installed and then export JAVA\_HOME (e.g. `export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64`)

A new version is now placed in resources/us/ihmc/javaDecklink/lib. Note the version as in libJavadecklink[version].so. 

Go in src/us/ihmc/javadecklink/Capture.java and add this version to LIBAV_SUPPORTED_VERSIONS.
