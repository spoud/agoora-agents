CREATE TABLE t_city
(
    city_uuid  uuid         NOT NULL,
    label      varchar(255) NOT NULL,
    meta       text         NULL,
    created    timestamp    NOT NULL,
    updated    timestamp    NOT NULL,
    created_by varchar(255) NOT NULL DEFAULT 'UNKNOWN'::character varying,
    updated_by varchar(255) NOT NULL DEFAULT 'UNKNOWN'::character varying,
    CONSTRAINT t_city_pkey PRIMARY KEY (city_uuid),
    CONSTRAINT ux_t_city_label UNIQUE (label)
);
CREATE INDEX idx_city_label ON t_city USING btree (label);

CREATE TABLE t_address
(
    address_uuid uuid         NOT NULL,
    label        varchar(255) NOT NULL,
    line1        varchar(255) NOT NULL,
    line2        varchar(255) NULL,
    meta         text         NULL,
    created      timestamp    NOT NULL,
    updated      timestamp    NOT NULL,
    created_by   varchar(255) NOT NULL DEFAULT 'UNKNOWN'::character varying,
    updated_by   varchar(255) NOT NULL DEFAULT 'UNKNOWN'::character varying,
    city_uuid    uuid         NOT NULL,
    CONSTRAINT t_address_pkey PRIMARY KEY (address_uuid),
    CONSTRAINT ux_t_address_label UNIQUE (label),
    CONSTRAINT fk_city FOREIGN KEY (city_uuid) REFERENCES t_city (city_uuid)
);

CREATE INDEX idx_address_label ON t_address USING btree (label);
CREATE INDEX idx_address_city_uuid ON t_address USING btree (city_uuid);
