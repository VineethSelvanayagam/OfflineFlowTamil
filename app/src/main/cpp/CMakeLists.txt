cmake_minimum_required(VERSION 3.22.1)
project(offlinewhisper LANGUAGES C CXX)
set(CMAKE_CXX_STANDARD 17)
# Keep CPU inference optimized even in the Android debug APK.
set(CMAKE_C_FLAGS_DEBUG "${CMAKE_C_FLAGS_DEBUG} -O2")
set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -O2")

# Location: app/third_party/whisper.cpp (not bundled in starter archive).
set(WHISPER_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/../../../third_party/whisper.cpp")
if(NOT EXISTS "${WHISPER_ROOT}/CMakeLists.txt")
    message(FATAL_ERROR "Missing app/third_party/whisper.cpp; see README.md")
endif()
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(GGML_VULKAN OFF CACHE BOOL "" FORCE)
set(GGML_OPENCL OFF CACHE BOOL "" FORCE)
add_subdirectory("${WHISPER_ROOT}" "${CMAKE_CURRENT_BINARY_DIR}/whisper-src" EXCLUDE_FROM_ALL)

add_library(offlinewhisper SHARED whisper_jni.cpp)
target_include_directories(offlinewhisper PRIVATE "${WHISPER_ROOT}/include")
target_link_libraries(offlinewhisper PRIVATE whisper log)
