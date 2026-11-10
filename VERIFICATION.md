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

## Đối chiếu NPC, nhiệm vụ và thao tác với hai tệp RAR

- Sau khi áp dụng migration, đối chiếu 164 bản ghi `map_template` trong PostgreSQL với SQL trong `SRC_NRO.rar`. Tên map, cửa chuyển map, tọa độ NPC và danh sách quái khớp sau khi bỏ qua khoảng trắng trong JSON và số khu đã chỉnh cho chế độ chơi chung. Đảo Kamê khôi phục đủ 4 NPC đúng tọa độ. Lý Tiểu Nương hiển thị mô tả nhiệm vụ số 11 và chỉ có nút Đóng trong bản web.
- Tên, mô tả và dữ liệu của 30 nhiệm vụ chính và 125 bước khớp sau khi chuẩn hóa CRLF/LF. Bản web vẽ mũi tên gốc `myTexture2darrow3` khi NPC của bước hiện tại ở ngoài màn hình. Ảnh đại diện trong menu lấy từ gói NPC gốc hoặc từ bản ghi mẫu của NPC.
- Bản web đọc và lưu 10 ô phím tắt trong `skills_shortcut`. Đã gán phím `0` trên khung điện thoại, thoát và đăng nhập trên desktop để kiểm tra vị trí ô. Đòn PvP bị máy chủ từ chối không phát hiệu ứng trên máy người khác. Đòn hợp lệ lên mộc nhân vẫn giảm HP và tăng bộ đếm nhiệm vụ.
- Đã kiểm tra hai lựa chọn khi ngã gục. Về nhà hồi 1 HP cho nhân vật thử chưa có tàu đặc biệt và giữ nguyên vàng. Hồi sinh tại chỗ ở Làng Aru trừ 500 vàng theo dịch vụ gốc.
- Tám mục thiếu lưới địa hình có ID 78, 95, 101, 116, 121, 125, 130 và 134. Hai tệp RAR cung cấp máy chủ Java và client Windows IL2CPP. Giao diện thách đấu, danh sách bạn bè và chat riêng chưa được nối vào web. Vật lý nhảy, bay, hiệu ứng chiến đấu nâng cao, phó bản và sự kiện chưa được xác nhận khớp từng trường hợp.

## Lối ra map đầu và nhiệm vụ tân thủ

- Lối đi ở mép bản đồ tự kích hoạt khi đi tiếp về phía cửa và nhân vật nằm trong vùng chuyển map gốc. Cửa nằm trong map tiếp tục dùng nút tương tác, phím E hoặc bấm vào biểu tượng cửa khi đứng trong vùng đó.
- Mũi tên chỉ lối sang map nhiệm vụ nếu map đích nối trực tiếp qua một cửa. Khi tới nhà, máy chủ gửi ngay bước Nói chuyện với NPC. Trường hợp nhân vật đã lưu trong nhà nhưng còn bước di chuyển dùng quy tắc khôi phục tân thủ của máy chủ gốc.
- Các đồi vẫn kiểm tra mốc nhiệm vụ gốc. Thử vách cao 48 pixel ở Đồi hoa cúc với cờ va chạm trái/phải trong `tile_set_info`. Đi ngang bị chặn tại vách và nhảy qua được.

## Auto nhiệm vụ và Auto đánh

- Năm tài khoản Trái Đất cùng chạy Auto NV trên Docker giới hạn 0,1 CPU và 512 MiB. Cả năm xuất hiện ở khu 0 của Làng Aru và từng tài khoản hoàn thành phần tân thủ, đánh đủ năm mộc nhân và báo lại ông Gôhan.
- Đã thử chuỗi mở đầu của cả ba hành tinh, gồm đi về nhà, gặp NPC, lấy rađa và thu hoạch đậu. Một nhân vật Trái Đất thử nghiệm tự tìm đủ 10 đùi gà, báo nhiệm vụ và dừng ở bước chọn chỉ số.
- Khi túi đầy và ô rađa đang có trang bị, tiến độ giữ ở bước lấy rađa. Giải phóng một ô cho phép Auto tiếp tục. Bước báo cáo của nhiệm vụ 11 hoàn thành khi Auto gặp NPC ở nhà theo điều kiện của engine gốc.
- Đã thử hai nút trên Chrome desktop và mô phỏng điện thoại. Auto đánh tiếp tục trong 128 giây khi tab bị đóng băng bằng Chrome DevTools. Khi mở lại tab, sức mạnh đã tăng và điều khiển di chuyển bằng tay tắt Auto.
- Các bài thử trên sử dụng PostgreSQL riêng. Nhiệm vụ boss, phó bản và sự kiện nâng cao chưa có một lượt chạy Auto xuyên suốt.

### Kiểm tra lại sau phát hành

- Năm phiên Chrome độc lập, gồm ba cấu hình desktop và hai cấu hình điện thoại, đều hoàn thành nhiệm vụ mộc nhân bằng Auto. Một phiên nhìn thấy cả năm nhân vật di chuyển trong cùng khu. Tải lại trang khôi phục đúng nhân vật với Auto đã tắt. Thoát game giải phóng đủ năm chỗ.
- Đã tái hiện Auto bị kẹt ở lối Làng Mori sang Vách núi Moori. Sau khi sửa, nhân vật đi qua lối này và lối Thị trấn Moori sang Thung lũng Maima bằng thao tác di chuyển của máy chủ. Đã thử phần nhặt vật thể Sao băng và báo lại NPC trên cả ba hành tinh. Chỉ dẫn nhiệm vụ Sao băng trỏ đến vách núi có vật thể. Chỉ dẫn báo cáo nhiệm vụ 11 trỏ đến NPC ở nhà.
- Hành trang đầy làm Auto dừng sau một lần nhặt thất bại và giữ nguyên tiến độ. Dọn một ô rồi bật lại cho phép nhặt vật thể Sao băng.
- Bước báo cáo cuối với Ôsin chuyển sang trạng thái hoàn thành khi nói chuyện bằng tay hoặc bằng Auto. Việc vào sự kiện Ma Bư vẫn cần chọn trong menu NPC theo thời gian mở cửa của game.

## HUD, chat, tuyệt kỹ và di chuyển

- Thử trên Chrome desktop và mô phỏng điện thoại 320 px ở cả màn hình dọc và ngang. Dòng sức mạnh và tên map nằm dưới khung HP/KI. Năm ô kỹ năng và ô tuyệt kỹ nằm cùng hàng trên điện thoại thử. Phím số chọn chiêu đã học.
- Kênh Thế giới gửi tin đến người ở map khác, còn kênh Khu vực giữ phạm vi cùng khu. Kiểm tra điều kiện sức mạnh, trừ đúng 1 ngọc và từ chối gửi liên tiếp trong thời gian chờ. Tin nhắn được hiển thị dưới dạng văn bản.
- Đối chiếu cả 30 cấp của ba tuyệt kỹ với `src/nro.sql` trong `SRC_NRO.rar`. Đã học Super Kamejoko qua Whis bằng nhân vật thử và kích hoạt cả ba tuyệt kỹ để kiểm tra gồng, phát chiêu, trừ KI và hồi chiêu.
- Đo tốc độ chạy cơ bản từ 120 lên 240 pixel bản đồ/giây. Thử nhảy lên 152 pixel và qua vách Đồi hoa cúc. Cả năm nhân vật chạy Auto cùng hoàn thành phần tân thủ và đánh mộc nhân sau khi tăng tốc. Hai lối ở Namek đã được thử lại.
- Auto đã dùng NPC gốc để đi từ Trạm Trái Đất sang Trạm Namek, từ Thành phố Vegeta đến Nappa và từ nhà Bunma tương lai về Trái Đất. Các bài thử này chạy trên PostgreSQL riêng.

## Auto nhiệm vụ một nút và Auto treo

- Trên PostgreSQL 16 riêng, một lần bật Auto NV đủ để dùng tiềm năng, sang map có Sao băng, nhặt vật thể và quay về báo NPC. Khi túi đầy, nhân vật tự về rương nhà cất một món dư rồi quay lại làm tiếp.
- Hai tài khoản cùng bật Auto ở bước bang hội lập chung một bang qua dịch vụ gốc. Chỉ trừ 10.000 vàng một lần và cả hai qua bước. Auto mua máy dò đúng giá 100 triệu vàng ở Bunma tương lai, sử dụng máy dò rồi sang map quái. Nhân vật ở nhà sau khi rời sự kiện Ma Bư có thể tự về Đảo Kamê báo Ôsin.
- Auto tìm Calick, chờ chuyến tàu 60 giây, đến nhà Bunma tương lai rồi báo nhiệm vụ. Năm phiên Chrome desktop và mobile cùng hoàn thành phần tân thủ và đánh mộc nhân. Một phiên nhìn thấy cả năm nhân vật di chuyển ở khu 0 của Làng Aru. Tải lại trang giữ nguyên Auto và thoát giải phóng cả năm chỗ.
- Auto treo giữ chế độ khi hết KI, tự đánh lại sau hồi sức và tiếp tục sau khi nối lại phiên ngắn. Điều khiển nhân vật tắt Auto. Hai bài kiểm thử Java bổ sung xác nhận Auto đổi mục tiêu khi bị kẹt và ưu tiên quái chưa được giữ.
- Kiểm tra tuyến và điều kiện lên tầng Ma Bư riêng. Chưa chạy trọn chuỗi boss và sự kiện theo giờ từ tài khoản mới.
