# task-connect-be

Backend cua TaskConnect. Mot ung dung Spring Boot duy nhat, chay trong mot tien trinh,
trien khai nhu mot don vi. Cac module nghiep vu goi nhau bang interface Java noi bo.

## Yeu cau

- JDK 17 tro len
- Maven 3.9 tro len
- Docker va Docker Compose

## Chay lan dau

```bash
cp .env.example .env          # sua mat khau truoc khi dung
docker compose up -d
docker compose ps             # cho ca ba service o trang thai healthy
mvn spring-boot:run
```

Ung dung chay o `http://localhost:8080`.
Swagger UI o `http://localhost:8080/swagger-ui.html`.
RabbitMQ management o `http://localhost:15672`.

## Ha tang

| Service | Image | Cong mac dinh |
|---|---|---|
| `mariadb` | `mariadb:11.4` | 3306 |
| `redis` | `redis:7-alpine` | 6379 |
| `rabbitmq` | `rabbitmq:3.13-management-alpine` | 5672, 15672 |

Toan he thong dung dung mot database: `taskconnect`.

Cong bi trung tren may thi doi trong `.env`, khong sua `docker-compose.yml`.

## Migration

Flyway chay tu dong khi ung dung khoi dong, doc tu
`src/main/resources/db/migration`. Hien co `V1__create_auth_tables.sql` (module Auth) va
`V2__create_user_tables.sql` (module User).

Muon chay bang tay bang Flyway CLI trong container:

```bash
docker compose --profile migrate run --rm flyway info
docker compose --profile migrate run --rm flyway migrate
```

Container `flyway` nam trong profile rieng nen khong tu chay cung `docker compose up`.
De ca no lan ung dung cung tu migrate se dam nhau tren bang `flyway_schema_history`.

Quy uoc dat ten: `V<so>__<mo_ta_ngan>.sql`. Cam sua migration da chay, sai thi viet
migration moi de len.

## Cau truc package

```
vn.taskconnect
├── TaskConnectApplication.java
├── common/
│   ├── exception/      BusinessException, ErrorCode, GlobalExceptionHandler
│   └── response/       ApiResponse
├── security/           SecurityConfig
├── auth/
│   ├── api/            interface va DTO cong khai, module khac chi import tu day
│   ├── controller/
│   ├── service/
│   ├── entity/
│   ├── repository/
│   ├── dto/{request,response}/
│   └── infrastructure/
└── user/               cung bo thu muc nhu auth
```

`entity/` va `repository/` nam ngang hang `controller/` va `service/`, khong nam trong
`domain/`.

Package `api/` la mat cong khai duy nhat cua module. Module khac chi duoc import tu do,
khong import entity, khong inject repository cua module khac.

Chin module con lai (task, matching, booking, payment, review, chat, notification, admin, ai)
se duoc tao khi bat tay vao lam, theo dung bo thu muc tren.

## Hop dong phan hoi

Moi phan hoi boc trong `ApiResponse`. Truong `null` bi loai khoi JSON.

```jsonc
// 200 OK
{ "success": true, "data": { }, "timestamp": "2026-08-21T14:03:11Z" }

// 409 Conflict
{
  "success": false,
  "message": "Công việc đã được giao cho Tasker khác.",
  "errorCode": "TSK-409-ALREADY_ASSIGNED",
  "timestamp": "2026-08-21T14:03:11Z"
}
```

`errorCode` theo dinh dang `PREFIX-HTTPCODE-REASON`. Phan so trong ma luon trung ma HTTP
that cua phan hoi, vi ma HTTP duoc lay tu chinh `ErrorCode`.

Service nem `BusinessException`, khong tu dung `ResponseEntity` loi.
`GlobalExceptionHandler` la noi duy nhat bien exception thanh phan hoi.

## Chua co, se bo sung sau

JWT va filter xac thuc, rate limit, cau hinh Redis va RabbitMQ chi tiet, gui email,
cau hinh OpenAPI.
