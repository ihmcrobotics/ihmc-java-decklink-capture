cmake_minimum_required(VERSION 2.6)
project(JavaDecklinkAPI)

include_directories(${JNI_INCLUDE_DIRS} "${SDK_PATH}/include")

#Capture.cpp Config.cpp $(SDK_PATH)/DeckLinkAPIDispatch.cpp $(CFLAGS) $(LDFLAGS)

add_library(JavaDecklink SHARED Capture.cpp Config.cpp)

target_link_libraries(JavaDecklink m dl pthread)



INSTALL(TARGETS JavaDecklink RUNTIME DESTINATION lib LIBRARY DESTINATION lib)
