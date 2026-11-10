# Kiểm chứng bản giao diện cổ điển

Kiểm tra ngày 09/11/2026 trên Docker giới hạn 512 MiB RAM và 0,5 CPU, với schema thử nghiệm riêng trên Aiven PostgreSQL qua TLS.

- Build Java 21 và Docker thành công. 5 bài kiểm thử Maven đạt.
- Tạo 5 tài khoản Trái Đất, đi từ Vách núi Aru qua Nhà Gôhan đến Làng Aru, cùng khu và nhìn thấy nhau. Phiên thứ sáu bị từ chối.
- Mở lời thoại Ông Gôhan từ nhiệm vụ gốc, thu hoạch 5 hạt đậu, mở hành trang, đánh quái, chat và giao dịch vàng với xác nhận từ cả hai bên.
- Ngắt kết nối rồi nối lại cùng nhân vật. Thoát và đăng nhập lại khôi phục vị trí đã ghi vào PostgreSQL.
- Chromium desktop 1240 × 700 và mô phỏng điện thoại 390 × 740. Kiểm tra đăng ký, NPC, chuyển map, ô trang bị, các tab, đăng nhập lại và di chuyển cảm ứng. Ảnh chụp trong thư mục `verification`.

Bản cập nhật ngày 10/11/2026 đối chiếu từng pixel của 909 ô ảnh gốc trong 33 atlas và kiểm tra bảng 33 dáng ghép đầu, thân, chân. Bảng lớp nền bao phủ 30 tổ hợp `bgId` và `bgType` của 160 map trong dữ liệu, kèm vị trí riêng cho map 103. Kiểm tra lại màn hình desktop và điện thoại, chuyển qua ba map đầu Trái Đất, mở NPC, hành trang và điều khiển cảm ứng.

Các bài chơi trên trình duyệt chạy tại máy phát triển với schema thử nghiệm. Các hiệu ứng đặc biệt, phó bản và nhiệm vụ nâng cao còn cần đối chiếu từng trường hợp với client Windows. Kiểm thử 5 tài khoản dùng các kết nối riêng trên máy phát triển.
