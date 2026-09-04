-- Seed data for attendance tables

-- Attendance Status Enum
INSERT INTO attendance_status_enum (status) VALUES 
    ('PRESENT'), 
    ('LATE'), 
    ('INSUFFICIENT_HOURS'), 
    ('ABSENT'), 
    ('REMOTE'), 
    ('ON_LEAVE'), 
    ('PUBLIC_HOLIDAY')
ON CONFLICT (status) DO NOTHING;

-- Sample Public Holidays (2026)
INSERT INTO public_holiday (id, building_id, holiday_date, name, created_at, updated_at) VALUES 
    ('a1b2c3d4-0001-0001-0001-000000000001', '11111111-1111-1111-1111-111111111111', '2026-01-01', 'New Year''s Day', NOW(), NOW()),
    ('a1b2c3d4-0001-0001-0001-000000000002', '11111111-1111-1111-1111-111111111111', '2026-01-06', 'Epiphany', NOW(), NOW()),
    ('a1b2c3d4-0001-0001-0001-000000000003', '11111111-1111-1111-1111-111111111111', '2026-03-06', 'National Independence Day', NOW(), NOW()),
    ('a1b2c3d4-0001-0001-0001-000000000004', '11111111-1111-1111-1111-111111111111', '2026-04-03', 'Good Friday', NOW(), NOW()),
    ('a1b2c3d4-0001-0001-0001-000000000005', '11111111-1111-1111-1111-111111111111', '2026-04-06', 'Easter Monday', NOW(), NOW()),
    ('a1b2c3d4-0001-0001-0001-000000000006', '11111111-1111-1111-1111-111111111111', '2026-05-01', 'Labour Day', NOW(), NOW()),
    ('a1b2c3d4-0001-0001-0001-000000000007', '11111111-1111-1111-1111-111111111111', '2026-06-25', ' Eid al-Adha', NOW(), NOW()),
    ('a1b2c3d4-0001-0001-0001-000000000008', '11111111-1111-1111-1111-111111111111', '2026-08-04', 'Republic Day', NOW(), NOW()),
    ('a1b2c3d4-0001-0001-0001-000000000009', '11111111-1111-1111-1111-111111111111', '2026-12-25', 'Christmas Day', NOW(), NOW()),
    ('a1b2c3d4-0001-0001-0001-000000000010', '11111111-1111-1111-1111-111111111111', '2026-12-26', 'Boxing Day', NOW(), NOW())
ON CONFLICT DO NOTHING;

-- Sample Attendance Records (optional test data)
INSERT INTO attendance_record (id, user_id, building_id, date, status, check_in_time, check_out_time, total_hours, created_at, updated_at) VALUES 
    ('b1b2c3d4-0001-0001-0001-000000000001', '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', CURRENT_DATE, 'PRESENT', '09:00:00', '17:00:00', 8.00, NOW(), NOW()),
    ('b1b2c3d4-0001-0001-0001-000000000002', '22222222-2222-2222-2222-222222222223', '11111111-1111-1111-1111-111111111111', CURRENT_DATE, 'LATE', '09:15:00', '17:00:00', 7.75, NOW(), NOW())
ON CONFLICT DO NOTHING;

-- Sample Attendance Stamp Logs (job execution logs)
INSERT INTO job_execution_log (id, job_execution_id, action, stamp_type, employee_id, timestamp, status, raw_data, error_message, created_at) VALUES
    ('c1c2c3d4-0001-0001-0001-000000000001', 'EXEC001', 'PROCESSED', 'CHECK_IN', 'EMP001', NOW(), 'SUCCESS', '{"employeeId": "EMP001", "timestamp": "09:00:00"}', NULL, NOW()),
    ('c1c2c3d4-0001-0001-0001-000000000002', 'EXEC001', 'PROCESSED', 'CHECK_OUT', 'EMP001', NOW(), 'SUCCESS', '{"employeeId": "EMP001", "timestamp": "17:00:00"}', NULL, NOW())
ON CONFLICT DO NOTHING;