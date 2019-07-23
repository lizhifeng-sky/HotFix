package android.com.hotfix;

import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.File;

public class StartActivity extends AppCompatActivity {
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        textView = findViewById(R.id.text);
        init();
    }

    private void init() {
        File externalStorageDirectory = Environment.getExternalStorageDirectory();
        Log.e("lzf_path", externalStorageDirectory.getAbsolutePath());
        // 遍历所有的修复dex , 因为可能是多个dex修复包
        File fileDir = externalStorageDirectory != null ?
                new File(externalStorageDirectory, "007") :
                new File(getFilesDir(), BugFixUtils.DEX_DIR);// data/user/0/包名/files/odex（这个可以任意位置）
        if (!fileDir.exists()) {
            fileDir.mkdirs();
        }
        if (BugFixUtils.isGoingToFix(this)) {
            BugFixUtils.loadFixedDex(this, new OnFixListener() {
                @Override
                public void onFixed() {
                    startActivity(new Intent(StartActivity.this, MainActivity.class));
                    finish();
                }
            });
            textView.setText("正在修复。。。。");
        }
    }
}
