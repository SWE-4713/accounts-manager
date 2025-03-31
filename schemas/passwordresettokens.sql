CREATE TABLE password_reset_token (
    id BIGINT NOT NULL IDENTITY(1,1) PRIMARY KEY,
    token VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    expiryDate DATETIME NOT NULL
    CONSTRAINT fk_password_reset_token_user FOREIGN KEY (user_id) REFERENCES dbo.[user](id)
);
