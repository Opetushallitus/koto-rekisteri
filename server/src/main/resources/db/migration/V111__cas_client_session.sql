-- Maps a CAS service ticket (mapping_id) to a Spring Session id so that CAS
-- back-channel single logout can invalidate the DB-backed session. session_id is
-- intentionally NOT unique: one browser session can hold several CAS tickets
-- (see OPH KJHH-2045).
CREATE TABLE cas_client_session (
    mapping_id text PRIMARY KEY,
    session_id text NOT NULL
);

CREATE INDEX cas_client_session_session_id_idx ON cas_client_session (session_id);
