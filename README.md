# Log Analytics Engine

A log analytics engine in Scala: parse log lines into structured events,
aggregate them over time windows, and query the results — built for
operational log analysis without a heavyweight stack.

## Roadmap

- [x] Project scaffold (sbt, ScalaTest, CI, README)
- [ ] Log parsing (common formats, structured events)
- [ ] Aggregation + time-window queries
- [ ] Self-review checklist, then public

## Quick start

```sh
sbt test
sbt "runMain loganalytics.Main"
```

## License

MIT — see [LICENSE](LICENSE).
