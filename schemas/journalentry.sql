DROP TABLE IF EXISTS JournalEntry;
CREATE TABLE dbo.JournalEntry (
    id BIGINT NOT NULL IDENTITY(1,1) PRIMARY KEY,
    entryDate DATE NOT NULL,
    account_id BIGINT NOT NULL,
    debit DECIMAL(15,2) NULL,
    credit DECIMAL(15,2) NULL,
    description VARCHAR(500),
    status VARCHAR(20) NOT NULL,  -- should be one of 'PENDING','APPROVED','REJECTED'
    comment VARCHAR(500),         -- for rejection reason
    postReference VARCHAR(50),    -- a generated reference code, if desired
    CONSTRAINT FK_JournalEntry_Account FOREIGN KEY (account_id)
        REFERENCES dbo.[Account](id)
);
