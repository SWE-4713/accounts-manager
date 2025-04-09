DROP TABLE IF EXISTS journal_entry_lines;
CREATE TABLE journal_entry_lines (
    id BIGINT NOT NULL IDENTITY(1,1) PRIMARY KEY,
    account_id BIGINT,
    debit DECIMAL(15,2) DEFAULT 0,
    credit DECIMAL(15,2) DEFAULT 0,
    description VARCHAR(255),
    journal_entry_id BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_account FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT fk_journal_entry FOREIGN KEY (journal_entry_id) REFERENCES journal_entries(id)
) 