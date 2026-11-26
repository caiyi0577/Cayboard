"""Rebuild the pinned emoji catalog/fonts/OpenCC additions from a downloaded source directory.

Usage: python tools/emoji/build_emoji_bundle.py <source-directory>
Source URLs and checksums are retained in assets/emoji/sources.json.
Apple artwork is proprietary; this local test bundle is not a grant to redistribute it.
"""
from pathlib import Path
from collections import defaultdict
import sys, re, shutil, json, hashlib
import xml.etree.ElementTree as ET
from fontTools.ttLib import TTFont

root = Path(__file__).resolve().parents[2]
src = Path(sys.argv[1]).resolve()
for name, info in json.loads((src/'sources.json').read_text(encoding='utf-8')).items():
    assert hashlib.sha256((src/name).read_bytes()).hexdigest() == info['sha256'], name
assets = root / 'app/src/main/assets'
out = assets / 'emoji'
out.mkdir(exist_ok=True)
groups = defaultdict(list)
group = ''
for line in (src / 'emoji-test.txt').read_text(encoding='utf-8').splitlines():
    if line.startswith('# group: '):
        group = line[9:]
    elif re.match(r'^[0-9A-F]', line):
        cps, rest = line.split(';', 1)
        status = rest.split('#')[0].strip()
        if status in ('fully-qualified', 'component'):
            value = ''.join(chr(int(cp, 16)) for cp in cps.split())
            groups[group].append(value)
names = ['笑脸与情感','人物与手势','肤色与发型','动物与自然','食物与饮料','旅行与地点','活动','物品','符号','旗帜']
assert len(groups) == len(names), list(groups)
kt = root / 'app/src/main/java/com/cayboard/data/EmojiData.kt'
original = kt.read_text(encoding='utf-8').split('object EmojiData {')[0]
lines = [original, '// Generated from Unicode Emoji 18.0; see assets/emoji/sources.json.', 'object EmojiData {',
         '    val categories: List<EmojiCategory> = listOf(']
for i, ((group, values), name) in enumerate(zip(groups.items(), names)):
    lines += [f'        EmojiCategory(name = "{name}", icon = "{values[0]}", emojis = group{i}()),']
lines += ['    )', '    val all: Set<String> by lazy { categories.flatMap { it.emojis }.toSet() }']
for i, values in enumerate(groups.values()):
    # Split large groups to keep JVM methods below 64 KiB.
    chunks = [values[n:n+200] for n in range(0,len(values),200)]
    lines += [f'    private fun group{i}(): List<String> = ' + ' + '.join(f'group{i}_{n}()' for n in range(len(chunks)))]
    for n, chunk in enumerate(chunks):
        lines += [f'    private fun group{i}_{n}() = listOf(']
        for start in range(0,len(chunk),8):
            lines += ['        ' + ', '.join(json.dumps(v,ensure_ascii=False) for v in chunk[start:start+8]) + ',']
        lines += ['    )']
lines += ['}', '']
kt.write_text('\n'.join(lines), encoding='utf-8')
(out/'catalog.tsv').write_text('\n'.join(f'{group}\t{emoji}' for group, values in groups.items() for emoji in values)+'\n', encoding='utf-8')
shutil.copyfile(src/'Android.ttf',out/'Android.ttf')
font = TTFont(src/'iOS.ttf')
index = next(i for i,s in enumerate(font['CBLC'].strikes) if s.bitmapSizeTable.ppemX == 96)
font['CBDT'].strikeData = [font['CBDT'].strikeData[index]]
font['CBLC'].strikes = [font['CBLC'].strikes[index]]
font.save(out/'iOS.ttf')
for name in ['Noto-OFL.txt','Unicode-LICENSE.txt','Apple-README.md','sources.json']:
    shutil.copyfile(src/name,out/name)
(out/'NOTICE.txt').write_text('Android: Google Noto Color Emoji, SIL OFL 1.1.\n'
    'Catalog and Chinese annotations: Unicode Emoji 18.0 / CLDR, Unicode License.\n'
    'iOS: Apple Color Emoji artwork, Apple Inc. Proprietary assets; NOT covered by converter MIT license.\n'
    'Converted by samuelngs/apple-emoji-ttf; this local build retains the 96 ppem strike.\n'
    'Do not publish the Apple font as open-source or redistribute without appropriate rights.\n',encoding='utf-8')

# Preserve curated upstream translations; supplement with CLDR short names and keywords.
upstream = root/'app/build/generated/chinese-assets/cayboard_pinyin/opencc/emoji.txt'
mapping = {}
for line in upstream.read_text(encoding='utf-8').splitlines():
    if '\t' in line and not line.startswith('#'):
        k, v = line.split('\t',1)
        mapping[k] = v.split()
canonical = {v.replace('\ufe0f',''):v for values in groups.values() for v in values}
for name in ['zh.xml','zh-derived.xml']:
    for ann in ET.parse(src/name).iter('annotation'):
        emoji = canonical.get(ann.attrib.get('cp','').replace('\ufe0f',''))
        if not emoji or not ann.text: continue
        for word in ann.text.split('|'):
            word = word.strip()
            if not re.fullmatch(r'[\u3400-\u9fff]{2,8}',word): continue
            values = mapping.setdefault(word,[word])
            if emoji not in values and len(values) < 4: values.append(emoji)
mapping['喷泉'] = ['喷泉','⛲']
opencc = assets/'cayboard_chinese/opencc'
(opencc/'emoji.txt').write_text('\n'.join(k+'\t'+' '.join(v) for k,v in sorted(mapping.items()))+'\n',encoding='utf-8')
shutil.copyfile(root/'app/build/generated/chinese-assets/cayboard_pinyin/opencc/emoji.json',opencc/'emoji.json')
report = {'unicode':'18.0','emoji_count':sum(map(len,groups.values())),
          'groups':{k:len(v) for k,v in groups.items()},'keyword_count':len(mapping),
          'font_outputs':{name:{'bytes':(out/name).stat().st_size,'sha256':hashlib.sha256((out/name).read_bytes()).hexdigest()} for name in ['Android.ttf','iOS.ttf']}}
(out/'bundle.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(report,ensure_ascii=False,indent=2))
