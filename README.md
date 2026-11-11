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

Thanh kỹ năng hiện năm ô mặc định theo hành tinh và một ô tuyệt kỹ phím **T**. Nhân vật mới học sẵn đòn cơ bản theo dữ liệu tạo nhân vật gốc. Các ô khóa mở khi học chiêu. Tuyệt kỹ Super Kamejoko, Ma phong ba và Cađíc liên hoàn chưởng dùng biểu tượng và thông số từ `src/nro.sql` trong `SRC_NRO.rar`, cùng điều kiện học tại Whis.

Nút chat màu cam hoặc phím **Enter** mở khung trò chuyện. **Khu vực** gửi trong cùng khu, **Thế giới** gửi tới mọi người đang chơi, kể cả khác map. Kênh Thế giới giữ điều kiện gốc cho người chơi thường. Cần trên 1,5 triệu sức mạnh và ít nhất 5 ngọc trong game. Mỗi tin dùng 1 ngọc và cách tin trước ít nhất 30 giây.

Trình duyệt vẽ 33 bộ ô địa hình theo lưới map từ `SRC_NRO.rar` và chọn lớp ảnh nền theo cặp `bgId` và `bgType`. Ảnh nền và bộ ô chỉ tải khi vào map cần dùng. Thứ tự các lớp nền và 33 dáng ghép sprite lấy từ client trong `Mod_Local.rar`. Hoạt ảnh cơ bản dùng bảng gốc. Một số hiệu ứng chiến đấu, thời tiết và hoạt ảnh đặc biệt vẫn được tái dựng gần đúng.

## Gameplay và các phần được lược bỏ

Giữ mã xử lý nhân vật, ba hành tinh, bản đồ, quái, boss, nhiệm vụ, kỹ năng, trang bị, đệ tử, bang hội, nâng cấp, phó bản và giải đấu. Web có màn hình hành trang, chỉ số/kỹ năng, nhiệm vụ, NPC/cửa hàng, nâng cấp, bang hội và giao dịch giữa người chơi. Các thao tác dùng lại kiểm tra và xử lý của engine.

Hai nút **Auto NV** và **Auto treo** ở góc phải dùng được sau khi đăng nhập. **Auto NV** tự tìm đường qua cửa và dùng tuyến tàu gốc để đến map nhiệm vụ chính. Nhân vật tự gặp NPC, đánh quái, nhặt vật phẩm, tăng chỉ số theo yêu cầu và cất một món đồ dư vào rương khi cần chỗ trống. Auto có thể dùng 100 triệu vàng trong game để mua máy dò bắt buộc nếu túi và rương đều thiếu, hoặc dùng 10.000 vàng để lập bang khi có hai tài khoản cùng bật Auto ở bước bang hội. Nút chuyển sang màu vàng và hiện lý do khi phải đợi người chơi khác, boss hoặc giờ mở sự kiện.

**Auto treo** tự tìm bãi luyện vừa sức trong các map đã mở, xét sức đánh, HP, giáp, cấp quái và boss nguy hiểm trong khu. Auto ưu tiên quái chưa bị người khác nhắm, hạ xong mục tiêu đang đánh rồi mới chọn tiếp. Khi thiếu HP, KI hoặc thể lực, Auto dùng đậu có sẵn hoặc lùi lại hồi sức rồi đánh tiếp. Nếu gục, nhân vật hồi sinh miễn phí và tìm bãi khác. Mỗi nhân vật bật riêng một chế độ. Bấm lại nút hoặc điều khiển nhân vật để tắt. Máy chủ xử lý Auto khi tab ở nền và kết nối còn mở. Nếu nối lại đúng phiên trong 45 giây, chế độ đang chạy được khôi phục.

Cả hai chế độ tự đi nhặt ngọc rồng thường, trang bị, đậu, nguyên liệu, sách và các đồ rơi được phép nhặt trong map hiện tại sau khi hạ xong quái đang đánh. Quyền sở hữu, đồ riêng theo nhiệm vụ và thời gian nhặt vẫn theo máy chủ gốc. Ngọc Namek và ngọc đen dạng mang trên người để tham gia sự kiện PvP được nhặt thủ công, vì chúng khóa đổi map và thay đổi luật hồi sinh. Nếu đã mang ngọc Namek, Auto giữ đường đang đi và chờ đúng thời hạn chuyển map, không quay lại luyện quái vì nhầm đường bị chặn. Túi đầy vẫn nhặt tiền và đồ có thể gộp hết vào túi; món cần thêm ô trống được bỏ qua và thử lại khi có chỗ. Đồ không thể tới hoặc nhặt thất bại được tạm bỏ qua để Auto tiếp tục hoạt động. Đùi gà tại nhà, Sao băng và vệ tinh đặt trên map được xử lý theo chức năng riêng.

Khi đổi bước nhiệm vụ hoặc đổi map, Auto NV tính tiếp đường ngay, dùng đúng tàu tại trạm của từng hành tinh. Khoảng chờ tìm đường không chuyển nhân vật sang luyện quái. Mục tiêu đã chết được bỏ giữ ngay; nếu cùng đối tượng quái hồi sinh khi nhân vật đang gục hoặc bị choáng, Auto vẫn nhận biết trận trước đã xong và tiếp tục bước mới.

Auto NV đọc đúng bước nhiệm vụ chính. Khi đạt mốc Sức mạnh, nhân vật hạ xong quái đang đánh rồi chuyển sang mục tiêu của bước tiếp theo. Với quái nhiệm vụ thường vừa tầm, Auto có thể hồi sinh tại chỗ và tiếp tục cùng mục tiêu nếu mỗi lượt vẫn giảm được HP quái. Nếu không tiến triển sau các lần gục hoặc mục tiêu quá khó, Auto luyện thêm và ưu tiên cộng Sức đánh. Khi cần thêm HP, Auto dành tối đa 20% tiềm năng kiếm được và tích lũy phần này qua các lượt nhỏ để mua được chỉ số cần thiết. Mức HP chuẩn bị được tính cho toàn bộ trận đấu. Nhân vật quá yếu có thể quay về tập mộc nhân. Khi đã đánh trúng một con, Auto tiếp tục con đó đến khi hạ xong, hoặc chọn lại nếu mục tiêu biến mất hay bị kẹt.

Sau các lần gục không tiến triển, Auto NV luyện tạm 30 giây rồi đọc lại nhiệm vụ và thử lại theo chỉ số hiện có. Auto không đòi thêm 5% Sức mạnh hay 25% chỉ số để thoát trạng thái này. Boss đi ngang map không loại bỏ toàn bộ map khỏi tuyến nhiệm vụ khi nhân vật vẫn có thể đánh tiến triển quái cần diệt và hồi sinh tại chỗ. Auto treo tiếp tục tránh bãi có boss quá mạnh.

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
| Chạy / bay lên xuống | WASD hoặc phím mũi tên | Bàn điều hướng cảm ứng |
| Nhảy | Space | Nút ↥ |
| Chọn quái / người chơi | Bấm vào nhân vật trên map | Chạm vào nhân vật |
| Đánh | J; phím 1–9 và 0 chọn kỹ năng đã gán trong tab Kỹ năng | ĐÁNH hoặc chạm ô kỹ năng |
| Tuyệt kỹ đã học | T | Ô tuyệt kỹ viền vàng |
| NPC / lối đi | Đến gần, nhấn E hoặc bấm nút tương tác | Nút E / nút hiện trên map |
| Lối ở mép map | Đi tiếp tới sát mép để tự chuyển map | Giữ nút hướng tới sát mép hoặc chạm mũi tên khi đã đứng trong cửa |
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
javac -cp target/nro-web.jar -d .local/checks scripts/SmokeTest.java scripts/LoadTest.java scripts/AutomationTest.java
java -cp 'target/nro-web.jar;.local/checks' LoadTest 35
java -cp 'target/nro-web.jar;.local/checks' AutomationTest
```

Các script tích hợp tạo tài khoản QA riêng và kiểm tra DB thật. Chỉ chạy khi có đủ 5 chỗ trống. Không dùng tài khoản mẫu/admin trong SQL cũ.

Xem `VERIFICATION.md` để biết chính xác những gì đã kiểm tra và giới hạn kiểm chứng. Chưa có một vòng chơi thủ công toàn bộ các nhiệm vụ, phó bản và sự kiện cũ; không coi kiểm thử tải ngắn là bảo đảm mọi nhánh của engine gốc đều không lỗi.
