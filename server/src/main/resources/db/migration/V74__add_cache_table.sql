CREATE TABLE "cache"
(
    "key"        text,
    "value"      jsonb                    NOT NULL,
    "updated_at" timestamp with time zone NOT NULL,
    "expires_at" timestamp with time zone NOT NULL,
    PRIMARY KEY ("key")
);
