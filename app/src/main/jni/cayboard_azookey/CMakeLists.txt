# Rime plugin: Japanese conversion through libCyAzooKey.so (prebuilt from tools/azookey, loaded at runtime).
project(rime-cayboard-azookey)
cmake_minimum_required(VERSION 3.10)

aux_source_directory(. azookey_src)

add_library(rime-cayboard_azookey-objs OBJECT ${azookey_src})
if(BUILD_SHARED_LIBS)
  set_target_properties(rime-cayboard_azookey-objs
    PROPERTIES
    POSITION_INDEPENDENT_CODE ON)
endif()

set(plugin_name rime-cayboard_azookey PARENT_SCOPE)
set(plugin_objs $<TARGET_OBJECTS:rime-cayboard_azookey-objs> PARENT_SCOPE)
set(azookey_deps ${rime_library} ${CMAKE_DL_LIBS})
if(ANDROID)
  list(APPEND azookey_deps log)
endif()
set(plugin_deps ${azookey_deps} PARENT_SCOPE)
set(plugin_modules "cayboard_azookey" PARENT_SCOPE)
