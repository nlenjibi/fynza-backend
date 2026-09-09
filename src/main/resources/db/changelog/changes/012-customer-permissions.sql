--liquibase formatted sql
--changeset fynza:012-customer-permissions dbms:postgresql

INSERT INTO permissions (code, resource, action, description) VALUES
    ('customer.read.own',   'customer', 'read.own',   'View own customer profile'),
    ('customer.update.own', 'customer', 'update.own', 'Update own customer profile'),
    ('address.read.own',    'address',  'read.own',   'View own customer addresses'),
    ('address.create.own',  'address',  'create.own', 'Add a customer address'),
    ('address.update.own',  'address',  'update.own', 'Update own customer address'),
    ('address.delete.own',  'address',  'delete.own', 'Delete own customer address'),
    ('customer.read',       'customer', 'read',       'Admin: view any customer profile'),
    ('customer.manage',     'customer', 'manage',     'Admin: full customer management'),
    ('customer.suspend',    'customer', 'suspend',    'Admin: suspend a customer account'),
    ('customer.activate',   'customer', 'activate',   'Admin: reactivate a customer account'),
    ('customer.block',      'customer', 'block',      'Admin: permanently block a customer account')
ON CONFLICT (code) DO NOTHING;

--rollback DELETE FROM permissions WHERE code IN ('customer.read.own','customer.update.own','address.read.own','address.create.own','address.update.own','address.delete.own','customer.read','customer.manage','customer.suspend','customer.activate','customer.block');
