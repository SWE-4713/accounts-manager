-- Create Errors Table (for storing error messages)
DROP TABLE IF EXISTS Errors;
CREATE TABLE errors (
                        `Error Id` INT NOT NULL,
                        `Error Description` VARCHAR(255),
                        PRIMARY KEY (`Error Id`)
);