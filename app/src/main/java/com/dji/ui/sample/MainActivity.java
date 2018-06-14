package com.dji.ui.sample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.Crashes;

/** Main activity that displays two choices to user */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button completeUIButton = (Button) findViewById(R.id.complete_ui_widgets);
        if (completeUIButton != null) {
            completeUIButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(v.getContext(), CompleteWidgetActivity.class);
                    startActivity(intent);
                }
            });
        }
        Button customUIWidgetBT = (Button)findViewById(R.id.bt_customized_ui_widgets);
        if (customUIWidgetBT != null) {
            customUIWidgetBT.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(v.getContext(), CustomizedWidgetsActivity.class);
                    startActivity(intent);
                }
            });
        }

        AppCenter.start(getApplication(), "e5d99874-9e0b-465b-a480-7a2d4e03502c",
                Analytics.class, Crashes.class);
    }
}
