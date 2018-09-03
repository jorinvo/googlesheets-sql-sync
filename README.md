# googlesheets-sql-sync

Easily keep your SQL database in sync with Google Sheets.

Use this to let users manually insert data using Google Sheets
while using all available SQL tooling for further processing.

SQL uses JDBC and bundles the PostgreSQL driver.
Additional drivers can be added any time.
If you sqlite or mysql support open an issue and support can be added in no time.


## Roadmap

- split code in files
- robust error handling
- sheet-title default to first sheet ?
- better sql statements
- allow sheet + range specification
- detect schema change
  - log to stderr
  - send slack alert (default is no overwrite)
  - add column works without conflict (can still slack notify)
- spec for config
- validate interval > 0
- validate targets exist
- add optional has-header-row and add optional headers (which must be set when has-header-row = false)
- refresh token in a smarter way
- logging
- prometheus metrics
  - duration
  - target and table as labels
  - column count
  - row count
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



    lein repl

    lein uberjar


## License

[MIT](./LICENSE)

