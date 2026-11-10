# Ngọc Rồng Hội Bạn — Web / Render / PostgreSQL

Mở cùng một link HTTPS để chơi bằng trình duyệt trên máy tính, điện thoại hoặc máy tính bảng. Không cài Unity, APK hay ứng dụng client. Bản này dùng engine Java có sẵn và client Canvas/WebSocket viết riêng cho web, sử dụng hình ảnh gốc trong thư mục dữ liệu.

## Cấu hình cho nhóm 4–5 người

- Một tiến trình game, **một instance Render**, tối đa 5 tài khoản cùng chơi. Người chơi trong cùng map/khu nhìn thấy nhau và dùng chung quái, vật phẩm rơi, chat.
- Map thường có một khu chung. Nhân vật mới bắt đầu ở vách núi theo hành tinh. Ví dụ với Trái Đất, đi sang phải từ Vách núi Aru vào Nhà Gôhan, rồi qua lối Làng Aru để gặp bạn bè ở khu 0. Khi đã đứng trong lối đi, bấm tên lối đi hoặc nhấn E.
- Mô phỏng 20 nhịp/giây; gửi trạng thái 10 lần/giây, nội suy hình ảnh bằng `requestAnimationFrame`. Không tạo luồng TCP cho từng trình duyệt.
- Gửi một bản trạng thái dùng chung cho mỗi khu. Giới hạn hàng đợi để máy mạng chậm không làm bộ nhớ tăng vô hạn.
- PostgreSQL pool mặc định 3 kết nối, không vượt 6 khi cấu hình. Phù hợp giới hạn 20 kết nối trong thông tin Aiven đã cung cấp.
- Chụp trạng thái nhân vật mỗi 30 giây và khi thoát. Ghi PostgreSQL ở luồng riêng, gộp các bản lưu liên tiếp; có thử lại khi kết nối DB lỗi. Khi Render gửi SIGTERM, máy chủ chờ ghi xong các bản lưu còn lại.
- Mất kết nối: giữ chỗ tối đa 45 giây và tự nối lại đúng nhân vật, đúng map. Vé kết nối lại được đổi sau mỗi lần dùng. Đóng tab lâu hơn thời gian này thì đăng nhập lại bằng mật khẩu.

Không thể bảo đảm không ngắt kết nối khi mạng thiết bị mất hoàn toàn, dịch vụ bị tắt hoặc hết tài nguyên. Cơ chế nối lại xử lý gián đoạn ngắn; dữ liệu đã lưu nằm trong PostgreSQL.

## Giao diện cổ điển

Khung HP/KI, logo, nút, font pixel và sprite lấy từ tài nguyên gốc. Bảng nhân vật mở bằng tên ở góc trái hoặc phím I, có các tab nhiệm vụ, hành trang, kỹ năng, bang hội và chức năng. Hộp thoại NPC hiển thị lời thoại do server gửi.

Trình duyệt vẽ 33 bộ ô địa hình theo lưới map từ `SRC_NRO.rar` và chọn lớp ảnh nền theo cặp `bgId` và `bgType`. Ảnh nền và bộ ô chỉ tải khi vào map cần dùng. Thứ tự các lớp nền và 33 dáng ghép sprite lấy từ client trong `Mod_Local.rar`. Hoạt ảnh cơ bản dùng bảng gốc. Một số hiệu ứng chiến đấu, thời tiết và hoạt ảnh đặc biệt vẫn được tái dựng gần đúng.

## Gameplay và các phần được lược bỏ

Giữ mã xử lý nhân vật, ba hành tinh, bản đồ, quái, boss, nhiệm vụ, kỹ năng, trang bị, đệ tử, bang hội, nâng cấp, phó bản và giải đấu. Web có màn hình hành trang, chỉ số/kỹ năng, nhiệm vụ, NPC/cửa hàng, nâng cấp, bang hội và giao dịch giữa người chơi. Các thao tác dùng lại kiểm tra và xử lý của engine.

Đã loại khỏi bản chạy: bảng `napthe`, `payments`, `posts`, `settings`; dữ liệu quảng cáo/thông báo cũ; giao diện và đường xử lý nạp thẻ/đổi VND; quảng cáo, liên kết tải client và tích hợp thanh toán bên thứ ba. Không có endpoint Momo, webhook thu tiền hay mã quảng cáo trong web. Giữ một số trường tương thích của engine trong schema, không dùng chúng để nhận tiền. Vàng/ngọc kiếm trong game, mua bán vật phẩm và giao dịch trong game vẫn hoạt động.

Không nhập tài khoản, mật khẩu, admin, nhân vật hoặc lịch sử thanh toán có sẵn trong bản MySQL. Người chơi tự tạo tài khoản mới trên web; mật khẩu lưu bằng PBKDF2 có salt. Schema mặc định là `nro_web`, không ghi vào các bảng `public` có sẵn. `b.sql` gốc được giữ tại workspace để đối chiếu, không nằm trong bộ triển khai.

## Đưa lên Render

1. Giải nén bộ triển khai vào một thư mục mới. Tạo repository GitHub trống, rồi đẩy **nội dung thư mục** lên repository đó:

   ```powershell
   git init
   git add .
   git commit -m "NRO web for Render"
   git branch -M main
   git remote add origin https://github.com/TAI_KHOAN/TEN_REPOSITORY.git
   git push -u origin main
   ```

2. Trong [Render Dashboard](https://dashboard.render.com/), chọn **New → Blueprint**, kết nối repository và dùng `render.yaml`. Hoặc tạo **Web Service**, chọn runtime **Docker**, region **Singapore**, health check `/healthz`.

3. Điền biến môi trường:

   | Biến | Giá trị |
   | --- | --- |
   | `DATABASE_URL` | Chuỗi `postgresql://avnadmin:MAT_KHAU@HOST:28253/defaultdb` từ Aiven. Dùng thông tin bạn đã gửi; mật khẩu thật không nằm trong ZIP. Ký tự đặc biệt trong mật khẩu phải được URL encode. |
   | `DB_SSL_ROOT_CERT` | `/app/config/ca.pem` — chứng chỉ CA được kèm trong gói. |
   | `DB_SCHEMA` | `nro_web` |
   | `DB_POOL_MAX` | `3` |
   | `MAX_PLAYERS` | `5` |
   | `INVITE_CODE` | Mã đăng ký do bạn tự chọn để chia sẻ cho nhóm; có thể để trống. |
   | `TZ` | `Asia/Ho_Chi_Minh` |

   Không chép file `.env` chứa mật khẩu lên GitHub. Trên máy đang sửa, `.env` đã được cấu hình riêng để kiểm thử kết nối Aiven; file này được loại khỏi Git, Docker context và ZIP.

4. Giữ **Instances = 1**, không bật autoscaling. Blueprint đã chọn `plan: free`, dùng PostgreSQL Aiven hiện có và không tạo tài nguyên trả phí. Free có chế độ ngủ khi không hoạt động, thời gian khởi động lại và các hạn mức. Xem [giới hạn Free](https://render.com/docs/free).

5. Deploy. Khi log có `NRO Web ready on port ...`, `/healthz` trả HTTP 200 và Render báo Live, mở link `https://TEN_DICH_VU.onrender.com`. Chia sẻ **đúng cùng một link** cho cả nhóm. Mỗi người tạo một tài khoản khác nhau và đi đến cùng bản đồ, cùng khu.

Render cấp HTTPS và chuyển WebSocket qua cùng cổng HTTP; ứng dụng đọc biến `PORT` của Render và lắng nghe `0.0.0.0`. Xem [WebSockets trên Render](https://render.com/docs/websocket) và [Blueprint](https://render.com/docs/blueprint-spec).

Không cần tạo thêm Render Postgres nếu tiếp tục dùng Aiven hiện có. Không cần persistent disk cho tiến trình nhân vật; dữ liệu nằm trong PostgreSQL. CA được dùng ở chế độ `verify-full`, xác minh cả chứng chỉ và hostname.

## Điều khiển

| Thao tác | Máy tính | Điện thoại / tablet |
| --- | --- | --- |
| Đi / bay lên xuống | WASD hoặc phím mũi tên | Bàn điều hướng cảm ứng |
| Nhảy | Space | Nút ↥ |
| Chọn quái / người chơi | Bấm vào nhân vật trên map | Chạm vào nhân vật |
| Đánh | J; phím 1–9 và 0 chọn kỹ năng đã gán trong tab Kỹ năng | ĐÁNH hoặc chạm ô kỹ năng |
| NPC / lối đi | Đến gần, nhấn E hoặc bấm nút tương tác | Nút E / nút hiện trên map |
| Nhặt đồ | F | Chạm vật phẩm / nút Nhặt |
| Ăn đậu | R | ĐẬU |
| Hành trang | I | Nút Hành trang |
| Chat | Enter | Ô chat |

Dùng nút **Thoát** để kết thúc phiên ngay. Đăng nhập cùng một tài khoản trên thiết bị khác bị từ chối trong khi tài khoản đang chơi, tránh tạo bản sao nhân vật. Chơi chung bằng các tài khoản riêng.

## Chạy và kiểm thử tại máy

Yêu cầu JDK 21 và Maven 3.9+ nếu build từ nguồn. Docker tự tải JDK/Maven trong giai đoạn build.

```powershell
Copy-Item .env.example .env
# Điền DATABASE_URL và đường dẫn CA trong .env
mvn -B package
java -Xms64m -Xmx384m "-Djava.awt.headless=true" -jar target/nro-web.jar
```

Hoặc Docker:

```powershell
docker build -t nro-web .
docker run --name nro-web -p 8080:8080 --env-file .env -e PORT=8080 nro-web
```

Mở `http://127.0.0.1:8080`. Kiểm thử nhiều phiên thật qua WebSocket:

```powershell
$env:NRO_TEST_URL = 'http://127.0.0.1:8080'
# DB_SCHEMA của lệnh test phải khớp với schema máy chủ đang chạy.
java --class-path target/nro-web.jar scripts/SmokeTest.java
javac -cp target/nro-web.jar -d .local/checks scripts/SmokeTest.java scripts/LoadTest.java
java -cp 'target/nro-web.jar;.local/checks' LoadTest 35
```

Các script tích hợp tạo tài khoản QA riêng và kiểm tra DB thật. Chỉ chạy khi có đủ 5 chỗ trống. Không dùng tài khoản mẫu/admin trong SQL cũ.

Xem `VERIFICATION.md` để biết chính xác những gì đã kiểm tra và giới hạn kiểm chứng. Chưa có một vòng chơi thủ công toàn bộ các nhiệm vụ, phó bản và sự kiện cũ; không coi kiểm thử tải ngắn là bảo đảm mọi nhánh của engine gốc đều không lỗi.
