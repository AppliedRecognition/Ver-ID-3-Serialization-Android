cmake_minimum_required(VERSION 3.22.1)
project(ImageUtil)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(ImageUtil SHARED ImageUtil.cpp)

find_library(jnigraphics-lib jnigraphics)
find_library(log-lib log)

target_link_libraries(ImageUtil ${jnigraphics-lib} ${log-lib})