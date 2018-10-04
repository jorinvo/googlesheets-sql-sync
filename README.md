# googlesheets-sql-sync

Keep your SQL database in sync with Google Sheets.

Use this to let users manually insert data using Google Sheets
while using all available SQL tooling for further processing.

SQL uses JDBC and bundles the PostgreSQL driver.
Additional drivers can be added any time.
If you sqlite or mysql support open an issue and support can be added in no time.

API requests are limited to one per second.


## Assumptions and simplifications

Sync happen not too frequently.
So doing them sequentially is ok.
Re-ready the config file from disk is ok. It provides a nice benefit of allowing changes to the config file while the system is running and the system picks them up automatically.
Since syncs don't happen that often and Google Sheets are not that big in size that rewrite tables completely is ok.


## Roadmap

- move -main to cli instead of core
- move system to core
- separate auth-only and no-server into their own functions instead of if statements in current worker
- Move options validation to core
- not happy with exposing mount, build system yourself
  - pass throttler explicitly
  - build web server from core
  - replace sys-exit with stop, allow passing :not-ok, on stop notify
- separate core and worker
- add core/generate-config
- separate db and store (sql and logic)
- rename try-http
- pass oauth urls via options (for using proxy server etc)
- don't call config from oauth but from worker
- validate public api with pre assertions
- prometheus metrics
  - duration
  - target and table as labels
  - column count
  - row count
- code docs
- setup instructions
- allow sheet + range specification
- github release
- circle ci
- support mysql
- support sqlite


## Installation

java 8+


## Usage

- run init command
- fill out config json
- start server
- visit oauth url and confirm

    java -Xmx100m -Xms30m -jar target/uberjar/googlesheets-sql-sync-0.1.0-standalone.jar googlesheets_sql_sync.json

    lein repl

    lein uberjar

### Use a library

Use `googlesheets-sql-sync.system` and `googlesheets-sql-sync.web`

Overwrite `'#googlesheets-sql-sync.log/println` to disable or modify logging


## License

[MIT](./LICENSE)

