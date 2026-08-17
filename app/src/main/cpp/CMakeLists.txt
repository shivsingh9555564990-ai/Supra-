cmake_minimum_required(VERSION 3.22.1)
project("ultronai")

add_library(ultronai-lib SHARED native-lib.cpp)

find_library(log-lib log)

target_link_libraries(ultronai-lib ${log-lib})
