ALTER TABLE oppija RENAME COLUMN name TO first_name;
ALTER TABLE oppija
    ADD COLUMN last_name TEXT NOT NULL,
    ADD COLUMN oid TEXT UNIQUE NOT NULL,
    ADD COLUMN hetu TEXT UNIQUE NOT NULL ,
    ADD COLUMN nationality TEXT,
    ADD COLUMN gender TEXT,
    ADD COLUMN address TEXT,
    ADD COLUMN postal_code TEXT,
    ADD COLUMN city TEXT,
    ADD COLUMN email TEXT;
