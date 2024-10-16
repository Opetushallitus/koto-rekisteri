alter table koto_suoritus
    add column courseid int not null default (-1);

alter table koto_suoritus
    add column coursename text not null default '';
