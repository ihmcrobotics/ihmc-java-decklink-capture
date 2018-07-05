cmake_minimum_required(VERSION 3.1)
project(javadecklinkapi)

SET(DECKLINK_VERSION "10.11")
set (CMAKE_CXX_STANDARD 11)



IF(CMAKE_INSTALL_PREFIX_INITIALIZED_TO_DEFAULT)
  SET(CMAKE_INSTALL_PREFIX
    ${CMAKE_SOURCE_DIR}/resources/us/ihmc/javadecklink CACHE PATH "Java Decklink API install prefix" FORCE
    )
ENDIF(CMAKE_INSTALL_PREFIX_INITIALIZED_TO_DEFAULT)

set(CMAKE_BUILD_TYPE RelWithDebInfo)

SET(SDK_PREFIX "${PROJECT_BINARY_DIR}/blackmagic")
SET(SDK_PATH "${SDK_PREFIX}/Linux")
include(ExternalProject)
ExternalProject_Add(DecklinkSDK
	    SOURCE_DIR "${SDK_PREFIX}"
	    STAMP_DIR "${PROJECT_BINARY_DIR}/blackmagic-stamp"
		URL "${PROJECT_SOURCE_DIR}/Blackmagic_DeckLink_SDK_${DECKLINK_VERSION}.zip"
		CONFIGURE_COMMAND ""
		BUILD_COMMAND ""
		INSTALL_COMMAND ""
	    
)	    


find_package(PkgConfig REQUIRED)
pkg_check_modules(AVLIB REQUIRED libswscale libavcodec libavformat libavutil)

find_package(Boost COMPONENTS thread)

find_package(JNI REQUIRED)

add_subdirectory(csrc)
