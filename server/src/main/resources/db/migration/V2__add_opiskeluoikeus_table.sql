CREATE TABLE IF NOT EXISTS opiskeluoikeus
(
    id                           serial primary key,
    oppija_id                    integer references oppija (id),
    oid                          text  not null,
    data                         jsonb not null,
    sisaltava_opiskeluoikeus_oid text
);
