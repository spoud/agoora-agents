INSERT INTO t_city (city_uuid, label, meta, created, created_by, updated, updated_by)
VALUES ('7bb2fdb0-8f05-44e8-b062-8a7d94d83b47', 'Bern', '{"strange_language":true}', NOW(), 'script', NOW(), 'script'),
       ('22099942-b10c-44ac-96d8-6e746caae391', 'Freibourg', '{"on_roschtigraben":true}', NOW(), 'script', NOW(),
        'script'),
       ('55fd863c-fb7e-4354-aea6-509611110bfd', 'Marly', '{"best_town":true}', NOW(), 'script', NOW(), 'script');

INSERT INTO t_address(address_uuid, label, line1, line2, meta, created, created_by, updated, updated_by, city_uuid)
VALUES ('7a94f53e-53c3-41fc-ac68-c224b4afc35b', 'SPOUD AG', 'Effingerstrasse 23', NULL, '{}', NOW(), 'script', NOW(),
        'script', '7bb2fdb0-8f05-44e8-b062-8a7d94d83b47'),
       ('ad103115-abed-49c4-b8cf-476f3daccccf', 'Gaétan', 'Street not found', NULL, '{}', NOW(), 'script', NOW(),
        'script', '55fd863c-fb7e-4354-aea6-509611110bfd');
