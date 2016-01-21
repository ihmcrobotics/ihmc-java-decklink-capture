cmake_minimum_required(VERSION 2.8)
project(javadecklinkapi)

IF(SDK_PATH)
	IF(NOT EXISTS "${SDK_PATH}/include/DeckLinkAPI.h")
		MESSAGE(FATAL_ERROR "${SDK_PATH}/include/DeckLinkAPI.h does not exists. Pass in a valid non-relative SDK_PATH with -DSDK_PATH=")
	ENDIF()
	MESSAGE("Decklink SDK_PATH is ${SDK_PATH}")
ELSE()
	MESSAGE(FATAL_ERROR "SDK_PATH is not set. Pass in the path to the Decklink SDK with -DSDK_PATH=")
ENDIF(SDK_PATH)
IF(CMAKE_INSTALL_PREFIX_INITIALIZED_TO_DEFAULT)
  SET(CMAKE_INSTALL_PREFIX
    ${CMAKE_SOURCE_DIR}/resources/us/ihmc/javadecklink CACHE PATH "Java Decklink API install prefix" FORCE
    )
ENDIF(CMAKE_INSTALL_PREFIX_INITIALIZED_TO_DEFAULT)

set(CMAKE_BUILD_TYPE RelWithDebInfo)

find_package(PkgConfig REQUIRED)
pkg_check_modules(AVLIB REQUIRED libswscale libavcodec libavformat)

find_package(Boost COMPONENTS thread)

find_package(JNI REQUIRED)

add_subdirectory(csrc)
