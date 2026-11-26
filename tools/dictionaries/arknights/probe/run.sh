#!/bin/bash
# Isolated probe: copies the T9 thread's latest replay resources (read-only template),
# runs once without and once with the Arknights pack. Never touches the template.
set -euo pipefail
P="$1"            # output dir holding cases.tsv (results written here)
REPO=/mnt/d/GitHub/Cayboard
TEMPLATE=${TEMPLATE:-$HOME/.cache/cayboard-t9-host/replay-roots/eng2-gen-everyday-47e5bb80/user}
LIB=${LIB:-$HOME/.cache/cayboard-t9-host/builds/9636457bb8860baac1b8/lib}
WORK=~/.cache/cayboard-ark-probe
rm -rf "$WORK"; mkdir -p "$WORK/classes"
~/.cache/cayboard-t9-host/dependencies/javac -encoding UTF-8 --release 8 -d "$WORK/classes" \
  "$REPO/tools/ime_lab/android_replay/InputEngine.java" "$REPO/tools/dictionaries/arknights/probe/ArkProbe.java"
PACK=user_cayboard_cells_$(printf cayboard_pinyin | sha256sum | cut -c1-16)
for variant in base ark; do
  U="$WORK/$variant"; mkdir -p "$U"
  for f in "$TEMPLATE"/*; do
    name=$(basename "$f")
    case "$name" in
      *.userdb|logs|build|installation.yaml|user.yaml) ;;
      cn_dicts|en_dicts|opencc|lua|*.gram|*.lm|*.onnx|*.vocab) ln -s "$f" "$U/$name" ;;
      *) cp -r "$f" "$U/$name" ;;
    esac
  done
  sed -i 's/^    - schema: t9_pinyin$/    - schema: t9_pinyin\n    - schema: cayboard_pinyin\n    - schema: double_pinyin_flypy/' "$U/default.custom.yaml"
  if [ "$variant" = ark ]; then
    mkdir -p "$U/cayboard_cells"
    cp "$REPO/app/src/main/assets/cell_dictionaries/cell_builtin_arknights.dict.yaml" "$U/cayboard_cells/"
    printf -- '---\nname: %s\nversion: "1"\nsort: by_weight\nuse_preset_vocabulary: false\nimport_tables:\n  - cayboard_cells/cell_builtin_arknights\n...\n' "$PACK" > "$U/$PACK.dict.yaml"
    for s in cayboard_pinyin t9_pinyin double_pinyin_flypy; do
      f="$U/$s.custom.yaml"
      if [ ! -s "$f" ] || ! grep -q '^patch:' "$f"; then printf 'patch:\n' >> "$f"; fi
      # Same managed block CellDictionaryEngine.patch inserts right after "patch:".
      sed -i "0,/^patch:.*/s//patch:\n  # BEGIN CAYBOARD CELL DICTIONARIES\n  \"translator\/packs\/+\": [\"$PACK\"]\n  # END CAYBOARD CELL DICTIONARIES/" "$f"
    done
  fi
  echo "== $variant"
  java -cp "$WORK/classes" ArkProbe "$LIB" "$U" "$P/cases.tsv" 2>"$WORK/$variant.err" | grep '^CASE' > "$P/result-$variant.tsv" || { tail -20 "$WORK/$variant.err"; exit 1; }
  wc -l < "$P/result-$variant.tsv"
done
du -sh "$WORK"
