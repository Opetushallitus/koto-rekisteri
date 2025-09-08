CREATE TABLE "koski_error"
(
    "id"        integer GENERATED ALWAYS AS IDENTITY,
    "entity"    text                     NOT NULL,
    "message"   text                     NOT NULL,
    "timestamp" timestamp with time zone NOT NULL DEFAULT now(),
    PRIMARY KEY ("id"),
    UNIQUE ("entity")
);
