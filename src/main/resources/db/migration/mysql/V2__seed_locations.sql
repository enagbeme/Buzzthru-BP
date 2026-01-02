INSERT INTO locations (name, type, address, active)
SELECT 'Buzzthru Laundromat 1', 'LAUNDRY', NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM locations WHERE name = 'Buzzthru Laundromat 1');

INSERT INTO locations (name, type, address, active)
SELECT 'Buzzthru Laundromat 2', 'LAUNDRY', NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM locations WHERE name = 'Buzzthru Laundromat 2');

INSERT INTO locations (name, type, address, active)
SELECT 'BP Gas Station', 'GAS_STATION', NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM locations WHERE name = 'BP Gas Station');

UPDATE locations SET name = 'Buzzthru Laundromat 1' WHERE name = 'Laundry 1';
UPDATE locations SET name = 'Buzzthru Laundromat 2' WHERE name = 'Laundry 2';
UPDATE locations SET name = 'BP Gas Station' WHERE name = 'Gas Station';
