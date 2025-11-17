{
  "id": "admob_android_interstitial",
  "title": "Tích hợp AdMob Interstitial trên Android",
  "description": "Hướng dẫn tích hợp và hiển thị quảng cáo interstitial (toàn màn hình) của Google AdMob trong ứng dụng Android (Java/Kotlin). Dựa trên tài liệu chính thức của Google.",
  "reference": "https://developers.google.com/admob/android/interstitial",
  "prerequisites": {
    "sdk_version_min": "19.7.0",
    "requirements": [
      "Đã tích hợp Google Mobile Ads SDK vào project Android và hoàn thành phần Get started guide.",
      "Gọi tất cả API của Mobile Ads SDK trên main thread.",
      "Đã cấu hình app ID AdMob trong AndroidManifest hoặc ở Get started."
    ],
    "test_ad_unit_id": {
      "note": "Luôn dùng test ad khi phát triển, tránh bị khóa tài khoản.",
      "interstitial_android": "ca-app-pub-3940256099942544/1033173712"
    }
  },
  "config": {
    "ad_unit_id": {
      "description": "Ad unit ID thực tế của interstitial trong ứng dụng, lấy từ AdMob console.",
      "placeholder": "ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY"
    }
  },
  "globals": {
    "member_variables": [
      {
        "name": "private InterstitialAd interstitialAd;",
        "language": "java",
        "usage": "Giữ reference cho quảng cáo interstitial đã load."
      },
      {
        "name": "private var interstitialAd: InterstitialAd? = null",
        "language": "kotlin",
        "usage": "Giữ reference cho quảng cáo interstitial đã load."
      }
    ]
  },
  "steps": [
    {
      "id": "always_use_test_ads",
      "title": "Luôn sử dụng test ad khi phát triển",
      "description": "Trong giai đoạn phát triển và test, luôn dùng test ad unit ID do Google cung cấp, sau đó thay bằng ad unit ID thật khi release.",
      "details": {
        "test_ad_unit_id_interstitial_android": "ca-app-pub-3940256099942544/1033173712"
      }
    },
    {
      "id": "load_interstitial_ad",
      "title": "Load interstitial ad",
      "description": "Sử dụng phương thức tĩnh InterstitialAd.load(...) để load quảng cáo. Kết quả trả về qua InterstitialAdLoadCallback.",
      "notes": [
        "Luôn gọi trên main thread.",
        "Có thể preload nhiều quảng cáo để giảm thời gian chờ khi show.",
        "Nếu đã có quảng cáo hoặc đang load, không gọi load lại trừ khi cần."
      ],
      "code_samples": [
        {
          "language": "java",
          "snippet": "private void loadAd() {\n    if (interstitialAd != null) {\n        return; // đã có ad, không load lại\n    }\n\n    AdRequest adRequest = new AdRequest.Builder().build();\n\n    InterstitialAd.load(\n        this,\n        AD_UNIT_ID, // test: \"ca-app-pub-3940256099942544/1033173712\"\n        adRequest,\n        new InterstitialAdLoadCallback() {\n            @Override\n            public void onAdLoaded(@NonNull InterstitialAd ad) {\n                interstitialAd = ad;\n                Log.d(TAG, \"Interstitial ad loaded\");\n            }\n\n            @Override\n            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {\n                Log.w(TAG, \"Failed to load interstitial: \" + loadAdError.getMessage());\n                interstitialAd = null;\n            }\n        }\n    );\n}"
        },
        {
          "language": "kotlin",
          "snippet": "private fun loadAd() {\n    if (interstitialAd != null) {\n        return // đã có ad, không load lại\n    }\n\n    val adRequest = AdRequest.Builder().build()\n\n    InterstitialAd.load(\n        this,\n        AD_UNIT_ID, // test: \"ca-app-pub-3940256099942544/1033173712\"\n        adRequest,\n        object : InterstitialAdLoadCallback() {\n            override fun onAdLoaded(ad: InterstitialAd) {\n                interstitialAd = ad\n                Log.d(TAG, \"Interstitial ad loaded\")\n            }\n\n            override fun onAdFailedToLoad(error: LoadAdError) {\n                Log.w(TAG, \"Failed to load interstitial: ${error.message}\")\n                interstitialAd = null\n            }\n        }\n    )\n}"
        }
      ]
    },
    {
      "id": "set_full_screen_content_callback",
      "title": "Thiết lập FullScreenContentCallback",
      "description": "Thiết lập callback để nhận các event liên quan đến việc hiển thị interstitial: show, dismiss, click, impression, lỗi hiển thị.",
      "important_points": [
        "Khi quảng cáo bị đóng hoặc hiển thị lỗi, phải set interstitialAd = null để không show lại lần nữa.",
        "Trong onAdDismissedFullScreenContent, thường tiến hành load lại một quảng cáo mới."
      ],
      "code_samples": [
        {
          "language": "java",
          "snippet": "private void attachFullScreenCallback() {\n    if (interstitialAd == null) return;\n\n    interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {\n        @Override\n        public void onAdShowedFullScreenContent() {\n            Log.d(TAG, \"Interstitial ad showed\");\n        }\n\n        @Override\n        public void onAdDismissedFullScreenContent() {\n            Log.d(TAG, \"Interstitial ad dismissed\");\n            interstitialAd = null; // tránh show lại\n            loadAd(); // preload cho lần sau nếu cần\n        }\n\n        @Override\n        public void onAdFailedToShowFullScreenContent(AdError adError) {\n            Log.w(TAG, \"Interstitial failed to show: \" + adError.getMessage());\n            interstitialAd = null;\n        }\n\n        @Override\n        public void onAdImpression() {\n            Log.d(TAG, \"Interstitial impression recorded\");\n        }\n\n        @Override\n        public void onAdClicked() {\n            Log.d(TAG, \"Interstitial clicked\");\n        }\n    });\n}"
        },
        {
          "language": "kotlin",
          "snippet": "private fun attachFullScreenCallback() {\n    interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {\n        override fun onAdShowedFullScreenContent() {\n            Log.d(TAG, \"Interstitial ad showed\")\n        }\n\n        override fun onAdDismissedFullScreenContent() {\n            Log.d(TAG, \"Interstitial ad dismissed\")\n            interstitialAd = null\n            loadAd() // preload cho lần sau nếu cần\n        }\n\n        override fun onAdFailedToShowFullScreenContent(adError: AdError) {\n            Log.w(TAG, \"Interstitial failed to show: ${adError.message}\")\n            interstitialAd = null\n        }\n\n        override fun onAdImpression() {\n            Log.d(TAG, \"Interstitial impression recorded\")\n        }\n\n        override fun onAdClicked() {\n            Log.d(TAG, \"Interstitial clicked\")\n        }\n    }\n}"
        }
      ]
    },
    {
      "id": "show_interstitial_ad",
      "title": "Hiển thị interstitial ad",
      "description": "Hiển thị quảng cáo tại các điểm tạm dừng tự nhiên trong flow của app, ví dụ: kết thúc một level, sau khi hoàn thành nhiệm vụ, sau khi share xong...",
      "notes": [
        "Trước khi show, cần kiểm tra interstitialAd khác null.",
        "Nếu chưa có hoặc đang load, có thể bỏ qua hoặc lên lịch hiển thị cho lần sau."
      ],
      "code_samples": [
        {
          "language": "java",
          "snippet": "private void showInterstitial() {\n    if (interstitialAd != null) {\n        attachFullScreenCallback();\n        interstitialAd.show(this);\n    } else {\n        Log.d(TAG, \"Interstitial ad is not ready yet\");\n        loadAd(); // có thể gọi để preload luôn cho lần sau\n    }\n}"
        },
        {
          "language": "kotlin",
          "snippet": "private fun showInterstitial() {\n    val ad = interstitialAd\n    if (ad != null) {\n        attachFullScreenCallback()\n        ad.show(this)\n    } else {\n        Log.d(TAG, \"Interstitial ad is not ready yet\")\n        loadAd()\n    }\n}"
        }
      ]
    },
    {
      "id": "recommended_flow",
      "title": "Flow khuyến nghị tích hợp trong Activity/Game",
      "description": "Quy trình tích hợp cơ bản trong một Activity hoặc màn chơi.",
      "flow": [
        "Trong onCreate hoặc khi bắt đầu game: gọi loadAd() để preload interstitial.",
        "Ở điểm kết thúc một vòng chơi/level/nhiệm vụ: gọi showInterstitial().",
        "Sau khi quảng cáo bị đóng (onAdDismissedFullScreenContent): set interstitialAd = null và loadAd() lại cho lần tiếp theo."
      ]
    },
    {
      "id": "best_practices",
      "title": "Best practices khi dùng interstitial",
      "description": "Các lưu ý để không làm giảm trải nghiệm người dùng và tuân thủ chính sách.",
      "items": [
        "Chỉ hiển thị interstitial tại các điểm chuyển cảnh tự nhiên (transition point) trong app.",
        "Khi hiển thị quảng cáo, hãy tạm dừng gameplay, animation nặng hoặc audio của app; tiếp tục khi người dùng đóng quảng cáo.",
        "Preload quảng cáo sớm (load trước khi cần show) để khi đến thời điểm hiển thị không phải chờ load.",
        "Không spam/flood người dùng bằng quảng cáo liên tục; cân bằng giữa doanh thu và trải nghiệm.",
        "Luôn test với test ad unit ID cho đến khi chuẩn bị release.",
        "Ads hết hạn sau khoảng 1 giờ, nên reload định kỳ nếu bạn caching lâu."
      ]
    }
  ]
}
