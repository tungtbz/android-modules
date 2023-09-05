# android-modules

## step1
Vào [Releases](https://github.com/tungtbz/android-modules/releases/) để lấy các file cần thiết
Copy các file **ads-release**, **analytic-release**, **base-release**, **remoteconfig-release** vào trong **Assets/Plugins/Android**

Với **Applovin** thì copy maxads-debug hoặc maxads-release tuỳ theo bản build debug hoặc release
Với **IronSource** cũng tương tự

Chỉnh sửa file **mainTemplate** và **baseProjectTemplate** theo từng mạng
### applovin
https://gist.github.com/tungtbz/f46bedcb69b83aa644650f17d327a7ba

https://gist.github.com/tungtbz/fcc32240d13fa10017a4f9cc6e4c2367

### ironSource
https://gist.github.com/tungtbz/97e3d7641062e1775a12bd1017fb0473

https://gist.github.com/tungtbz/19d50bb212a257ee30be351c86e4c9cd

## Step2:
Sau khi build game thì mở AndroidStudio và chỉnh sửa file **string.xml**
- chỉnh sửa key ads_main_network theo mạng quảng cáo đang dùng (applovin,ironSource)
- chỉnh sửa key banner_position và mrec_possition (0 = top, 1 = bottom)

