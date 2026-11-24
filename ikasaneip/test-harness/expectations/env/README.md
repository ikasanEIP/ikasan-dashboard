# Per-environment expectation overrides

Files in `expectations/env/<HARNESS_ENV>/` are layered over `expectations/`, matching on file path and scenario name.
Only the fields you specify are overridden. For example, `expectations/env/external/search/wiretap.yaml`:

```yaml
scenarios:
  - name: all wiretaps in the last day
    expect: { total: null, minTotal: 1 }
```
