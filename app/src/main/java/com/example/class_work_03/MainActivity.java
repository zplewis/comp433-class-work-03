package com.example.class_work_03;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    void setupDBAndTables() {
        SQLiteDatabase mydb = this.openOrCreateDatabase("mydb", Context.MODE_PRIVATE, null);

        // Create a table that keeps up with image blobs if it does not exist already
        // SQLite does not have a dedicated DATETIME type, so use text storing in this format:
        // YYYY-MM-DD HH:MM:SS (ISO8601)
        String sql = "CREATE TABLE IF NOT EXISTS images (" +
                "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "IMAGE BLOB NOT NULL, " +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP";

        sql = "CREATE TABLE IF NOT EXISTS image_tags ( " +
                "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "IMAGE_ID INTEGER NOT NULL, " +
                "TAG NOT NULL";
    }

    public void onClickClearBtn(View view) {
        MyDrawingArea mda = findViewById(R.id.mydrawingarea_main);
        mda.resetPath();
    }
}