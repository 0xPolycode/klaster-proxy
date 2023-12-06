CREATE TABLE klaster_proxy.cached_send_rtc_event (
    chain_id       BIGINT  NOT NULL,
    tx_hash        VARCHAR NOT NULL,
    block_number   BIGINT  NOT NULL,
    message_id     VARCHAR NOT NULL,
    caller_address VARCHAR NOT NULL
);

CREATE INDEX ON klaster_proxy.cached_send_rtc_event(caller_address);

CREATE TABLE klaster_proxy.latest_fetched_send_rtc_event_block_number (
    chain_id     BIGINT NOT NULL PRIMARY KEY,
    block_number BIGINT NOT NULL
);
