# Cinema Revenue Analytics Frontend

Dashboard React + TypeScript cho hệ thống Cinema Revenue Analytics.

## Yêu cầu trước khi chạy

- Java 17+
- Maven
- Node.js 18+
- Backend Spring Boot đang chạy trên `http://localhost:8080`

## Chạy backend (profile `memory,stub`)

Từ thư mục gốc của project:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=memory,stub
```

Hoặc chạy trực tiếp từ IDE bằng profile `memory,stub`.

Lưu ý:
- Backend mặc định chạy ở port `8080`.
- Profile `stub` là mock tạm thời cho showtime data; khi người 1 đưa dữ liệu thật, có thể bỏ profile `stub`.
- Dữ liệu mẫu chỉ có 4 tuần từ `2026-09-14` đến `2026-10-11`.

## Chạy frontend

1. Cài đặt dependency:

```bash
npm install
```

2. Tạo file môi trường nếu cần:

```bash
cp .env.example .env
```

3. Khởi động dev server:

```bash
npm run dev -- --host 0.0.0.0 --port 5173
```

4. Frontend sẽ mở tại:

```text
http://localhost:5173
```

## Biến môi trường

File `.env.example`:

```env
VITE_API_BASE_URL=
```

- Nếu biến rỗng, Vite sẽ dùng proxy `/api` tới `http://localhost:8080` như đã cấu hình trong `vite.config.ts`.
- Nếu muốn gọi backend trực tiếp, set `VITE_API_BASE_URL=http://localhost:8080`.

## Dữ liệu mẫu

Backend profile `memory` chỉ có dữ liệu mô phỏng trong 4 tuần sau:

- 2026-09-14 -> 2026-09-20
- 2026-09-21 -> 2026-09-27
- 2026-09-28 -> 2026-10-04
- 2026-10-05 -> 2026-10-11

Ngoài khoảng này, frontend sẽ nhận mảng rỗng hoặc empty state, không báo lỗi.

## Kiểm tra nhanh

```bash
npm test
npm run build
```

## Lưu ý

- Frontend không tự parse lại `reason`/`action` của backend; chỉ hiển thị nguyên văn.
- Tuần mặc định áp dụng là `2026-09-14`.
- Không dùng `new Date("yyyy-MM-dd")` để tránh lệch múi giờ; logic tuần dựa trên ngày địa phương.
