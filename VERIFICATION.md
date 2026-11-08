# Kết quả kiểm chứng — 08/11/2026

## Đã đạt

- Build Maven/JDK 21: toàn bộ mã Java biên dịch; 5 bài kiểm thử tự động đạt, không có test thất bại.
- Build Docker nhiều giai đoạn thành công; container chạy bằng tài khoản không phải root, giới hạn 512 MiB RAM và 0,5 CPU.
- Kết nối Aiven PostgreSQL thật bằng CA đã cung cấp, SSL `verify-full`; migration tạo schema riêng và khởi động lại không nhập trùng dữ liệu.
- 5 tài khoản độc lập, gồm cả ba chủng tộc, cùng một map và khu; nhìn thấy cùng danh sách nhân vật. Phiên thứ 6 bị từ chối, các phiên cũ tiếp tục hoạt động.
- Di chuyển được nhân sang phiên khác, chat cùng map tới được người chơi khác.
- Ngắt WebSocket đột ngột rồi dùng vé nối lại: giữ nguyên ID nhân vật; vé cũ không dùng lại được.
- Giao dịch 10 vàng giữa hai tài khoản: đòi cả hai xác nhận; gửi lặp xác nhận từ một phía không thay cho bên còn lại.
- Tấn công quái thật bằng kỹ năng của engine: HP quái giảm trong trạng thái map chung.
- Đọc hành trang; chuyển về nhà và tập hợp về Làng Aru.
- Thay đổi vị trí, thoát, đọc trực tiếp PostgreSQL và đăng nhập lại: tọa độ đã lưu khớp với tọa độ được phục hồi.
- Kiểm tra trực tiếp trình duyệt ở desktop và khung mobile 390 × 844: đăng ký/đăng nhập, hiển thị map, hành trang và giữ nút cảm ứng để di chuyển. Ảnh mobile nằm trong thư mục `verification`.
- Gói ZIP dùng danh sách file cho phép, loại `.env`, bản SQL MySQL chứa tài khoản cũ, client Unity, archive cũ và dữ liệu hình ảnh x2/x3/x4. Có kiểm tra tính toàn vẹn ZIP và SHA-256.

## Bài thử tải đã đo

Môi trường: Docker tại máy phát triển, giới hạn 512 MiB RAM / 0,5 CPU, dùng PostgreSQL Aiven qua TLS. Đây không phải số đo mạng di động hoặc đo trên dịch vụ Render đã xuất bản.

| Chỉ số | Kết quả |
| --- | --- |
| Phiên chơi đồng thời | 5 |
| Thời gian đo | 35 giây |
| Mẫu khoảng cách giữa các bản cập nhật | 1.775 |
| Khoảng cách cập nhật p95 | 100 ms |
| Khoảng cách lớn nhất | 107 ms |
| Số phiên bị ngắt trong bài đo | 0 |
| RAM sau bài đo | khoảng 150 MiB / 512 MiB |
| Tổng dữ liệu trạng thái gửi tới cả 5 phiên | khoảng 73,3 KiB/giây |

Bài đo bao gồm chuyển động và chu kỳ tự lưu. Trạng thái và log được kiểm tra sau bài chạy; không ghi nhận exception trong container của vòng kiểm thử này.

## Giới hạn kiểm chứng

Chưa xuất bản lên tài khoản Render: người dùng chọn nhận bộ triển khai và hướng dẫn. Chưa thử bằng 5 điện thoại/máy tính vật lý trên các mạng khác nhau; bài nhiều người dùng các kết nối WebSocket riêng và giao diện được thử trong trình duyệt. Chưa chơi thủ công toàn bộ chuỗi nhiệm vụ, phó bản, giải đấu và mọi biến thể vật phẩm của engine cũ. Không thể chứng nhận tuyệt đối mọi nhánh gameplay không lỗi.

Giữ Render ở một instance. Khi chủ động deploy hoặc khởi động lại tiến trình, các WebSocket cũ sẽ đóng và vé nối lại trong RAM hết hiệu lực; đăng nhập lại để tiếp tục dữ liệu đã lưu. Nên cho nhóm thoát game trước đợt cập nhật mã nguồn.
