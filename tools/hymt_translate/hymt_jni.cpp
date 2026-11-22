#include <jni.h>
#include <llama.h>
#include <atomic>
#include <algorithm>
#include <memory>
#include <mutex>
#include <stdexcept>
#include <string>
#include <vector>
#include <sys/auxv.h>

static_assert(GGML_TYPE_STQ1_0 == 42, "Pinned Hy-MT2 release requires STQ1_0 type 42");
namespace {
std::atomic<uint64_t> cancellation{0};
std::once_flag initialized;
std::string utf8(JNIEnv *env, jstring s) {
    if (!s) return {};
    jclass cls=env->FindClass("java/lang/String");
    auto method=env->GetMethodID(cls,"getBytes","(Ljava/lang/String;)[B");
    auto encoding=env->NewStringUTF("UTF-8");
    auto bytes=static_cast<jbyteArray>(env->CallObjectMethod(s,method,encoding));
    env->DeleteLocalRef(encoding);env->DeleteLocalRef(cls);
    if (!bytes) throw std::runtime_error("UTF-8 conversion failed");
    jsize n=env->GetArrayLength(bytes);std::string value(n,'\0');
    env->GetByteArrayRegion(bytes,0,n,reinterpret_cast<jbyte *>(value.data()));
    env->DeleteLocalRef(bytes);return value;
}
jstring javaString(JNIEnv *env,const std::string &text) {
    auto bytes=env->NewByteArray(static_cast<jsize>(text.size()));
    env->SetByteArrayRegion(bytes,0,static_cast<jsize>(text.size()),reinterpret_cast<const jbyte *>(text.data()));
    auto cls=env->FindClass("java/lang/String");
    auto ctor=env->GetMethodID(cls,"<init>","([BLjava/lang/String;)V");
    auto encoding=env->NewStringUTF("UTF-8");
    auto s=static_cast<jstring>(env->NewObject(cls,ctor,bytes,encoding));
    env->DeleteLocalRef(bytes);env->DeleteLocalRef(encoding);env->DeleteLocalRef(cls);return s;
}
void fail(JNIEnv *env,const std::exception &e) {env->ThrowNew(env->FindClass("java/lang/IllegalStateException"),e.what());}
struct Engine {
    llama_model *model=nullptr;
    llama_context *ctx=nullptr;
    uint64_t epoch=0;
    ~Engine() {if(ctx) llama_free(ctx);if(model) llama_model_free(model);}
};
bool aborted(void *data) {return cancellation.load(std::memory_order_relaxed)!=static_cast<Engine *>(data)->epoch;}
std::string translate(Engine &e,const std::string &text,const std::string &source,const std::string &target) {
    e.epoch=cancellation.load(std::memory_order_relaxed);
    llama_memory_clear(llama_get_memory(e.ctx),true);
    std::string instruction="Translate the following text into "+target+". Only output the translation, without explanation.";
    if(source!="auto") instruction+=" The source language is "+source+".";
    const std::string prompt="<｜hy_begin▁of▁sentence｜><｜hy_User｜>"+instruction+"\n\n"+text+"<｜hy_Assistant｜>";
    const auto vocab=llama_model_get_vocab(e.model);
    int count=-llama_tokenize(vocab,prompt.data(),static_cast<int32_t>(prompt.size()),nullptr,0,false,true);
    if(count<=0 || count>2304) throw std::runtime_error("Text is too long; translate it in smaller parts");
    std::vector<llama_token> tokens(count);
    if(llama_tokenize(vocab,prompt.data(),static_cast<int32_t>(prompt.size()),tokens.data(),count,false,true)!=count) throw std::runtime_error("Tokenization failed");
    for(int start=0;start<count;start+=256) {
        if(aborted(&e)) throw std::runtime_error("cancelled");
        const int n=std::min(256,count-start);
        auto batch=llama_batch_get_one(tokens.data()+start,n);
        if(llama_decode(e.ctx,batch)!=0) throw std::runtime_error(aborted(&e)?"cancelled":"Prompt evaluation failed");
    }
    auto chain=llama_sampler_chain_init(llama_sampler_chain_default_params());
    std::unique_ptr<llama_sampler,decltype(&llama_sampler_free)> sampler(chain,llama_sampler_free);
    llama_sampler_chain_add(chain,llama_sampler_init_penalties(llama_vocab_n_tokens(vocab),64,1.05f,0.0f,0.0f));
    llama_sampler_chain_add(chain,llama_sampler_init_greedy());
    std::string result;
    for(int step=0;step<1536;++step) {
        if(aborted(&e)) throw std::runtime_error("cancelled");
        auto token=llama_sampler_sample(chain,e.ctx,-1);
        if(llama_vocab_is_eog(vocab,token)) return result;
        std::vector<char> piece(128);
        int n=llama_token_to_piece(vocab,token,piece.data(),static_cast<int32_t>(piece.size()),0,false);
        if(n<0) {piece.resize(-n);n=llama_token_to_piece(vocab,token,piece.data(),static_cast<int32_t>(piece.size()),0,false);}
        if(n<0) throw std::runtime_error("Token decoding failed");
        result.append(piece.data(),n);
        auto batch=llama_batch_get_one(&token,1);
        if(llama_decode(e.ctx,batch)!=0) throw std::runtime_error(aborted(&e)?"cancelled":"Translation evaluation failed");
    }
    throw std::runtime_error("Translation is too long; translate it in smaller parts");
}
}
extern "C" {
JNIEXPORT jlong JNICALL Java_com_cayboard_translate_OfflineTranslatorNative_nativeCreate(JNIEnv *env,jclass,jstring path,jint threads) {
    try {
        if((getauxval(AT_HWCAP)&(1UL<<20))==0) throw std::runtime_error("This device does not support this translation model");
        std::call_once(initialized,[]{llama_backend_init();llama_log_set([](ggml_log_level,const char *,void *){},nullptr);});
        auto e=std::make_unique<Engine>();auto params=llama_model_default_params();params.n_gpu_layers=0;params.load_mode=LLAMA_LOAD_MODE_MMAP;
        e->model=llama_model_load_from_file(utf8(env,path).c_str(),params);
        if(!e->model) throw std::runtime_error("Translation model could not be loaded");
        auto ctx=llama_context_default_params();ctx.n_ctx=4096;ctx.n_batch=256;ctx.n_ubatch=128;ctx.n_threads=threads;ctx.n_threads_batch=threads;
        e->ctx=llama_init_from_model(e->model,ctx);
        if(!e->ctx) throw std::runtime_error("Translation context could not be created");
        llama_set_abort_callback(e->ctx,aborted,e.get());return reinterpret_cast<jlong>(e.release());
    } catch(const std::exception &e) {fail(env,e);return 0;}
}
JNIEXPORT jstring JNICALL Java_com_cayboard_translate_OfflineTranslatorNative_nativeTranslate(JNIEnv *env,jclass,jlong handle,jstring text,jstring source,jstring target) {
    try {if(!handle) throw std::runtime_error("Translation engine is closed");return javaString(env,translate(*reinterpret_cast<Engine *>(handle),utf8(env,text),utf8(env,source),utf8(env,target)));}
    catch(const std::exception &e) {fail(env,e);return nullptr;}
}
JNIEXPORT void JNICALL Java_com_cayboard_translate_OfflineTranslatorNative_nativeCancel(JNIEnv *,jclass) {cancellation.fetch_add(1,std::memory_order_relaxed);}
JNIEXPORT void JNICALL Java_com_cayboard_translate_OfflineTranslatorNative_nativeDestroy(JNIEnv *,jclass,jlong handle) {delete reinterpret_cast<Engine *>(handle);}
}
