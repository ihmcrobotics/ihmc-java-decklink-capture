cmake_minimum_required(VERSION 2.6)
project(JavaDecklinkAPI)

include_directories(${JNI_INCLUDE_DIRS} "${SDK_PATH}/include" ${AVLIB_INCLUDE_DIRS} ${Boost_INCLUDE_DIRS})

#Capture.cpp Config.cpp $(SDK_PATH)/DeckLinkAPIDispatch.cpp $(CFLAGS) $(LDFLAGS)

add_library(JavaDecklink SHARED Capture.cpp "${SDK_PATH}/include/DeckLinkAPIDispatch.cpp")

target_link_libraries(JavaDecklink m dl pthread ${AVLIB_LIBRARIES} ${Boost_LIBRARIES})



INSTALL(TARGETS JavaDecklink RUNTIME DESTINATION lib LIBRARY DESTINATION lib)
