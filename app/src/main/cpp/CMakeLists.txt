cmake_minimum_required(VERSION 3.22.1)

project("aegis_forensics")

add_library(
    aegis_forensics
    SHARED
    aegis_engine.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    aegis_forensics
    ${log-lib}
)
