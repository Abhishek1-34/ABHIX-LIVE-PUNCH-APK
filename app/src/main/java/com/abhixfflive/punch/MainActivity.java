package com.abhixfflive.punch;
import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.widget.*;
public class MainActivity extends Activity {
    TextView status;
    public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(1);
        layout.setPadding(24,24,24,24);
        layout.setBackgroundColor(Color.rgb(20,20,20));
        TextView title = new TextView(this);
        title.setText("ABHIXFFLIVE PUNCH");
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        layout.addView(title);
        status = new TextView(this);
        status.setTextColor(Color.WHITE);
        status.setText("Choose PUNCH.zip to begin.");
        layout.addView(status);
        Button select = new Button(this);
        select.setText("SELECT PUNCH.ZIP");
        select.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.setType("application/zip");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(i,1);
        });
        layout.addView(select);
        Button check = new Button(this);
        check.setText("CHECK SHIZUKU");
        check.setOnClickListener(v -> {
            boolean found = false;
            try { Class.forName("rikka.shizuku.Shizuku"); found = true; }
            catch(Exception ignored) {}
            status.setText(found ? "Shizuku API detected." : "Shizuku integration not included in this build. No files changed.");
        });
        layout.addView(check);
        setContentView(layout);
    }
    protected void onActivityResult(int r,int c,Intent d) {
        super.onActivityResult(r,c,d);
        if(r==1 && c==RESULT_OK && d!=null)
            status.setText("Selected archive: "+d.getData()+"\nExtraction and replacement are not implemented yet.");
    }
}
