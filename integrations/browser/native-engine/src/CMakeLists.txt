cmake_minimum_required(VERSION 3.16)
project(JarvisShieldNative)
set(CMAKE_CXX_STANDARD 20)
add_library(jarvis_shield SHARED src/adblock_engine.cpp)
