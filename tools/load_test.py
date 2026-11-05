#!/usr/bin/env python3
"""Chapter 7: 표준 라이브러리만 사용하는 Mini FEP 동시 거래 부하 검증 도구."""
import argparse
import concurrent.futures
import json
import math
import statistics
import threading
import time
import urllib.error
import urllib.request
from collections import Counter

EXPECTED = {'A': 'SUCCESS001', 'B': 'SUCCESS002'}


def run_one(number, institution, url, timeout, start_gate):
    """각 거래는 독립 HTTP 요청이며, 실패 거래를 자동 재전송하지 않는다."""
    start_gate.wait()
    payload = json.dumps({'institutionCode': institution, 'data': f'{number:010d}'},
                         separators=(',', ':')).encode('utf-8')
    req = urllib.request.Request(url, data=payload,
                                 headers={'Content-Type': 'application/json'}, method='POST')
    started = time.perf_counter()
    try:
        with urllib.request.urlopen(req, timeout=timeout) as response:
            status = response.status
            body = json.loads(response.read().decode('utf-8'))
        # HTTP 200뿐 아니라 기관별 실제 응답 전문까지 검증한다.
        valid = status == 200 and body.get('data') == EXPECTED[institution]
        reason = 'OK' if valid else f'INVALID_RESPONSE(status={status},data={body.get("data")})'
    except urllib.error.HTTPError as exc:
        try:
            body = json.loads(exc.read().decode('utf-8'))
            reason = f'HTTP_{exc.code}:{body.get("code", "UNKNOWN")}'
        except (ValueError, UnicodeError):
            reason = f'HTTP_{exc.code}:INVALID_BODY'
        valid = False
    except (urllib.error.URLError, TimeoutError, OSError, ValueError, UnicodeError) as exc:
        valid = False
        reason = type(exc).__name__
    return institution, valid, reason, (time.perf_counter() - started) * 1000


def main():
    parser = argparse.ArgumentParser(description='Mini FEP Chapter 7 동시 부하 테스트')
    parser.add_argument('--url', default='http://127.0.0.1:8083/api/business/send')
    parser.add_argument('--count', type=int, default=100, help='전체 거래 수 (기본 100)')
    parser.add_argument('--concurrency', type=int, default=5, help='동시 요청 수 (기본 5)')
    parser.add_argument('--institution', choices=['A', 'B', 'AB'], default='AB')
    parser.add_argument('--timeout', type=float, default=15, help='HTTP 요청 제한시간(초)')
    args = parser.parse_args()
    if not 1 <= args.count <= 100000 or not 1 <= args.concurrency <= 500 or args.timeout <= 0:
        parser.error('count=1~100000, concurrency=1~500, timeout>0 이어야 합니다')
    if not args.url.startswith(('http://127.0.0.1:', 'http://localhost:')):
        parser.error('안전한 로컬 PoC 검증을 위해 localhost 주소만 허용합니다')

    # Event를 사용해 작업 스레드의 시작을 최대한 맞춘다. 전체 실행시간은 대기/완료 포함.
    gate = threading.Event()
    started = time.perf_counter()
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.concurrency) as executor:
        futures = []
        for i in range(args.count):
            inst = ('A' if i % 2 == 0 else 'B') if args.institution == 'AB' else args.institution
            futures.append(executor.submit(run_one, i + 1, inst, args.url, args.timeout, gate))
        gate.set()
        results = [future.result() for future in concurrent.futures.as_completed(futures)]
    elapsed = time.perf_counter() - started
    latencies = sorted(item[3] for item in results)
    success = sum(item[1] for item in results)
    p95_index = max(0, math.ceil(len(latencies) * 0.95) - 1)
    print('\n=== Chapter 7 동시 거래 부하 테스트 결과 ===')
    print(f'대상={args.url} 기관={args.institution} 요청={args.count} 동시성={args.concurrency}')
    print(f'총 소요시간={elapsed:.3f}s  TPS={args.count / elapsed:.2f} (전체 완료 기준)')
    print(f'성공={success} 실패={len(results) - success} 성공률={success / len(results) * 100:.2f}%')
    print(f'응답시간(ms): 평균={statistics.mean(latencies):.2f} 최소={latencies[0]:.2f} '
          f'최대={latencies[-1]:.2f} P95={latencies[p95_index]:.2f}')
    for inst in ('A', 'B'):
        subset = [r for r in results if r[0] == inst]
        if subset:
            print(f'기관 {inst}: 요청={len(subset)} 성공={sum(r[1] for r in subset)} '
                  f'실패={sum(not r[1] for r in subset)}')
    failures = Counter(r[2] for r in results if not r[1])
    for reason, count in sorted(failures.items()):
        print(f'실패 유형: {reason} = {count}건')
    # CI/자동화에서 실패를 확인할 수 있도록 종료 코드를 반환한다.
    return 0 if success == len(results) else 1


if __name__ == '__main__':
    raise SystemExit(main())
