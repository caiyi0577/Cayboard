#!/bin/bash
# Same engine and test sets: stock sentence decoding (joint_decoder off, grammar on)
# versus the n-gram joint decoder. Restores the schema afterwards. Run inside WSL.
set -u
REPO=/mnt/d/GitHub/Cayboard
SCHEMA=$REPO/app/src/main/assets/cayboard_chinese/t9_pinyin.schema.yaml
OUT=$REPO/.codex-artifacts/t9-ngram-20261010/stock
M=${M:-$HOME/cayboard-work/ngram}  # build workspace; inputs archived in D:/Cayboard-Data/corpus, models in D:/Cayboard-Data/models
SETS="reported=tools/ime_lab/fixtures/user_reported_20261009.json anota=.codex-artifacts/t9-ngram-20261010/anota/fixture.json everyday120=tools/ime_lab/fixtures/everyday_clauses_20261009.json gen-everyday=.codex-artifacts/t9-ngram-20261010/gen/everyday/fixture.json constructions=.codex-artifacts/t9-ngram-20261010/gen/constructions/fixture.json chat=tools/ime_lab/fixtures/daily_chat_20261003.json"
cd "$REPO"
sed -i "s/^  joint_decoder: true/  joint_decoder: false/" "$SCHEMA"
bash tools/ime_lab/ngram/eval_runs.sh "$OUT" base none $SETS
sed -i "s/^  joint_decoder: false/  joint_decoder: true/" "$SCHEMA"
bash tools/ime_lab/ngram/eval_runs.sh "$OUT" lm3 "$M/model-v3-ls-tri.lm" $SETS
echo COMPARE-DONE | tee -a "$OUT/runs.log"
