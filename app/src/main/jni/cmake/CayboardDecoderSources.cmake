# Cayboard code compiled into the native decoder target: the decoder sources in cayboard_decoder/ and the CyLM decoder.
# Explicit integration hooks in CayboardEngineIntegration.patch and patches/cayboard-librime.patch call these sources.
file(READ "${CMAKE_SOURCE_DIR}/librime/src/rime/gear/script_translator.cc" CAYBOARD_SCRIPT_TRANSLATOR)
string(FIND "${CAYBOARD_SCRIPT_TRANSLATOR}" "T9DecodeJoint(" CAYBOARD_DECODER_HOOK)
if(CAYBOARD_DECODER_HOOK EQUAL -1)
  message(FATAL_ERROR "Cayboard decoder hooks missing from librime; the librime patch was not applied")
endif()
target_include_directories(rime-static PRIVATE "${CMAKE_SOURCE_DIR}/librime-t9/src")
target_include_directories(rime-static PRIVATE "${CMAKE_SOURCE_DIR}/librime-octagram/src")
# OctagramT9.patch includes t9_lookup_trace.h (work counters, empty outside the host replay).
target_include_directories(rime-octagram-objs PRIVATE "${CMAKE_SOURCE_DIR}/librime-t9/src")
# CyLM decoder (cylm/cylm.cpp, also built alone as libcylm.so): used only when a schema names its files.
target_include_directories(rime-static PRIVATE "${CMAKE_SOURCE_DIR}/cylm")
target_sources(rime-static PRIVATE "${CMAKE_SOURCE_DIR}/cylm/cylm.cpp")
target_include_directories(rime-static PRIVATE "${CMAKE_SOURCE_DIR}/cayboard_decoder")
target_sources(rime-static PRIVATE "${CMAKE_SOURCE_DIR}/cayboard_decoder/t9_script_options.cc"
               "${CMAKE_SOURCE_DIR}/cayboard_decoder/t9_cylm_sentence.cc"
               "${CMAKE_SOURCE_DIR}/cayboard_decoder/t9_sentence_rescore.cc"
               "${CMAKE_SOURCE_DIR}/cayboard_decoder/t9_joint_decoder.cc")
# Source properties are per directory: set it where rime-static is defined, or main() gets linked in.
set_source_files_properties("${CMAKE_SOURCE_DIR}/cylm/cylm.cpp" TARGET_DIRECTORY rime-static PROPERTIES COMPILE_DEFINITIONS "CYLM_LIBRARY;CYLM_ORT"
  # OrtStatus results of option setters are ignored on purpose in cylm.cpp; the NDK build treats that as an error.
  COMPILE_OPTIONS "-Wno-unused-result")
# The reranker uses the onnxruntime C API; rime-static links onnxruntime already (jni CMakeLists).
target_include_directories(rime-static PRIVATE "${CMAKE_SOURCE_DIR}/onnxruntime/include")
