"""Build a compact immutable DAG/HMM lexicon from pinned MIT Jieba data; no network at build/runtime."""
import argparse, ast, hashlib, json, math, struct, unicodedata
from pathlib import Path
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--source-dir',type=Path,required=True,help='Pinned jieba-dict.txt, jieba-LICENSE, prob_start/trans/emit.py, jieba-source.json')
args=parser.parse_args()
HERE=args.source_dir.resolve()
ROOT=Path(__file__).resolve().parents[1]
source=json.loads((HERE/'jieba-source.json').read_text())
assert source['commit']=='67fa2e36e72f69d9134b8a1037b83fbb070b9775', 'Use the pinned Jieba revision'

OUT=ROOT/'app/src/main/assets/clipboard-segmentation'
OUT.mkdir(parents=True,exist_ok=True)
def han(c): return unicodedata.name(c,'').startswith(('CJK UNIFIED IDEOGRAPH','CJK COMPATIBILITY IDEOGRAPH'))
words={};total=0
for line in (HERE/'jieba-dict.txt').read_text(encoding='utf-8').splitlines():
 word,freq,*_=line.split(); freq=int(freq);total+=freq
 if freq>0 and all(map(han,word)): words[word]=freq
nodes=[{}];freqs=[0.0];maxlen=0
for word,freq in words.items():
 node=0;maxlen=max(maxlen,len(word))
 for char in word:
  cp=ord(char)
  if cp not in nodes[node]: nodes[node][cp]=len(nodes);nodes.append({});freqs.append(0.0)
  node=nodes[node][cp]
 freqs[node]=math.log(freq/total)
def prob(name):
 tree=ast.parse((HERE/(name+'.py')).read_text(encoding='utf-8'))
 return ast.literal_eval(next(n.value for n in tree.body if isinstance(n,ast.Assign)))
start=prob('prob_start');trans=prob('prob_trans');emit=prob('prob_emit');states='BMES'
cps=sorted({ord(c) for row in emit.values() for c in row})
path=OUT/'jieba.cseg'
temporary=path.with_suffix('.tmp')
with temporary.open('wb') as f:
 f.write(struct.pack('<6Id',0x31475343,1,len(nodes),len(nodes)-1,len(cps),maxlen,total))
 first=0
 for i,node in enumerate(nodes): f.write(struct.pack('<IIf',first,len(node),freqs[i]));first+=len(node)
 for node in nodes:
  for cp,target in sorted(node.items()):f.write(struct.pack('<II',cp,target))
 for cp in cps:f.write(struct.pack('<I4f',cp,*(emit[s].get(chr(cp),-1e30) for s in states)))
 f.write(struct.pack('<4f',*(max(start.get(s,-1e30),-1e30) for s in states)))
 f.write(struct.pack('<16f',*(max(trans.get(a,{}).get(b,-1e30),-1e30) for a in states for b in states)))
temporary.replace(path)
meta=json.loads((HERE/'jieba-source.json').read_text())
meta.update(format='CSG1 / little endian CSR trie + HMM',words=len(words),nodes=len(nodes),edges=len(nodes)-1,hmmCharacters=len(cps),maxWordLength=maxlen,bytes=path.stat().st_size,sha256=hashlib.sha256(path.read_bytes()).hexdigest())
(OUT/'source.json').write_text(json.dumps(meta,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
licenses=ROOT/'app/src/main/assets/licenses/clipboard-segmentation';licenses.mkdir(parents=True,exist_ok=True)
(licenses/'Jieba-MIT.txt').write_bytes((HERE/'jieba-LICENSE').read_bytes())
print(json.dumps(meta,ensure_ascii=False))
