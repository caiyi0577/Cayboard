"""Fetch the pinned STQ kernel and preserve the enum IDs used by the official Hy-MT2 release."""
from pathlib import Path
import argparse,urllib.request,tarfile,re,json
COMMIT='1e411d8f5a1e23525fa3265dfb4bd76265465397'
parser=argparse.ArgumentParser();parser.add_argument('directory');args=parser.parse_args();root=Path(args.directory).resolve();root.mkdir(parents=True,exist_ok=True)
source=root/'llama.cpp';archive=root/'llama-source.tar.gz';url=f'https://codeload.github.com/sjl623/llama.cpp/tar.gz/{COMMIT}'
if not (source/'include/llama.h').exists():
    if not archive.exists():
        with urllib.request.urlopen(url,timeout=90) as data,archive.open('wb') as output:
            while block:=data.read(1024*1024):output.write(block)
    with tarfile.open(archive) as bundle:
        for member in bundle.getmembers():
            target=source/Path(*Path(member.name).parts[1:]);assert target.resolve().is_relative_to(source.resolve())
            if member.isfile():target.parent.mkdir(exist_ok=True,parents=True);target.write_bytes(bundle.extractfile(member).read())
for filename,pairs in [('ggml/include/ggml.h',[('GGML_TYPE_Q2_0',42,43),('GGML_TYPE_STQ1_0',43,42)]),('include/llama.h',[('LLAMA_FTYPE_MOSTLY_Q2_0',41,42),('LLAMA_FTYPE_MOSTLY_STQ1_0',42,41)])]:
    path=source/filename;text=path.read_text(encoding='utf-8')
    for name,before,after in pairs:
        if re.search(r'\b'+name+r'\s*=\s*'+str(after)+r'\b',text):continue
        text,n=re.subn(r'(\b'+name+r'\s*=\s*)'+str(before)+r'\b',lambda m:m[1]+str(after),text);assert n==1,(filename,name)
    path.write_text(text,encoding='utf-8')
(root/'source-provenance.json').write_text(json.dumps({'url':url,'commit':COMMIT,'compatibility':'Official pinned Hy-MT2 GGUF uses STQ tensor type42/filetype41; remap the newly conflicting Q2_0 IDs to43/42. Weights and STQ kernels unchanged.'},indent=2),encoding='utf-8')
print(source)
