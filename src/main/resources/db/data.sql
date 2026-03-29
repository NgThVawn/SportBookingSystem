INSERT IGNORE INTO membership_levels (name, min_bookings, discount_rate, description, badge_color)
VALUES
  ('NONE',    0,   0.0000, 'Thành viên thường',    '#888888'),
  ('SILVER',  10,  0.0500, 'Bạc — giảm 5%',        '#C0C0C0'),
  ('GOLD',    30,  0.0700, 'Vàng — giảm 7%',        '#FFD700'),
  ('DIAMOND', 100, 0.1000, 'Kim cương — giảm 10%', '#B9F2FF');

INSERT IGNORE INTO roles (name) VALUES ('USER'), ('OWNER'), ('ADMIN');