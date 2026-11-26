"""Package verified local model files; build download metadata is never shipped."""
import argparse
import hashlib
import json
import os
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

def digest(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def matches_model_file(path, info):
    return path.is_file() and path.stat().st_size == info['size'] and digest(path) == info['sha256']


def prepare(output, cache, selected=None):
    models = json.loads((ROOT / 'scripts/offline-models.json').read_text(encoding='utf-8'))
    if selected:
        models = [model for model in models if model['id'] in selected]
        if {m['id'] for m in models} != set(selected):
            raise ValueError('Unknown offline model selection')
    payload = output / 'bundled-models'
    payload.mkdir(parents=True, exist_ok=True)
    for stale in payload.iterdir():
        if stale.is_dir() and stale.name not in {m['id'] for m in models}:
            assert stale.resolve().parent == payload.resolve()
            shutil.rmtree(stale)
    manifest = []
    for model in models:
        files = []
        for info in model['files']:
            source = cache / model['id'] / info['name']
            legacy_name = info.get('legacyCacheName')
            if not matches_model_file(source, info):
                legacy_source = source.with_name(legacy_name) if legacy_name else None
                if legacy_source is None or not matches_model_file(legacy_source, info):
                    raise ValueError(f'Missing or invalid offline build resource: {source}')
                source = legacy_source
            target = payload / model['id'] / info['name']
            target.parent.mkdir(parents=True, exist_ok=True)
            if not target.is_file() or digest(target) != info['sha256']:
                target.unlink(missing_ok=True)
                try:
                    os.link(source, target)
                except OSError:
                    shutil.copyfile(source, target)
            if legacy_name and legacy_name != target.name:
                target.with_name(legacy_name).unlink(missing_ok=True)
            files.append({key: info[key] for key in ('name', 'size', 'sha256')})
        manifest.append(dict(id=model['id'], version=model['version'], files=files))
    (payload / 'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding='utf-8')
    print(f'Bundled {len(manifest)} verified models, {sum(f["size"] for m in manifest for f in m["files"]):,} bytes')

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', type=Path, default=ROOT / 'app/build/generated/bundled-model-assets')
    parser.add_argument('--cache', type=Path, default=ROOT / '.gradle/bundled-model-cache')
    parser.add_argument('--models', default='')
    args = parser.parse_args()
    prepare(args.output, args.cache, [m for m in args.models.split(',') if m])
