CREATE TABLE yki_suoritus_error
(
    id                  SERIAL PRIMARY KEY,
    message             TEXT        NOT NULL,
    context             TEXT        NOT NULL,
    exception_message   TEXT        NULL,
    stack_trace         TEXT        NULL,
    created             TIMESTAMP   NOT NULL
);