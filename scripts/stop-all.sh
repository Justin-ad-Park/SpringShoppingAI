#!/usr/bin/env bash
# Resolve this checkout so services from a different clone are never selected.
set -euo pipefail
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd -P)"
exec python3 - "$PROJECT_ROOT" "$@" <<'PY'
import argparse
import os
from pathlib import Path
import shlex
import shutil
import signal
import subprocess
import sys
import time

root = Path(sys.argv.pop(1)).resolve()
parser = argparse.ArgumentParser(description='이 프로젝트의 쇼핑 서비스만 찾아 종료합니다. 포트 번호에 의존하지 않습니다.')
parser.add_argument('--dry-run', action='store_true', help='종료 대상을 확인하고 실제 종료하지 않음')
parser.add_argument('--force', action='store_true', help='SIGTERM 후 15초 내 종료하지 않은 대상에 SIGKILL 사용')
options = parser.parse_args()
for command in ('ps', 'lsof'):
    if shutil.which(command) is None:
        sys.exit(f'{command} 명령이 필요합니다.')
modules = {
    'chatbot-client': 'com.example.shopping.client.ClientApplication',
    'shopping-agent': 'com.example.shopping.agent.AgentApplication',
    'shopping-mcp': 'com.example.shopping.mcp.McpApplication',
}

def output(*args):
    return subprocess.run(args, capture_output=True, text=True, check=True).stdout


def identify(pid):
    """Verify Java + exact module path/main class, without displaying process arguments."""
    try:
        executable = output('ps', '-p', str(pid), '-o', 'comm=').strip()
        if Path(executable).name != 'java':
            return None
        args = shlex.split(output('ps', '-p', str(pid), '-o', 'args='))
        cwd_lines = output('lsof', '-a', '-p', str(pid), '-d', 'cwd', '-Fn').splitlines()
        cwd = next(Path(line[1:]).resolve() for line in cwd_lines if line.startswith('n'))
        if '-jar' in args:
            jar = Path(args[args.index('-jar') + 1])
            jar = (cwd / jar).resolve() if not jar.is_absolute() else jar.resolve()
            for module in modules:
                if jar.parent == root / module / 'build/libs' and jar.name.startswith(module + '-') and jar.suffix == '.jar':
                    return module
        # bootRun / IDE: both the main class and a classpath in this checkout must match.
        for module, main in modules.items():
            if main not in args:
                continue
            for flag in ('-cp', '-classpath', '--class-path'):
                if flag not in args:
                    continue
                for entry in args[args.index(flag) + 1].split(os.pathsep):
                    path = (cwd / entry).resolve()
                    if path.is_relative_to(root / module / 'build/classes'):
                        return module
    except (subprocess.CalledProcessError, ValueError, IndexError, StopIteration, OSError):
        # A process can exit between discovery and inspection; uncertain targets are skipped.
        return None
    return None

try:
    rows = output('ps', '-ax', '-o', 'pid=,comm=').splitlines()
except (OSError, subprocess.CalledProcessError):
    sys.exit('프로세스 목록을 읽지 못했습니다. ps 실행 권한을 확인하세요.')

targets = {}
try:
    for row in rows:
        fields = row.strip().split(None, 1)
        if len(fields) == 2 and Path(fields[1]).name == 'java':
            pid = int(fields[0])
            module = identify(pid)
            if module:
                targets[pid] = module
except FileNotFoundError:
    sys.exit('python3, ps, lsof가 필요합니다.')

if not targets:
    print('이 프로젝트에서 실행 중인 쇼핑 서비스를 찾지 못했습니다.')
    sys.exit(0)
for pid, module in targets.items():
    print(f'{"[확인]" if options.dry_run else "[종료 요청]"} {module} (PID {pid})', flush=True)
if options.dry_run:
    sys.exit(0)

for pid, module in targets.items():
    # Recheck identity immediately before signalling; never trust a stale PID alone.
    if identify(pid) == module:
        try:
            os.kill(pid, signal.SIGTERM)
        except ProcessLookupError:
            pass
        except PermissionError:
            sys.exit(f'PID {pid} 종료 권한이 없습니다.')

deadline = time.monotonic() + 15
remaining = dict(targets)
while remaining and time.monotonic() < deadline:
    remaining = {pid: module for pid, module in remaining.items() if identify(pid) == module}
    if remaining:
        time.sleep(0.3)
if remaining and options.force:
    for pid, module in remaining.items():
        if identify(pid) == module:
            try:
                os.kill(pid, signal.SIGKILL)
                print(f'[강제 종료] {module} (PID {pid})')
            except ProcessLookupError:
                pass
            except PermissionError:
                sys.exit(f'PID {pid} 강제 종료 권한이 없습니다.')
    time.sleep(0.3)
    remaining = {pid: module for pid, module in remaining.items() if identify(pid) == module}
if remaining:
    print('아직 실행 중: ' + ', '.join(f'{name} (PID {pid})' for pid, name in remaining.items()))
    sys.exit('로그를 확인하거나 필요하면 --force 옵션으로 다시 실행하세요.')
print('쇼핑 서비스가 모두 종료되었습니다.')
PY
