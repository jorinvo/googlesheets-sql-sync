# googlesheets-sql-sync

Keep your SQL database in sync with Google Sheets.

Use this to let users manually insert data using Google Sheets
while using all available SQL tooling for further processing.

SQL uses JDBC and bundles the PostgreSQL driver.
Additional drivers can be added any time.
If you sqlite or mysql support open an issue and support can be added in no time.


## Assumptions and simplifications

Sync happen not too frequently.
So doing them sequentially is ok.
Re-ready the config file from disk is ok. It provides a nice benefit of allowing changes to the config file while the system is running and the system picks them up automatically.
Since syncs don't happen that often and Google Sheets are not that big in size that rewrite tables completely is ok.


## Roadmap

- throttle API requests
- logging
- does sheet-title default to first sheet ?
- prometheus metrics
  - duration
  - target and table as labels
  - column count
  - row count
- detect schema change
  - log to stderr
  - send slack alert (default is no overwrite)
  - add column works without conflict (can still slack notify)
- spec for config
- validate interval > 0
- validate targets exist
- Save creds separate to not conflict with config edits
- add optional has-header-row and add optional headers (which must be set when has-header-row = false)
- better sql statements
- allow sheet + range specification
- refresh token in a smarter way (not every single time)
- figure out how to wait for coroutines on close
- find spreadsheet-title
- test.check
- circle ci
- use transaction for db
- github release
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

require `googlesheets-sql-sync.system`


## License

[MIT](./LICENSE)

