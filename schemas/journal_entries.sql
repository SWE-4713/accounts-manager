-- Create Journal Entries Table
DROP TABLE IF EXISTS journal_entries;
CREATE TABLE journal_entries (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    entry_date DATETIME NOT NULL,
    description VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    manager_comment VARCHAR(255),
    entry_comment VARCHAR(500),
    created_by VARCHAR(255)
);