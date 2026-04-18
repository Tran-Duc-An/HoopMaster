# HoopMaster_BE

## Giới thiệu dự án

HoopMaster_BE là backend cho hệ thống hỗ trợ luyện tập bóng rổ thông minh, cung cấp API và các dịch vụ xử lý dữ liệu, AI, và quản lý người dùng. Dự án hướng tới việc giúp người chơi cải thiện kỹ năng thông qua phản hồi tự động, chấm điểm động tác, và cá nhân hóa bài tập.

### Các tính năng chính hiện tại

- **Quản lý người dùng:** Đăng ký, đăng nhập, lưu thông tin cá nhân và lịch sử luyện tập.
- **Quản lý bài tập:** Lấy danh sách bài tập, chi tiết bài tập, và gợi ý bài tập phù hợp.
- **Chấm điểm động tác (Pose Feedback):** Nhận dữ liệu pose từ client, đánh giá động tác, trả về phản hồi chi tiết.
- **Giao tiếp AI (LLM, TTS):** Tích hợp AI để sinh phản hồi, hướng dẫn luyện tập, chuyển văn bản thành giọng nói.
- **Quản lý phiên luyện tập:** Tạo, lưu, và truy xuất lịch sử các phiên luyện tập.
- **Socket Realtime:** Hỗ trợ giao tiếp realtime giữa client và server (ví dụ: phản hồi động tác trực tiếp).
- **Test client:** Có sẵn script Python để kiểm thử API và các tính năng backend.

## Backend Setup & Usage Guide

### 1. Cài đặt môi trường

- Yêu cầu: Node.js, npm, Python 3
- Cài đặt package Node.js:
  ```bash
  npm install
  ```

### 2. Chạy Backend Server

- Chạy server Node.js:
  ```bash
  node src/server.js
  ```
  hoặc dùng nodemon (nếu đã cài):
  ```bash
  npx nodemon src/server.js
  ```

### 3. Chạy Test Client (Python)

- Đảm bảo đã cài Python 3 và các thư viện cần thiết (requests, websockets, v.v.)
- Chạy test client:
  ```bash
  python test_client.py
  ```

### 4. Ghi chú
- File `.env` chứa thông tin nhạy cảm, KHÔNG đẩy lên git.
- Thư mục `node_modules` sẽ được tự động tạo lại khi chạy `npm install`.
- Đảm bảo đã cấu hình đúng các biến môi trường trong `.env`.

---

## English Quickstart

1. Install dependencies:
   ```bash
   npm install
   ```
2. Run backend:
   ```bash
   node src/server.js
   # or
   npx nodemon src/server.js
   ```
3. Run test client:
   ```bash
   python test_client.py
   ```
