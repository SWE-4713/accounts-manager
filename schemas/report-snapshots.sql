DROP TABLE IF EXISTS report_snapshots;
CREATE TABLE report_snapshots (
  id             BIGINT       AUTO_INCREMENT PRIMARY KEY,
  report_type    VARCHAR(10)  NOT NULL,
  start_date     DATE         NOT NULL,
  end_date       DATE         NOT NULL,
  generated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  payload_json   LONGTEXT     NOT NULL,
  INDEX idx_report_range (report_type, start_date, end_date)
);