SELECT setval(
    pg_get_serial_sequence('app_users', 'id'),
    COALESCE((SELECT MAX(id) FROM app_users), 1),
    true
);
