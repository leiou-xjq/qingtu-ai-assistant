-- 修复admin密码为123456
UPDATE user SET password = 'c3312ce1d5651727540351303d1a5542' WHERE username = 'admin';