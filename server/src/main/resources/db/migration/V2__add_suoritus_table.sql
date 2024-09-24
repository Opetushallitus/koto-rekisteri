create table suoritus
(
    id                     serial primary key,
    oppija_id              integer references oppija (id),
    oid                    text  unique not null,
    data                   jsonb not null,
    sisaltava_suoritus_oid text references suoritus (oid),
    constraint not_self_referencing check ( sisaltava_suoritus_oid != oid )
);

create index suoritus_oid_index on suoritus (oid);
