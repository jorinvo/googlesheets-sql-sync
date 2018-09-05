# googlesheets-sql-sync

Keep your SQL database in sync with Google Sheets.

Use this to let users manually insert data using Google Sheets
while using all available SQL tooling for further processing.

SQL uses JDBC and bundles the PostgreSQL driver.
Additional drivers can be added any time.
If you sqlite or mysql support open an issue and support can be added in no time.


## Roadmap

- robust error handling
  - config file not exists / malformed / permissions / write
  - db
- does sheet-title default to first sheet ?
- trigger immediate sync via SIGALRM
- ensure only one of do-sync and handle-code can happen at a time
- throttle API requests
- logging
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
- if localhost, use https://clojuredocs.org/clojure.java.browse



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

