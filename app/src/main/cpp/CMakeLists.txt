cmake_minimum_required(VERSION 3.4.1)

set(ocvlibs "C:/Users/19138/Documents/BiZhang4/OpenCV-android-sdk/sdk/native/libs")
include_directories("C:/Users/19138/Documents/BiZhang4/OpenCV-android-sdk/sdk/native/jni/include")

add_library(libopencv_java4 SHARED IMPORTED )
set_target_properties(libopencv_java4 PROPERTIES
        IMPORTED_LOCATION "${ocvlibs}/${ANDROID_ABI}/libopencv_java4.so")


add_library( # Sets the name of the library.
        native-lib SHARED native-lib.cpp )



find_library(log-lib log )


target_link_libraries(
        native-lib android log libopencv_java4


        ${log-lib} )