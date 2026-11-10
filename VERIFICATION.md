# Kiểm chứng bản giao diện cổ điển

Kiểm tra ngày 09/11/2026 trên Docker giới hạn 512 MiB RAM và 0,5 CPU, với schema thử nghiệm riêng trên Aiven PostgreSQL qua TLS.

- Build Java 21 và Docker thành công. 5 bài kiểm thử Maven đạt.
- Tạo 5 tài khoản Trái Đất, đi từ Vách núi Aru qua Nhà Gôhan đến Làng Aru, cùng khu và nhìn thấy nhau. Phiên thứ sáu bị từ chối.
- Mở lời thoại Ông Gôhan từ nhiệm vụ gốc, thu hoạch 5 hạt đậu, mở hành trang, đánh quái, chat và giao dịch vàng với xác nhận từ cả hai bên.
- Ngắt kết nối rồi nối lại cùng nhân vật. Thoát và đăng nhập lại khôi phục vị trí đã ghi vào PostgreSQL.
- Chromium desktop 1240 × 700 và mô phỏng điện thoại 390 × 740. Kiểm tra đăng ký, NPC, chuyển map, ô trang bị, các tab, đăng nhập lại và di chuyển cảm ứng. Ảnh chụp trong thư mục `verification`.

Bản cập nhật ngày 10/11/2026 đối chiếu từng pixel của 909 ô ảnh gốc trong 33 atlas và kiểm tra bảng 33 dáng ghép đầu, thân, chân. Bảng lớp nền bao phủ 30 tổ hợp `bgId` và `bgType` của 160 map trong dữ liệu, kèm vị trí riêng cho map 103. Kiểm tra lại màn hình desktop và điện thoại, chuyển qua ba map đầu Trái Đất, mở NPC, hành trang và điều khiển cảm ứng.

Các bài chơi trên trình duyệt chạy tại máy phát triển với schema thử nghiệm. Các hiệu ứng đặc biệt, phó bản và nhiệm vụ nâng cao còn cần đối chiếu từng trường hợp với client Windows. Kiểm thử 5 tài khoản dùng các kết nối riêng trên máy phát triển.

Đợt đối chiếu tiếp theo so trực tiếp với hai tệp RAR. Cả 313 tệp địa hình, trang trí và phân loại ô trong `data/map` đều khớp bản lưu. Tám mục bản đồ trong SQL thiếu tệp lưới ngay trong bản lưu gốc. Bổ sung nền Phòng thí nghiệm Myuu và khôi phục vị trí Dr. Myuu, Bill, Whis và Ôsin theo SQL gốc. Bản web gom người chơi vào một khu chung để chơi nhóm.

SQL chứa 30 nhiệm vụ chính và 125 bước gốc. Hai mô tả được sửa lại dấu xuống dòng. Đã thử chuỗi mở đầu ở Trái Đất, Namek và Xayda qua nút giao diện, từ gặp NPC ở nhà đến lấy rađa, thu hoạch đậu và chuyển sang nhiệm vụ tập luyện. Đã tái hiện lỗi rađa trên bản web cũ và xác nhận mở lại rương khôi phục tiến độ của nhân vật đó. Bảng nhiệm vụ dùng bố cục tên, các bước rồi đến mô tả như client gốc, với cỡ chữ và thao tác cho trình duyệt. Nhiệm vụ gắn với nạp tiền được bỏ theo yêu cầu.

Đã hoàn thành bước đánh 5 mộc nhân, báo ông Gôhan và nhận nhiệm vụ tìm đùi gà. Vật phẩm xuất hiện theo điều kiện nhiệm vụ của từng nhân vật theo quy tắc máy chủ gốc.

## Kết nối và tải 5 người

Các bài thử ngày 10/11/2026 dùng schema thử nghiệm riêng. Năm phiên Chrome độc lập, gồm ba desktop và hai mô phỏng điện thoại, cùng nhìn thấy đủ năm nhân vật ở khu 0 của Làng Aru. Một phiên di chuyển bằng cảm ứng và phiên khác nhận vị trí mới. Tải lại trang khôi phục đúng nhân vật và năm lần thoát đều giải phóng chỗ.

- Năm tài khoản cùng di chuyển 120 giây trên Docker giới hạn 0,1 CPU và 512 MiB, kết nối Aiven PostgreSQL qua TLS. Trong 6.049 mẫu, khoảng cách giữa hai bản cập nhật là 100 ms ở phân vị 95, tối đa 200 ms. Không phiên nào ngắt kết nối, tổng dữ liệu gửi đi khoảng 76,7 KiB/s.
- Khi PostgreSQL giữ khóa tài khoản trong 2,4 giây, khoảng cách lớn nhất giữa hai bản cập nhật của người còn lại giảm từ 2.405 ms xuống 101 ms. Đã thử khóa khi đăng nhập và khi thoát.
- Đã kiểm tra đăng nhập lại trong khi còn bản lưu chưa ghi, ngắt WebSocket trong lúc đăng nhập, bấm nút đăng nhập liên tiếp và mời giao dịch khi thông tin lần đăng nhập còn chờ ghi.
- Nhân vật lưu tại bản đồ thiếu dữ liệu địa hình được đưa về nhà đúng hành tinh. Các bản đồ thiếu địa hình bị chặn khi chuyển map trên web.
- Bài thử tích hợp xác nhận giới hạn 5 người, nối lại sau khi ngắt mạng, lời thoại và nhiệm vụ mở đầu, chat, giao dịch, đánh mộc nhân và vị trí lưu trên PostgreSQL. Năm bài kiểm thử Maven đều đạt.
