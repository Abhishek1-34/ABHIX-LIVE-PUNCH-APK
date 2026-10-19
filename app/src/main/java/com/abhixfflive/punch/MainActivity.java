package com.abhixfflive.punch;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends Activity {
    private TextView status;
    private Button extractButton;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 24, 24, 24);
        layout.setBackgroundColor(Color.rgb(20, 20, 20));

        TextView title = new TextView(this);
        title.setText("ABHIXFFLIVE PUNCH");
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        layout.addView(title);

        status = new TextView(this);
        status.setTextColor(Color.WHITE);
        status.setPadding(0, 8, 0, 12);
        status.setText("PUNCH.zip is bundled in this app.");
        layout.addView(status);

        extractButton = new Button(this);
        extractButton.setText("EXTRACT PUNCH ZIP + 7Z");
        extractButton.setOnClickListener(v -> extractButton.setEnabled(false));
        extractButton.setOnClickListener(v -> startExtraction());
        layout.addView(extractButton);

        Button check = new Button(this);
        check.setText("CHECK SHIZUKU");
        check.setOnClickListener(v -> {
            try {
                Class.forName("rikka.shizuku.Shizuku");
                status.setText("Shizuku class detected. Permission/API integration is not configured in this build.");
            } catch (Exception e) {
                status.setText("Shizuku API is not included. Extraction to app storage does not require Shizuku.");
            }
        });
        layout.addView(check);
        setContentView(layout);
    }

    private void startExtraction() {
        extractButton.setEnabled(false);
        status.setText("Extracting bundled archives…");
        new Thread(() -> {
            String message;
            try {
                File base = new File(getExternalFilesDir(null), "PUNCH_Extracted");
                if (!base.exists() && !base.mkdirs()) throw new IOException("Cannot create output folder");
                File temp = new File(getCacheDir(), "PUNCH_outer.zip");
                try (InputStream in = getAssets().open("PUNCH.zip"); FileOutputStream out = new FileOutputStream(temp)) {
                    copy(in, out);
                }
                File sevenZip = null;
                try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(temp.toPath())))) {
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        File outFile = safeChild(base, entry.getName());
                        if (entry.isDirectory()) {
                            if (!outFile.mkdirs() && !outFile.isDirectory()) throw new IOException("Cannot create directory");
                        } else {
                            File parent = outFile.getParentFile();
                            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) throw new IOException("Cannot create directory");
                            try (FileOutputStream out = new FileOutputStream(outFile)) { copy(zis, out); }
                            if (outFile.getName().toLowerCase().endsWith(".7z")) sevenZip = outFile;
                        }
                        zis.closeEntry();
                    }
                }
                if (sevenZip == null) throw new IOException("No .7z archive found inside PUNCH.zip");
                File sevenOut = new File(base, "PUNCH_7Z_CONTENTS");
                if (!sevenOut.exists() && !sevenOut.mkdirs()) throw new IOException("Cannot create 7z output folder");
                extract7z(sevenZip, sevenOut);
                message = "Extraction complete. Files saved in app storage:\n" + base.getAbsolutePath();
            } catch (Exception e) {
                message = "Extraction failed: " + e.getClass().getSimpleName() + ": " + e.getMessage();
            }
            final String result = message;
            runOnUiThread(() -> { status.setText(result); extractButton.setEnabled(true); });
        }).start();
    }

    private static void extract7z(File archive, File output) throws IOException {
        try (SeekableByteChannel channel = Files.newByteChannel(archive.toPath(), StandardOpenOption.READ);
             SevenZFile sevenZ = SevenZFile.builder().setSeekableByteChannel(channel).get()) {
            SevenZArchiveEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = sevenZ.getNextEntry()) != null) {
                File outFile = safeChild(output, entry.getName());
                if (entry.isDirectory()) {
                    if (!outFile.mkdirs() && !outFile.isDirectory()) throw new IOException("Cannot create directory");
                    continue;
                }
                File parent = outFile.getParentFile();
                if (parent != null && !parent.isDirectory() && !parent.mkdirs()) throw new IOException("Cannot create directory");
                try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outFile))) {
                    int n;
                    while ((n = sevenZ.read(buffer)) != -1) out.write(buffer, 0, n);
                }
            }
        }
    }

    private static File safeChild(File root, String name) throws IOException {
        File target = new File(root, name);
        String rootPath = root.getCanonicalPath() + File.separator;
        String targetPath = target.getCanonicalPath();
        if (!targetPath.startsWith(rootPath)) throw new IOException("Unsafe archive path rejected");
        return target;
    }

    private static void copy(InputStream in, java.io.OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
    }
}
