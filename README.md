# IHMC Java Decklink Capture

[ ![ihmc-java-decklink-capture](https://maven-badges.herokuapp.com/maven-central/us.ihmc/ihmc-java-decklink-capture/badge.svg?style=plastic)](https://maven-badges.herokuapp.com/maven-central/us.ihmc/ihmc-java-decklink-capture)

A simple JNI library that uses the Blackmagic Decklink SDK to capture video only to a MJPEG file. 
capture control is provided trough a JNI interface

## Requirements


### Ubuntu 20.04
Get "Desktop Video 12.1" from [https://www.blackmagicdesign.com/support/family/capture-and-playback](https://www.blackmagicdesign.com/support/family/capture-and-playback).

Install avcodec dependencies. On ubuntu 20.04 run

```
apt install libavformat58 libavcodec58 libswscale5
```

Native libraries are provided for
- Ubuntu 20.04

### Ubuntu 18.04
Get "Desktop Video 11.2" from [https://www.blackmagicdesign.com/support/family/capture-and-playback](https://www.blackmagicdesign.com/support/family/capture-and-playback).

Install avcodec dependencies. On ubuntu 18.04 run

```
apt install libavformat57 libavcodec57 libswscale4
```

Native libraries are provided for
- Ubuntu 18.04


### Ubuntu 16.04/16.10
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

Download the matching Desktop Video SDK from [https://www.blackmagicdesign.com/support/family/capture-and-playback](https://www.blackmagicdesign.com/support/family/capture-and-playback). For Ubuntu 20.04, download version 12.1, for 18.04 version 11.2, and for 16.04 version 10.8.5.


Place the Blackmagic Desktop Video SDK in the root folder of this distribution. It will get unzipped by the build file. The SDK is not included due to licensing issues.
```
apt-get install libavformat-dev libavcodec-dev libswscale-dev
mkdir build
cd build
cmake ..
make 
make install
```

A new version is now placed in resources/us/ihmc/javaDecklink/lib. Note the version as in libJavadecklink[version].so. 

Go in src/us/ihmc/javadecklink/Capture.java and add this version to LIBAV_SUPPORTED_VERSIONS.

Note: Known incompatibility with gcc-9. Compatible with ggc-5.
