package com.trantrongmanh.lab5;

import android.app.Application;

/**
 * Custom Application class.
 * Firebase tự khởi tạo qua google-services.json, không cần init thủ công.
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Firebase SDK tự khởi tạo khi có google-services.json
    }
}
