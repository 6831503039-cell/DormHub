-- Sample data. IDs are generated in insert order (rooms 1-8), so tenants/bills/repairs refer to them below.
INSERT INTO Room (roomNumber, floor, roomType, monthlyRent, status) VALUES ('101', 1, 'แอร์', 4500, 'OCCUPIED');
INSERT INTO Room (roomNumber, floor, roomType, monthlyRent, status) VALUES ('102', 1, 'แอร์', 4500, 'OCCUPIED');
INSERT INTO Room (roomNumber, floor, roomType, monthlyRent, status) VALUES ('103', 1, 'พัดลม', 3000, 'AVAILABLE');
INSERT INTO Room (roomNumber, floor, roomType, monthlyRent, status) VALUES ('104', 1, 'พัดลม', 3000, 'MAINTENANCE');
INSERT INTO Room (roomNumber, floor, roomType, monthlyRent, status) VALUES ('201', 2, 'แอร์', 4800, 'OCCUPIED');
INSERT INTO Room (roomNumber, floor, roomType, monthlyRent, status) VALUES ('202', 2, 'แอร์', 4800, 'AVAILABLE');
INSERT INTO Room (roomNumber, floor, roomType, monthlyRent, status) VALUES ('203', 2, 'พัดลม', 3200, 'AVAILABLE');
INSERT INTO Room (roomNumber, floor, roomType, monthlyRent, status) VALUES ('204', 2, 'แอร์', 5000, 'OCCUPIED');

INSERT INTO Tenant (firstName, lastName, phone, email, checkInDate, room_id) VALUES ('สมชาย', 'ใจดี', '081-111-1111', 'somchai@example.com', '2026-06-01', 1);
INSERT INTO Tenant (firstName, lastName, phone, email, checkInDate, room_id) VALUES ('สมศักดิ์', 'มั่นคง', '081-222-2222', 'somsak@example.com', '2026-06-01', 1);
INSERT INTO Tenant (firstName, lastName, phone, email, checkInDate, room_id) VALUES ('สุดา', 'รักเรียน', '082-333-3333', 'suda@example.com', '2026-07-15', 2);
INSERT INTO Tenant (firstName, lastName, phone, email, checkInDate, room_id) VALUES ('มานี', 'มีสุข', '083-444-4444', 'manee@example.com', '2026-08-01', 5);
INSERT INTO Tenant (firstName, lastName, phone, email, checkInDate, room_id) VALUES ('ปิติ', 'ยินดี', '084-555-5555', 'piti@example.com', '2026-09-01', 8);

INSERT INTO Bill (room_id, billMonth, rent, waterUnits, waterRate, electricUnits, electricRate, paid) VALUES (1, '2026-09', 4500, 6, 18, 120, 8, TRUE);
INSERT INTO Bill (room_id, billMonth, rent, waterUnits, waterRate, electricUnits, electricRate, paid) VALUES (2, '2026-09', 4500, 4, 18, 95, 8, TRUE);
INSERT INTO Bill (room_id, billMonth, rent, waterUnits, waterRate, electricUnits, electricRate, paid) VALUES (5, '2026-09', 4800, 5, 18, 140, 8, TRUE);
INSERT INTO Bill (room_id, billMonth, rent, waterUnits, waterRate, electricUnits, electricRate, paid) VALUES (1, '2026-10', 4500, 7, 18, 132, 8, FALSE);
INSERT INTO Bill (room_id, billMonth, rent, waterUnits, waterRate, electricUnits, electricRate, paid) VALUES (2, '2026-10', 4500, 5, 18, 101, 8, TRUE);
INSERT INTO Bill (room_id, billMonth, rent, waterUnits, waterRate, electricUnits, electricRate, paid) VALUES (5, '2026-10', 4800, 6, 18, 150, 8, FALSE);
INSERT INTO Bill (room_id, billMonth, rent, waterUnits, waterRate, electricUnits, electricRate, paid) VALUES (8, '2026-10', 5000, 3, 18, 88, 8, FALSE);

INSERT INTO RepairRequest (room_id, title, description, reportedDate, status) VALUES (4, 'พัดลมเพดานไม่หมุน', 'เปิดแล้วมีเสียงแต่ใบพัดไม่หมุน', '2026-10-10', 'IN_PROGRESS');
INSERT INTO RepairRequest (room_id, title, description, reportedDate, status) VALUES (2, 'ก๊อกน้ำห้องน้ำรั่ว', 'น้ำหยดตลอดเวลา', '2026-10-18', 'PENDING');
INSERT INTO RepairRequest (room_id, title, description, reportedDate, status) VALUES (5, 'หลอดไฟหน้าห้องขาด', NULL, '2026-09-20', 'DONE');
