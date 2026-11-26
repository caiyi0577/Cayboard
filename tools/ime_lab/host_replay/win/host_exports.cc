// Windows host replay only: a Windows DLL exports nothing by default, so lexicon_regression.py (ctypes) reaches the
// The input engine API and app candidate policy through these two C entry points. The JNI entry points are exported
// by JNIEXPORT already.
#include <rime_api.h>
#include <string>

#include "candidate_policy.h"

extern "C" __declspec(dllexport) RimeApi* cayboard_host_rime_get_api() { return rime_get_api(); }

extern "C" __declspec(dllexport) void cayboard_host_initialize_candidate_policy(const char* user_dir) {
  cayboard::InitializeCandidatePolicy(user_dir ? std::string(user_dir) : std::string());
}
