-- Create Errors Table (for storing error messages)
DROP TABLE IF EXISTS Errors;
CREATE TABLE Errors (
    error_id INT NOT NULL,
    error_description VARCHAR(255),
    PRIMARY KEY (error_id)
);
