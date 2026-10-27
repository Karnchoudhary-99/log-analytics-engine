# Log Analytics Engine

A log analytics engine in Scala: parse log lines into structured events,
aggregate them over time windows, and query the results — built for
operational log analysis without a heavyweight stack. Stdlib only for parsing
(no JSON dependency); ScalaTest for tests.

## Roadmap

- [x] Project scaffold (sbt, ScalaTest, CI, README)
- [x] Log parsing (common formats, structured events)
- [x] Aggregation + time-window queries
- [x] Alerting (threshold rules over windows) + JSON/CSV export
- [x] Error-rate trends, burst detection, multi-file input
- [ ] Self-review checklist, then public

## Formats

One file can mix all three — `MultiFormatParser` dispatches per line:

- **Plain:** `2026-10-27T18:00:00Z INFO [web] User login succeeded user=alice`
  (ISO, `yyyy-MM-dd HH:mm:ss`, or epoch timestamps; `key=value` pairs become fields)
- **Apache combined:** `127.0.0.1 - alice [27/Oct/2026:18:00:00 +0000] "GET / HTTP/1.1" 200 1234 "-" "curl/8.0"`
  (status-derived severity: 5xx ERROR, 4xx WARN, else INFO)
- **JSON:** `{"timestamp":"...","level":"ERROR","service":"db","message":"...","pool":"main"}`
  (known keys mapped, everything else kept as fields)

## Quick start

```sh
sbt test
sbt "runMain loganalytics.Demo"
sbt "runMain loganalytics.Main parse examples/sample.log"
sbt "runMain loganalytics.Main aggregate examples/sample.log --window 1h"
sbt "runMain loganalytics.Main query examples/sample.log --level ERROR"
sbt "runMain loganalytics.Main query examples/sample.log --source web --contains login"
```

Query filters compose (AND): `--level`, `--source`, `--from`, `--to`,
`--contains`, `--field key=value`. Aggregation buckets events into tumbling
windows (`--window 15m|1h|1d`), reporting per-window totals, per-level and
per-source counts, and error rates.

## Sample output

```
Parsed 7 events, 0 failures (success rate 100.0%)
Error rate: 28.6%
By level:
  INFO: 3
  WARN: 2
  ERROR: 2
Windows (5 min):
  2026-10-27T18:00:00Z total=6 errors=1
  2026-10-27T18:05:00Z total=1 errors=1
```

## Alerts and export

```sh
sbt "runMain loganalytics.Main alerts examples/alerts.log"
# 1 alert(s): the 18:05 window is 3/4 errors (75%), tripping the default rule
sbt "runMain loganalytics.Main alerts examples/alerts.log --alert sev:rate=0.5,count=2"
sbt "runMain loganalytics.Main parse examples/sample.log --format json"
sbt "runMain loganalytics.Main aggregate examples/sample.log --window 1h --format csv"
sbt "runMain loganalytics.Main query examples/sample.log --level ERROR --format json"
```

Alert specs are `name:rate=0.5,count=3,window=5m` (rate and count are
optional, at least one required; a window fires when either threshold is
met). Severity bands: `critical` at >= 50% error rate or >= 20 errors,
`warning` at >= 25% or >= 5 errors, otherwise `info`.

## Trends, bursts, and multi-file input

```sh
sbt "runMain loganalytics.Main trend examples/trend.log"
# Error-rate trend: rising (slope 0.250 per window); no bursts
sbt "runMain loganalytics.Main trend examples/burst.log"
# ... plus: 1 burst(s): 18:10 total=12 baseline=2.0 ratio=6.0x
sbt "runMain loganalytics.Main trend examples/burst.log --burst-ratio 2.0"
sbt "runMain loganalytics.Main parse examples/sample.log examples/alerts.log"
```

Every command accepts several files, read as one stream. Trend slope is
the least-squares slope of per-window error rates; a window is a burst
when its count is at least `--burst-ratio` (default 3.0) times the mean of
the other windows and at least 5 events.

## License

MIT — see [LICENSE](LICENSE).
