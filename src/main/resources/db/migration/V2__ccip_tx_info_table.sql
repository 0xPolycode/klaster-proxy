CREATE TYPE klaster_proxy.CCIP_TX_TYPE AS ENUM ('OTHER', 'WALLET_CREATE', 'ERC20_TRANSFER', 'NATIVE_TRANSFER');

CREATE TABLE klaster_proxy.ccip_tx_info (
    chain_id          BIGINT        NOT NULL,
    tx_hash           VARCHAR       NOT NULL,
    tx_type           CCIP_TX_TYPE  NOT NULL,
    fn_signature      VARCHAR       NOT NULL,
    block_number      BIGINT        NOT NULL,
    controller_wallet VARCHAR       NOT NULL,
    tx_value          NUMERIC(78)   NOT NULL,
    tx_date           TIMESTAMPTZ   NOT NULL,
    dest_chains       NUMERIC(78)[] CONSTRAINT non_null CHECK (array_position(dest_chains, NULL) IS NULL),
    salt              VARCHAR,
    token_address     VARCHAR,
    token_receiver    VARCHAR,
    token_amount      NUMERIC(78),
    PRIMARY KEY (chain_id, tx_hash)
);

CREATE INDEX ON klaster_proxy.ccip_tx_info(chain_id);
CREATE INDEX ON klaster_proxy.ccip_tx_info(tx_hash);
CREATE INDEX ON klaster_proxy.cached_send_rtc_event(chain_id);
CREATE INDEX ON klaster_proxy.cached_send_rtc_event(tx_hash);
CREATE INDEX ON klaster_proxy.cached_send_rtc_event(chain_id, tx_hash);
