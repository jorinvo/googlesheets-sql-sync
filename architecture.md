## settings

- auth only
- no server
- no metrics
- single sync


## inputs

- run on startup:
  - trigger sync
- system signals:
  - stop
  - trigger sync
- scheduler
  - triggers sync in interval
- web server
  - serve metrics
  - handle oauth replies


## effects and state

- googlesheet API HTTP calls
  - api rate limiting
  - http calls
  - only one call at a time
- metric storage
- worker: do one job at a time
- oauth store
  - store authentication in file
- config file: load config file from disk every time
- db: talk to database
- logger

## commander state machine

Current:
:sync
  > try
    :get-config
  > catch
    :log
    :fail
  > try
    refresh-access-token
      :log
      :get-auth-data
      > if refresh token
        :log
        > try
          :get-config
          :get-access-token
          :save-auth-data
        > catch
          :log
      > else
        :log
    > if access token
      :log
      :get-sheet-rows
      update-table
        > try
          :log
          :log
          > try
            :query-db
            > if table empty
              :log
              :update-db
          > catch
            ignore
          > if existing headers
            :log
            :update-db
          > else
            :log
            :update-db
          :log
          :insert-into-db
        > catch
          rethrow
      :log
      :inc-sync-metric
    > else if not no-server
      show-init-message
        :log
        > if local-redirect
          :browse-url
    > else
      :log
      :fail
  > catch
    :log



Required side effects:

:http
  :talk-to-google
:write-file :read-file
  :get-auth-data :save-auth-data
  :get-config :save-config
:query-db
:browse-url
:get-metrics :update-metrics
:log
:fail

Resources at edge of system:
- http-client
- file-system
- database
- metrics
- logger
- system







TODO spec for state

TODO should trigger-sync and timeout also go through machine?
  for single sync, better to not include
  when triggered via timeout, can schedule next timeout after, outside of machine
  when triggered via signal, needs to reset timeout after
  when triggered while still running, ignore trigger

TODO when sync failed, for a single sync that should be a hard fail
  timer should handle softening
-> this can be a separate state machine

TODO should stop go through machine?


TODO flatten+namespace maps where possible

actions can save their meta (like original http request) in the state, especially useful for errors


