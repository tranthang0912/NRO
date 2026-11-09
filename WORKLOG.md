# NRO Web / Render

## Yêu cầu sửa hướng triển khai

Người dùng đã từ chối bản client Canvas đầu tiên vì khác Ngọc Rồng gốc. Yêu cầu mới rõ ràng: viết lại client web theo giao diện NRO cổ điển trong ảnh, giữ lối chơi và các luồng trong server gốc, không tự thêm dịch chuyển miễn phí/vật lý riêng/cắt tính năng. Không có source client. Đã kiểm tra Mod_Local.rar (chỉ Windows IL2CPP) và SRC_NRO.rar (Java server). Được phép tái dựng client từ tài nguyên hiện có. Nhánh làm việc codex/classic-client; chưa đẩy thay đổi chưa kiểm chứng lên main.

Việc đang làm: trích xuất UI/tile/font gốc từ Unity assets; dựng giao diện pixel NRO; chuyển các thao tác gameplay qua Controller và packet gốc; khôi phục vị trí/luồng tạo nhân vật; kiểm thử nhiệm vụ/NPC/hành trang/chiến đấu/di chuyển/5 phiên và đối chiếu ảnh. Giữ PostgreSQL/Render Free và sửa lưu dữ liệu cần thiết. Phải ghi rõ phạm vi chưa kiểm chứng, không khẳng định giống tuyệt đối khi thiếu source client.

Yêu cầu: mở link Render bằng trình duyệt PC/điện thoại, PostgreSQL Aiven, tối đa 5 người cùng thế giới, bỏ thanh toán/nạp thẻ/Momo/quảng cáo.

Kiến trúc: giữ engine Java và dữ liệu game; thêm transport WebSocket JSON và client Canvas cảm ứng. Một instance và một vòng cập nhật quản lý trạng thái; không cần Unity hay cổng TCP công khai. PostgreSQL dùng schema riêng, không nhập tài khoản/admin/mật khẩu cũ từ bản dump. Chứng chỉ CA được dùng để xác minh TLS. Bí mật chỉ qua biến môi trường.

Đã hoàn tất build Maven, chuyển JDBC/dữ liệu, client web, Docker/Render, 5 kiểm thử đơn vị, kiểm thử tích hợp 5 phiên và ngắt/kết nối lại, chiến đấu/giao dịch/lưu PostgreSQL thật, bài tải 35 giây và kiểm tra desktop/mobile. Gói release/NRO-Web-Render.zip (19 MiB), README.md, VERIFICATION.md và ảnh kiểm chứng đã tạo; ZIP đã kiểm tra toàn vẹn và loại mật khẩu DB. Chưa xuất bản Render theo lựa chọn của người dùng. Xem VERIFICATION.md về phạm vi kiểm chứng; chưa chơi toàn bộ nội dung engine cũ.

## Tiến độ 09/11/2026

Đã phục hồi spawn 39 + gender, bỏ rally/home miễn phí khỏi API và UI, chuyển portal qua changeMapWaypoint với kiểm tra vùng waypoint, mở NPC qua MenuController. Font tên nhân vật dùng bitmap gốc với 224 glyph tiếng Việt. Maven test đạt 5/5. Chưa kiểm thử gameplay/browser cho các thay đổi này. Giao diện và movement vẫn đang tái dựng, chưa đạt yêu cầu giống bản gốc hoàn toàn. Nhánh classic-client chưa push.

Đối chiếu trực tiếp SRC_NRO.rar ngày 09/11: 499 file Java, 22 file thay đổi. Đã phát hiện nhánh web return trước initNonInteractiveNPC, làm thiếu NPC gốc. Đã đưa khởi tạo NPC lên trước return. Danh sách sai khác lưu .local/original-source-audit.json. Mod_Local.rar xác nhận có GameAssembly.dll và metadata IL2CPP, thiếu source C#/Unity project. Yêu cầu giống hoàn toàn chưa được đáp ứng.

Giao diện đang tái dựng: classic/classic.css đã được gắn vào index, sử dụng logo và nút gốc, bỏ hero tự thiết kế. Đây là bản trung gian chưa kiểm chứng browser và chưa đạt đồ họa gốc. JS syntax và đường dẫn CSS assets đạt. Chưa push/deploy. Thiếu tiles 1-13 trong data/res/x1 và chưa tìm thấy trong Unity resources.


## Bản giao diện cổ điển theo ảnh mẫu

Đã thay bố cục bằng giao diện game toàn màn hình với ảnh gốc cho HUD, logo, nút, font và sprite. Các map thiếu dải ô dùng ảnh gần nhất trong bộ gốc. Bổ sung gói lời thoại hướng dẫn, menu cây đậu, rương đồ và sửa giới hạn tọa độ tại lối đi. Đã kiểm thử 5 phiên ở Làng Aru và đối chiếu trên desktop/mobile. Xem VERIFICATION.md.
