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

- How to keep running without server (wait for go routines)
- Drop table before create if it was empty
- does sheet-title default to first sheet ?
- use transaction for db
- github release
- code docs
- setup instructions
- get rid of lein
- prometheus metrics
  - duration
  - target and table as labels
  - column count
  - row count
- handle reload os sig to reload config (?)
- Save creds separate to not conflict with config edits
- add optional has-header-row and add optional headers (which must be set when has-header-row = false)
- allow sheet + range specification
- refresh token in a smarter way (not every single time)
- find spreadsheet-title
- circle ci
- support mysql
- support sqlite
- add column works without conflict


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


## License

[MIT](./LICENSE)

