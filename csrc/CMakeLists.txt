cmake_minimum_required(VERSION 2.8)
project(JavaDecklinkAPI)

include_directories(${JNI_INCLUDE_DIRS} "${SDK_PATH}/include" ${AVLIB_INCLUDE_DIRS} ${Boost_INCLUDE_DIRS})

#Capture.cpp Config.cpp $(SDK_PATH)/DeckLinkAPIDispatch.cpp $(CFLAGS) $(LDFLAGS)

STRING(REGEX REPLACE "^([0-9]+)\\.[0-9]+\\.[0-9]+" "\\1" LIBAV_MAJOR "${AVLIB_libavcodec_VERSION}")
STRING(REGEX REPLACE "^([0-9]+)\\.[0-9]+\\.[0-9]+" "\\1" SWSCALE_MAJOR "${AVLIB_libswscale_VERSION}")
STRING(REGEX REPLACE "^([0-9]+)\\.[0-9]+\\.[0-9]+" "\\1" AVFORMAT_MAJOR "${AVLIB_libavformat_VERSION}")


IF("${SWSCALE_MAJOR}" VERSION_GREATER "2")
	SET(LIBNAME_POSTFIX "")
	IF("${AVLIB_LIBRARIES}" MATCHES "ffmpeg")
		SET(LIBNAME_POSTFIX "-ffmpeg")
	ENDIF()
	
	SET(LIBNAME JavaDecklink-avcodec${LIBAV_MAJOR}-swscale${SWSCALE_MAJOR}-avformat${AVFORMAT_MAJOR}${LIBNAME_POSTFIX})
ELSE()
	SET(LIBNAME JavaDecklink${LIBAV_MAJOR})
ENDIF()

SET(SDK_LIB "${SDK_PATH}/include/DeckLinkAPIDispatch.cpp")
set_source_files_properties("${SDK_LIB}" PROPERTIES GENERATED TRUE)
add_library(${LIBNAME} SHARED Capture.cpp "${SDK_LIB}")

target_link_libraries(${LIBNAME} m dl pthread ${AVLIB_LIBRARIES} ${Boost_LIBRARIES})
add_dependencies(${LIBNAME} DecklinkSDK)



INSTALL(TARGETS ${LIBNAME} RUNTIME DESTINATION lib LIBRARY DESTINATION lib)
