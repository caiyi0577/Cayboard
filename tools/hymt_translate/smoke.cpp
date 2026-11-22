#include "hymt_jni.cpp"
#include <cstdio>
int main(int argc,char **argv) {
    if(argc<2) return 2;
    llama_log_set(nullptr,nullptr);
    llama_backend_init();
    Engine engine;
    auto params=llama_model_default_params();params.n_gpu_layers=0;params.load_mode=LLAMA_LOAD_MODE_MMAP;
    engine.model=llama_model_load_from_file(argv[1],params);
    if(!engine.model) {fprintf(stderr,"MODEL_LOAD_FAILED\n");return 3;}
    auto ctx=llama_context_default_params();ctx.n_ctx=4096;ctx.n_batch=256;ctx.n_ubatch=128;ctx.n_threads=4;ctx.n_threads_batch=4;
    engine.ctx=llama_init_from_model(engine.model,ctx);
    if(!engine.ctx) return 4;
    llama_set_abort_callback(engine.ctx,aborted,&engine);
    try {auto result=translate(engine,"你好，明天见。","Chinese","Japanese");printf("TRANSLATION=%s\n",result.c_str());}
    catch(const std::exception &e) {fprintf(stderr,"TRANSLATION_ERROR=%s\n",e.what());return 5;}
    return 0;
}
