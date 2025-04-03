-- Create Journal Entry Lines Table
DROP TABLE IF EXISTS tbl_journal_entry_lines;
CREATE TABLE journal_entry_lines (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     account_id BIGINT,
                                     debit DECIMAL(15,2) DEFAULT 0,
                                     credit DECIMAL(15,2) DEFAULT 0,
                                     description VARCHAR(255),
                                     journal_entry_id BIGINT,
                                     CONSTRAINT fk_account FOREIGN KEY (account_id) REFERENCES accounts(id),
                                     CONSTRAINT fk_journal_entry FOREIGN KEY (journal_entry_id) REFERENCES journal_entry(id)
);