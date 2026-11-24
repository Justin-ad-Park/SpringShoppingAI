#!/usr/bin/env bash
# 전용 Python 환경에서 모델을 다운로드·변환한다. Java 앱 실행 시에는 Python이 필요 없다.
set -euo pipefail
MODULE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PYTHON_BIN="${PYTHON_BIN:-python3.11}"
VENV_DIR="$MODULE_DIR/.venv"
BUILD_DIR="$MODULE_DIR/.model-build"
MODEL_DIR="$MODULE_DIR/models/multilingual-e5-small-ko-v2"

if [[ -f "$MODEL_DIR/multilingual-e5-small-ko-v2.pt" && -f "$MODEL_DIR/tokenizer.json" ]]; then
    echo "모델이 이미 준비되어 있습니다: $MODEL_DIR"
    exit 0
fi

"$PYTHON_BIN" -m venv "$VENV_DIR"
"$VENV_DIR/bin/python" -m pip install -r "$MODULE_DIR/modelShell/requirements.txt"
mkdir -p "$BUILD_DIR"
EXPORT_DIR="$(mktemp -d "$BUILD_DIR/export.XXXXXX")"
# 0.30.0 변환기는 -o로 지정한 빈 폴더에 .pt와 tokenizer.json을 직접 생성한다.
"$VENV_DIR/bin/djl-convert" -m dragonkue/multilingual-e5-small-ko-v2 -o "$EXPORT_DIR" --cpu-only
for file_name in multilingual-e5-small-ko-v2.pt tokenizer.json serving.properties; do
    test -s "$EXPORT_DIR/$file_name" || { echo "필수 파일 없음: $file_name" >&2; exit 1; }
done
mkdir -p "$MODEL_DIR"
cp "$EXPORT_DIR/"* "$MODEL_DIR/"
# mktemp로 만든 이번 변환의 임시 폴더만 정리한다.
rm -rf -- "$EXPORT_DIR"
echo "모델 준비 완료: $MODEL_DIR"
