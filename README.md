# googlesheets-sql-sync

Easily keep your SQL database in sync with Google Sheets.

Use this to let users manually insert data using Google Sheets
while using all available SQL tooling for further processing.

SQL uses JDBC and bundles the PostgreSQL driver.
Additional drivers can be added any time.
If you sqlite or mysql support open an issue and support can be added in no time.


## Roadmap

- config init command for oauth
  - client-id client-secret [path]
  - use expires-in to refresh access token periodically
- logging
- detect schema change
  - log to stderr
  - send slack alert (default is no overwrite)
  - add column works without conflict (can still slack notify)
- spec for config
- option to disable init
- use transaction for db
- optional pass path for json init
- validate interval > 0
- validate targets exist
- check for existing config file before init
- add optional has-header-row
- sheet-title default to first sheet
- allow range specification
- allow sheet specification
- guess column types (start with all text)
- flexible targets
- prometheus metrics
  - duration
  - target and table as labels
  - column count
  - row count
- find spreadsheet-title
- support sheet-id
- detect sheet-id
- test.check
- circle ci
- github release
- support mysql
- support sqlite



## Installation

java 8+


## Usage



    lein repl

    lein uberjar


## License

[MIT](./LICENSE)

