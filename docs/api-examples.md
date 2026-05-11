# Veya API Examples

Base URL:

```bash
BASE_URL=http://localhost:8080
```

## Register

```bash
curl -sS -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Aigerim Veya",
    "email": "aigerim@example.com",
    "password": "password123",
    "familyName": "Veya Family"
  }'
```

## Login

```bash
curl -sS -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "aigerim@example.com",
    "password": "password123"
  }'
```

```bash
export ACCESS_TOKEN="<accessToken from auth response>"
```

## Get Current Family

```bash
curl -sS "$BASE_URL/api/families/current" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

## Create Task

```bash
curl -sS -X POST "$BASE_URL/api/tasks" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Buy medicine",
    "description": "Pick up vitamins from pharmacy",
    "priority": "HIGH",
    "dueDate": "2026-05-12",
    "dueTime": "19:00",
    "repeatType": "NONE"
  }'
```

## List Tasks

```bash
curl -sS "$BASE_URL/api/tasks" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

## Update Task Status

```bash
export TASK_ID="<task id>"

curl -sS -X PATCH "$BASE_URL/api/tasks/$TASK_ID/status" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "DONE",
    "comment": "Completed"
  }'
```

## Add Shopping Item

```bash
curl -sS -X POST "$BASE_URL/api/shopping" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "молоко",
    "quantity": "2",
    "category": "groceries"
  }'
```

## Toggle Shopping Item

```bash
export SHOPPING_ITEM_ID="<shopping item id>"

curl -sS -X PATCH "$BASE_URL/api/shopping/$SHOPPING_ITEM_ID/toggle" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

## Create Reminder

```bash
curl -sS -X POST "$BASE_URL/api/reminders" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Call grandma",
    "description": "Weekly check-in",
    "reminderDate": "2026-05-15",
    "reminderTime": "20:00",
    "repeatType": "WEEKLY"
  }'
```

## Parse AI Command

```bash
curl -sS -X POST "$BASE_URL/api/ai/parse" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "text": "добавь молоко и хлеб"
  }'
```

```bash
export AI_COMMAND_ID="<commandId from parse response>"
```

## Create Entity From AI Command

```bash
curl -sS -X POST "$BASE_URL/api/ai/commands/$AI_COMMAND_ID/create" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

## List Notifications

```bash
curl -sS "$BASE_URL/api/notifications" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```
