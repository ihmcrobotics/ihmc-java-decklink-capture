cmake_minimum_required(VERSION 2.8)
project(JavaDecklinkAPI)

include_directories(${JNI_INCLUDE_DIRS} "${SDK_PATH}/include" ${AVLIB_INCLUDE_DIRS} ${Boost_INCLUDE_DIRS})

#Capture.cpp Config.cpp $(SDK_PATH)/DeckLinkAPIDispatch.cpp $(CFLAGS) $(LDFLAGS)

STRING(REGEX REPLACE "^([0-9]+)\\.[0-9]+\\.[0-9]+" "\\1" LIBAV_MAJOR "${AVLIB_libavcodec_VERSION}")
SET(LIBNAME JavaDecklink${LIBAV_MAJOR})
add_library(${LIBNAME} SHARED Capture.cpp "${SDK_PATH}/include/DeckLinkAPIDispatch.cpp")

target_link_libraries(${LIBNAME} m dl pthread ${AVLIB_LIBRARIES} ${Boost_LIBRARIES})



INSTALL(TARGETS ${LIBNAME} RUNTIME DESTINATION lib LIBRARY DESTINATION lib)
