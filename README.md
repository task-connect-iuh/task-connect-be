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
`src/main/resources/db/migration`. Hien co `V1` (bang goc Auth), `V2` (bang goc User),
`V3` (doi xac minh email sang OTP), `V4`/`V5` (seed danh muc dich vu/loai chung chi),
`V6`-`V8` (bang quen mat khau + doi/hoan quy uoc luu UTC+7), `V9` (them `bio` vao ho so),
`V10` (seed tai khoan super-admin duy nhat - xem `docs/PROGRESS-ADMIN-MODULE.md`, dung
placeholder `${adminEmail}`/`${adminPasswordHash}` nap tu `ADMIN_SEED_EMAIL`/
`ADMIN_SEED_PASSWORD_HASH` trong `.env` - thieu bien nay app khong khoi dong duoc).

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
│   ├── config/          TimeConfig (bean Clock, dung chung)
│   ├── exception/        BusinessException, ErrorCode, GlobalExceptionHandler
│   └── response/         ApiResponse
├── security/             SecurityConfig, JWT filter, rate limit
├── auth/
│   ├── api/               interface va DTO cong khai, module khac chi import tu day
│   │   └── event/         su kien Spring noi bo (vd EmailVerificationRequestedEvent)
│   ├── controller/
│   ├── service/
│   ├── entity/
│   ├── repository/
│   └── dto/{request,response}/
├── user/                  cung bo thu muc nhu auth
└── notification/          gui email (Brevo SMTP relay qua JavaMailSender)
    ├── api/                NotificationFacade + DTO lien module
    ├── service/            NotificationFacadeImpl
    └── infrastructure/     EmailSender, template, cau hinh mail, listener
```

`entity/` va `repository/` nam ngang hang `controller/` va `service/`, khong nam trong
`domain/`.

Package `api/` la mat cong khai duy nhat cua module. Module khac chi duoc import tu do,
khong import entity, khong inject repository cua module khac. Auth publish su kien Spring
noi bo (khong qua RabbitMQ) khi can gui email, module Notification lang nghe va gui that.

Tam module con lai (task, matching, booking, payment, review, chat, admin, ai)
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

## Xac minh email bang OTP

Dang ky xong, tai khoan o trang thai `UNVERIFIED` va nhan mot email chua ma OTP 6 chu
so, hieu luc 5 phut. `POST /api/v1/auth/verify-email` nhan `{email, otp}`. Nhap sai qua
5 lan thi ma bi vo hieu hoa, phai xin ma moi qua `POST /api/v1/auth/resend-verification`
(chi duoc goi lai sau 60 giay ke tu lan gan nhat, goi lai se vo hieu hoa ma cu).

Gui that qua Brevo SMTP relay (`app.mail.enabled=true`, xem `.env.example`). O may dev
chua co API key, de `MAIL_ENABLED=false` (mac dinh): ung dung van chay va sinh ma binh
thuong, chi khong co email nao ra ngoai.

## Chua co, se bo sung sau

Cau hinh Redis va RabbitMQ chi tiet cho cac luong nghiep vu khac, cau hinh OpenAPI,
outbox pattern cho luong thong bao (hien Auth publish su kien Spring noi bo, chua qua
RabbitMQ - xem Javadoc EmailVerificationRequestedEvent).
