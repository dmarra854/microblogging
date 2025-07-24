-- src/main/resources/data.sql

-- Clear existing data (optional, useful for repeated runs during development)
-- On application restart, if using H2 in-memory, the DB is fresh, so TRUNCATE isn't strictly necessary for a clean slate.
-- However, it's good for shared/persistent databases if you use this for more than H2.
DELETE FROM tweets;
DELETE FROM follows;
DELETE FROM "users"; -- "user" might be a reserved keyword in some DBs, hence quotes

-- 1. Insert Users
-- Passwords are plaintext here for simplicity; in real app, they would be hashed
INSERT INTO "users" (id, username, password_hash, email, created_at) VALUES
('11111111-1111-1111-1111-111111111111', 'alice', '$2a$10$abcdefghijklmnopqrstuvwxyz', 'alice@example.com', '2023-01-01 10:00:00'),
('22222222-2222-2222-2222-222222222222', 'bob',   '$2a$10$abcdefghijklmnopqrstuvwxyz', 'bob@example.com',   '2023-01-02 11:00:00'),
('33333333-3333-3333-3333-333333333333', 'charlie', '$2a$10$abcdefghijklmnopqrstuvwxyz', 'charlie@example.com', '2023-01-03 12:00:00'),
('44444444-4444-4444-4444-444444444444', 'mick', '$2a$10$abcdefghijklmnopqrstuvwxyz', 'mick@example.com', '2023-01-04 12:00:00');
-- 2. Insert Follow Relationships
-- Alice (1) follows Bob (2)
INSERT INTO follows (follower_id, followee_id) VALUES
('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222');

-- Bob (2) follows Alice (1) and Charlie (3)
INSERT INTO follows (follower_id, followee_id) VALUES
('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111'),
('22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333');

-- Charlie (3) follows Alice (1)
INSERT INTO follows (follower_id, followee_id) VALUES
('33333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111111');

-- 3. Insert Tweets
-- Tweets by Alice
INSERT INTO tweets (id, user_id, content, created_at) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', 'Hello Twitter, this is Alice!', '2024-07-22 10:00:00'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '11111111-1111-1111-1111-111111111111', 'Just posted my second tweet! #microblogging', '2024-07-22 10:05:00'),
('cccccccc-cccc-cccc-cccc-cccccccccccc', '11111111-1111-1111-1111-111111111111', 'Enjoying the sun today!', '2024-07-22 10:15:00');

-- Tweets by Bob
INSERT INTO tweets (id, user_id, content, created_at) VALUES
('dddddddd-dddd-dddd-dddd-dddddddddddd', '22222222-2222-2222-2222-222222222222', 'Bob is here! What''s up, world?', '2024-07-22 10:02:00'),
('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', '22222222-2222-2222-2222-222222222222', 'Thinking about microservices architecture...', '2024-07-22 10:10:00'),
('ffffffff-ffff-ffff-ffff-ffffffffffff', '22222222-2222-2222-2222-222222222222', 'Anyone for coffee?', '2024-07-22 10:20:00');

-- Tweets by Charlie
INSERT INTO tweets (id, user_id, content, created_at) VALUES
('44444444-4444-4444-4444-444444444444', '33333333-3333-3333-3333-333333333333', 'Charlie''s first tweet!', '2024-07-22 10:07:00');