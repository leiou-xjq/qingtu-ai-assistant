#!/bin/bash
# =============================================
# 青途智伴AI助手 - 测试数据初始化脚本
# 用法：
#   1. 启动后端: mvn spring-boot:run (在 backend 目录)
#   2. 执行本脚本: bash stress-test/init-test-data.sh
#   3. 验证: cat stress-test/user-tokens.csv
# =============================================

API_BASE="http://localhost:8080/api"
TOKEN_FILE="stress-test/user-tokens.csv"

echo "========================================="
echo "  青途智伴 - 测试数据准备"
echo "  API: $API_BASE"
echo "========================================="

# 初始化 CSV 文件
echo "USER_ID,TOKEN" > "$TOKEN_FILE"

# 1. 注册 10 个测试用户并登录获取 Token
for i in $(seq 1 10); do
  USERNAME="testuser${i}"
  echo ""
  echo "--- 用户 $i: $USERNAME ---"

  # 注册
  echo "  注册..."
  REGISTER=$(curl -s -X POST "${API_BASE}/user/register" \
    -H "Content-Type: application/json" \
    -d "{
      \"username\": \"${USERNAME}\",
      \"password\": \"123456\",
      \"nickname\": \"测试用户${i}\"
    }")
  echo "  Register: $REGISTER"

  # 登录
  echo "  登录..."
  LOGIN=$(curl -s -X POST "${API_BASE}/user/login" \
    -H "Content-Type: application/json" \
    -d "{
      \"username\": \"${USERNAME}\",
      \"password\": \"123456\"
    }")
  echo "  Login: $LOGIN"

  # 提取 Token (假设返回格式为 { "data": { "token": "xxx" } })
  TOKEN=$(echo "$LOGIN" | grep -o '"token":"[^"]*"' | head -1 | sed 's/"token":"//;s/"//')
  USER_ID=$(echo "$LOGIN" | grep -o '"userId":[0-9]*' | head -1 | sed 's/"userId"://')

  if [ -n "$TOKEN" ] && [ -n "$USER_ID" ]; then
    echo "$USER_ID,$TOKEN" >> "$TOKEN_FILE"
    echo "  Token 已保存: $USER_ID"

    # 为每个用户添加课程
    echo "  添加课程..."
    COURSES=("高等数学" "大学英语" "数据结构" "数据库原理" "操作系统")
    for wday in $(seq 1 5); do
      curl -s -X POST "${API_BASE}/course/add" \
        -H "Content-Type: application/json" \
        -H "Authorization: $TOKEN" \
        -d "{
          \"courseName\": \"${COURSES[$((wday-1))]}\",
          \"weekday\": $wday,
          \"startTime\": \"08:00\",
          \"endTime\": \"09:40\",
          \"location\": \"教学楼${wday}01\",
          \"teacher\": \"张老师\"
        }" > /dev/null
    done

    # 为每个用户添加消费记录
    echo "  添加消费记录..."
    CATEGORIES=("饮食" "交通" "购物" "娱乐" "其他")
    for j in $(seq 1 10); do
      AMOUNT=$((RANDOM % 200 + 10))
      curl -s -X POST "${API_BASE}/cost/add" \
        -H "Content-Type: application/json" \
        -H "Authorization: $TOKEN" \
        -d "{
          \"amount\": ${AMOUNT}.00,
          \"category\": \"${CATEGORIES[$((RANDOM % 5))]}\",
          \"description\": \"测试消费${j}\"
        }" > /dev/null
    done
  else
    echo "  [WARNING] 登录失败，跳过此用户"
  fi
done

echo ""
echo "========================================="
echo "  测试数据准备完成!"
echo "  Token 文件: $TOKEN_FILE"
echo "  用户数量: $(tail -n +2 $TOKEN_FILE | wc -l)"
echo "========================================="
cat "$TOKEN_FILE"
