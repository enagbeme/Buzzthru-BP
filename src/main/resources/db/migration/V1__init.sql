CREATE TABLE locations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    type VARCHAR(30) NOT NULL,
    address VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE devices (
    id BIGSERIAL PRIMARY KEY,
    device_uuid VARCHAR(64) NOT NULL UNIQUE,
    location_id BIGINT NOT NULL REFERENCES locations(id),
    computer_name VARCHAR(200),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    registered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE employees (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(160) NOT NULL,
    pin_hash VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE time_entries (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    location_id BIGINT NOT NULL REFERENCES locations(id),
    device_id BIGINT NOT NULL REFERENCES devices(id),
    clock_in_time TIMESTAMP NOT NULL,
    clock_out_time TIMESTAMP,
    edited BOOLEAN NOT NULL DEFAULT FALSE,
    edited_by BIGINT REFERENCES employees(id),
    edit_reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_time_entries_employee_open ON time_entries(employee_id, clock_out_time);
CREATE INDEX idx_time_entries_location_in ON time_entries(location_id, clock_in_time);

CREATE TABLE time_entry_audits (
    id BIGSERIAL PRIMARY KEY,
    time_entry_id BIGINT NOT NULL REFERENCES time_entries(id),
    edited_by BIGINT REFERENCES employees(id),
    edited_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    field_name VARCHAR(60) NOT NULL,
    old_value VARCHAR(255),
    new_value VARCHAR(255)
);
