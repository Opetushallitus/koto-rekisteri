ALTER TABLE "koski_error"
    RENAME TO "koski_error_tmp";

CREATE TABLE "koski_error"
(
    "id"        text                     NOT NULL,
    "entity"    text                     NOT NULL,
    "message"   text                     NOT NULL,
    "timestamp" timestamp with time zone NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX "koski_error_id_entity_idx" ON "koski_error" ("id", "entity");

INSERT INTO koski_error
SELECT split_part(entity, ':', 2) AS id,
       split_part(entity, ':', 1) AS entity,
       message,
       timestamp
FROM "koski_error_tmp";

DROP TABLE "koski_error_tmp";