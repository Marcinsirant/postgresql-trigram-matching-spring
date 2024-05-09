create table addresses
(
    address_id  uuid not null
        constraint pk_addresses
            primary key,
    street      varchar,
    city        varchar,
    postal_code varchar,
    house_no    varchar,
    building_no varchar,
    note        varchar
);

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX trgm_addresses ON addresses USING gin (street gin_trgm_ops,
                                                    city gin_trgm_ops,
                                                    note gin_trgm_ops);