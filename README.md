# Google Sheets to SQL Sync

[![Build Status](https://travis-ci.org/jorinvo/googlesheets-sql-sync.svg?branch=master)](https://travis-ci.org/jorinvo/googlesheets-sql-sync)
[![cljdoc badge](https://cljdoc.org/badge/googlesheets-sql-sync/googlesheets-sql-sync)](https://cljdoc.org/d/googlesheets-sql-sync/googlesheets-sql-sync/CURRENT)
[![Clojars Project](https://img.shields.io/clojars/v/googlesheets-sql-sync.svg)](https://clojars.org/googlesheets-sql-sync)

Keep your SQL database in sync with Google Sheets using [googlesheets-sql-sync](https://github.com/jorinvo/googlesheets-sql-sync).

Let users manually insert data using Google Sheets while having the power of all available SQL tooling for further processing.

googlesheets-sql-sync uses [JDBC](https://github.com/clojure/java.jdbc) and bundles the PostgreSQL driver.
Additional drivers can be added any time.
If you would like to add support for SQLite, MySQL or any other SQL database, open an issue and it can probably be added in no time.


## Assumptions and simplifications

To simplify the task of synchronisation, the following assumptions are made:

- Sync happens not too frequently. Think minutes, not milliseconds.
- Number of tables to sync is not too high. Maybe a hundred but not a million.
- The Google Sheets are not too big. They might contain a thousand rows but not millions.
- Headers are mostly stable. It is an exception to rename, add or delete columns.

This allows for a few simplifications in the implementation:

- Do all specified sync tasks in sequence. There is enough time for this.
- Limit API requests to Google's API to one a second to prevent rate limits as much as possible.
- Truncate each table on every sync to ensure all changes are applied.
- Log error when table schema doesn't match headers from sheet and require the user to check error manually. Most likely the user simply drops the table in this case and moves on.
- Re-read the config file from disk before each sync interval, which allows for adjusting the config while the system is running.



## Setup

### Installation

1. Make sure you have Java 8+ installed. Check by running `java -version`
2. Download latest `googlesheets-sql-sync.jar` from [Github](https://github.com/jorinvo/googlesheets-sql-sync/releases).

### Setup Google Application

1. Create a [new Project](https://console.developers.google.com/projectcreate) or work in an existing one
2. Enable the [Sheets API](https://console.developers.google.com/apis/library/sheets.googleapis.com?q=sheets)
3. Setup your app's [OAuth consent screen](https://console.developers.google.com/apis/credentials/consent)
   If this is an organisation-internal service, you most likely want to set it as *internal* and select the *scope* `spreadsheets.readonly`.
4. Create a new [OAuth client ID](https://console.developers.google.com/apis/credentials/oauthclient) or use an existing one
  1. Set _"Application type"_ to _"Web application"_
  2. Set at least one correct _"Authorized redirect URI"_. To run googlesheets-sql-sync on your local machine with default settings use http://localhost:9955/oauth
  3. Keep _"Client ID"_ and _"Client secret"_ handy for later


### Usage

1. Create an empty config file

```
java -jar googlesheets-sql-sync.jar --init
```

2. Now fill out the missing information in the config file.
  1. Use your Google credentials from above.
  2. Specify at least one target and one sheet using that target.
  3. You can find more DB options in the [JDBC docs](https://jdbc.postgresql.org/documentation/head/connect.html).
  4. Name the `table` as you wish for it to appear in your database.
  5. To get a `spreadsheet_id`, open one of [your Google Sheets](https://docs.google.com/spreadsheets) and copy the part between `/d/` and `/edit` from the URL bar in your Browser.
  6. Specify the `range` using the `A1:Z10`. Skip the number to select all rows - like `A:ZZ`. You can also specify a _sheet_ if your spreadsheet contains multiple sheets by prefixing th range like `SomeSheet!A:ZZ`.
  For example, the `spreadsheet_id` for `https://docs.google.com/spreadsheets/d/1q5BNyL7-FnApmkjq45HlKPK-W-pdEmTrtpz0iaHm8p0/edit#gid=0`
  is `1q5BNyL7-FnApmkjq45HlKPK-W-pdEmTrtpz0iaHm8p0`.

3. Start the program with:

```
java -jar googlesheets-sql-sync.jar
```

4. You will be prompted to visit an OAuth URL to authorize and connect your Google Account.

5. After successful authorization, a first sync is triggered
   and further ones will occur in the specified interval.


### Running without Server

Often you don't want to open up another port just for OAuth of a small sync tool.
To work around this you can run `java -jar googlesheets-sql-sync.jar --auth-only` on your local machine, then copy the generated `googlesheets_sql_sync.auth.json` file to your server and on the server run `java -jar googlesheets-sql-sync.jar --no-server`


### Customization

The program can be configured using command line flags. To see available options, run:

```
java -jar googlesheets-sql-sync.jar --help
```


### Pitfalls

- When you authenticate a Google OAuth app, then throw away your `.auth.json` file and try to re-authenticate, Google for some reason will only send you `access_token` and `expires_in`, no `refresh_token`. To fix this go to https://myaccount.google.com/permissions remove the app's permission and try again.


Let me know if you run into any [issues](https://github.com/jorinvo/googlesheets-sql-sync/issues) or if you have any suggestions for improvements.


### Use as Clojure package

- You can generate a config file with `'#googlesheets-sql-sync.config/generate`
- Generate and validate options for running the system with `'#googlesheets-sql-sync.options/defaults` and `'#googlesheets-sql-sync.options/validate`
- Run the system with `'#googlesheets-sql-sync.core/start`
- Overwrite `'#googlesheets-sql-sync.log/info`, `'#googlesheets-sql-sync.log/warn`, `#googlesheets-sql-sync.log/error` to modify or disable logging.


## Development

- Make sure you have [Leinigen](https://leiningen.org/) installed.
- `lein test` Run tests
- `lein run` Run the whole system
- `lein repl` Start in dev mode with REPL enabled
  - Try out comments in [dev namespace](https://github.com/jorinvo/googlesheets-sql-sync/blob/master/dev/dev.clj)

### Building for production

- Run `lein uberjar`


## License

[MIT](https://github.com/jorinvo/googlesheets-sql-sync/blob/master/LICENSE)
