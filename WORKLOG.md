# NRO Web / Render

Yêu cầu: mở link Render bằng trình duyệt PC/điện thoại, PostgreSQL Aiven, tối đa 5 người cùng thế giới, bỏ thanh toán/nạp thẻ/Momo/quảng cáo.

Kiến trúc: giữ engine Java và dữ liệu game; thêm transport WebSocket JSON và client Canvas cảm ứng. Một instance và một vòng cập nhật quản lý trạng thái; không cần Unity hay cổng TCP công khai. PostgreSQL dùng schema riêng, không nhập tài khoản/admin/mật khẩu cũ từ bản dump. Chứng chỉ CA được dùng để xác minh TLS. Bí mật chỉ qua biến môi trường.

Đã hoàn tất build Maven, chuyển JDBC/dữ liệu, client web, Docker/Render, 5 kiểm thử đơn vị, kiểm thử tích hợp 5 phiên và ngắt/kết nối lại, chiến đấu/giao dịch/lưu PostgreSQL thật, bài tải 35 giây và kiểm tra desktop/mobile. Gói release/NRO-Web-Render.zip (19 MiB), README.md, VERIFICATION.md và ảnh kiểm chứng đã tạo; ZIP đã kiểm tra toàn vẹn và loại mật khẩu DB. Chưa xuất bản Render theo lựa chọn của người dùng. Xem VERIFICATION.md về phạm vi kiểm chứng; chưa chơi toàn bộ nội dung engine cũ.
