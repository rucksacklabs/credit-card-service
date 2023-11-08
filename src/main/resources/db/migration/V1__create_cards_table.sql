CREATE EXTENSION IF NOT EXISTS pgcrypto;

create table if not exists credit_cards
(
    id         uuid     PRIMARY KEY default gen_random_uuid(),
    number     bytea    not null,
    name       text     not null,
    expiry     text     not null,
    card_limit int      not null  default -1
);

create index credit_cards_number_index
    on credit_cards (number);