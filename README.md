# IHMC Java Decklink Capture

A simple JNI library that uses the Blackmagic Decklink SDK to capture video only to a MJPEG file. 
capture control is provided trough a JNI interface

## Compilation

Get the Blackmagic SDK for Linux and unpack somewhere.
```
mkdir build
cd build
cmake -DSDK_PATH=[Blackmagic SDK]/Linux -DCMAKE_INSTALL_PREFIX=../resources/us/ihmc/javadecklink ..
make 
make install
```

A new version is now placed in resources/us/ihmc/javaDecklink/lib. Note the version as in libJavadecklink[version].so. 

Go in src/us/ihmc/javadecklink/Capture.java and add this version to LIBAV_SUPPORTED_VERSIONS.
