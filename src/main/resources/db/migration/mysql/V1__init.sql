CREATE TABLE locations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    type VARCHAR(30) NOT NULL,
    address VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE devices (
    id BIGINT NOT NULL AUTO_INCREMENT,
    device_uuid VARCHAR(64) NOT NULL,
    location_id BIGINT NOT NULL,
    computer_name VARCHAR(200),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    registered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_devices_device_uuid (device_uuid),
    CONSTRAINT fk_devices_location FOREIGN KEY (location_id) REFERENCES locations(id)
);

CREATE TABLE employees (
    id BIGINT NOT NULL AUTO_INCREMENT,
    full_name VARCHAR(160) NOT NULL,
    pin_hash VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE time_entries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    clock_in_time TIMESTAMP NOT NULL,
    clock_out_time TIMESTAMP NULL,
    edited BOOLEAN NOT NULL DEFAULT FALSE,
    edited_by BIGINT NULL,
    edit_reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_time_entries_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_time_entries_location FOREIGN KEY (location_id) REFERENCES locations(id),
    CONSTRAINT fk_time_entries_device FOREIGN KEY (device_id) REFERENCES devices(id),
    CONSTRAINT fk_time_entries_edited_by FOREIGN KEY (edited_by) REFERENCES employees(id)
);

CREATE INDEX idx_time_entries_employee_open ON time_entries(employee_id, clock_out_time);
CREATE INDEX idx_time_entries_location_in ON time_entries(location_id, clock_in_time);

CREATE TABLE time_entry_audits (
    id BIGINT NOT NULL AUTO_INCREMENT,
    time_entry_id BIGINT NOT NULL,
    edited_by BIGINT NULL,
    edited_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    field_name VARCHAR(60) NOT NULL,
    old_value VARCHAR(255),
    new_value VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT fk_time_entry_audits_time_entry FOREIGN KEY (time_entry_id) REFERENCES time_entries(id),
    CONSTRAINT fk_time_entry_audits_edited_by FOREIGN KEY (edited_by) REFERENCES employees(id)
);
