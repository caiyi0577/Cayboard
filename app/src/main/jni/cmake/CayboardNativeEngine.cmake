# SPDX-FileCopyrightText: 2015 - 2024 Rime community
#
# SPDX-License-Identifier: GPL-3.0-or-later

# Lua's Android fseeko guard comes from patches/cayboard-lua-android.patch (Gradle applyCayboardSourcePatches).
include("${CMAKE_CURRENT_LIST_DIR}/CayboardPatches.cmake")
# Nine-key whole-word grammar scoring and host replay counters; the plugin copy below picks it up.
cayboard_apply_patch("${CMAKE_SOURCE_DIR}/librime-octagram" "${CMAKE_CURRENT_LIST_DIR}/OctagramT9.patch")

set(CAYBOARD_ENGINE_PLUGINS cayboard_azookey librime-octagram librime-predict librime-t9)

foreach(plugin ${CAYBOARD_ENGINE_PLUGINS})
  file(COPY "${CMAKE_SOURCE_DIR}/${plugin}/"
       DESTINATION "${CMAKE_SOURCE_DIR}/librime/plugins/${plugin}")
endforeach()

file(COPY "${CMAKE_SOURCE_DIR}/librime-lua/"
     DESTINATION "${CMAKE_SOURCE_DIR}/librime/plugins/lua")

if(NOT EXISTS "${CMAKE_SOURCE_DIR}/librime/plugins/lua/thirdparty")
  file(COPY "${CMAKE_SOURCE_DIR}/librime-lua-deps/"
       DESTINATION "${CMAKE_SOURCE_DIR}/librime/plugins/lua/thirdparty")
endif()

option(BUILD_TEST "" OFF)
option(BUILD_STATIC "" ON)
option(CAYBOARD_ENGINE_VERBOSE_TRACE "Trace every native dictionary lookup (slow)" OFF)
if(CAYBOARD_ENGINE_VERBOSE_TRACE)
  add_compile_definitions(CAYBOARD_ENGINE_VERBOSE_TRACE=1)
endif()
include("${CMAKE_CURRENT_LIST_DIR}/T9WorkCounters.cmake")
# Every Cayboard decoder integration change; the same file as patches/cayboard-librime.patch, which Gradle applies first.
cayboard_apply_patch("${CMAKE_SOURCE_DIR}/librime" "${CMAKE_CURRENT_LIST_DIR}/CayboardEngineIntegration.patch")
# Use librime's explicit module list so stale copies in its ignored plugins/ directory cannot join the build.
set(cayboard_saved_plugins "$ENV{RIME_PLUGINS}")
string(JOIN " " cayboard_plugin_list ${CAYBOARD_ENGINE_PLUGINS} lua)
set(ENV{RIME_PLUGINS} "${cayboard_plugin_list}")
add_subdirectory(librime)
set(ENV{RIME_PLUGINS} "${cayboard_saved_plugins}")
include("${CMAKE_CURRENT_LIST_DIR}/CayboardDecoderSources.cmake")
target_compile_options(
  rime-static PRIVATE "-ffile-prefix-map=${CMAKE_SOURCE_DIR}=." "-Wno-error=deprecated-declarations")

target_compile_options(
  rime-lua-objs PRIVATE "-ffile-prefix-map=${CMAKE_SOURCE_DIR}=.")
