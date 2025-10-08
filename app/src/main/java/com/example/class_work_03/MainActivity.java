package com.example.class_work_03;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    final String DB_NAME = "mydb";

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

        // Creates the tables if they do not exist already
        setupDBAndTables();

        // Loads the three most recent images
        findImages();
    }

    void setupDBAndTables() {

        SQLiteDatabase mydb = null;

        try {

            mydb = this.openOrCreateDatabase(DB_NAME, Context.MODE_PRIVATE, null);

            // allegedly, you can enable support for foreign keys, so let's do that:
            mydb.execSQL("PRAGMA foreign_keys = ON;");

            // both of these lines are required to clear the tables
//            mydb.execSQL("DROP TABLE IF EXISTS images");
//            mydb.execSQL("DROP TABLE IF EXISTS image_tags");

            // Create a table that keeps up with image blobs if it does not exist already
            // SQLite does not have a dedicated DATETIME type, so use text storing in this format:
            // YYYY-MM-DD HH:MM:SS (ISO8601)
            String sql = "CREATE TABLE IF NOT EXISTS images (" +
                    "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "IMAGE BLOB NOT NULL, " +
                    "CREATED_AT TEXT DEFAULT CURRENT_TIMESTAMP)";
            mydb.execSQL(sql);

            sql = "CREATE TABLE IF NOT EXISTS image_tags ( " +
                    "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "IMAGE_ID INTEGER NOT NULL, " +
                    "TAG TEXT NOT NULL)";
            mydb.execSQL(sql);
        } catch (SQLiteException e) {
            Log.e("DB_ERROR", "Database error: " + e.getMessage());
        } finally {
            if (mydb != null && mydb.isOpen()) {
                mydb.close();
            }
        }
    }

    public void onClickClearBtn(View view) {
        MyDrawingArea mda = findViewById(R.id.mydrawingarea_main);
        mda.resetPath();
    }

    /**
     *
     * @return
     */
    public byte[] getBitmapAsBytes(MyDrawingArea mda) {
        Bitmap bmp = mda.getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    /**
     *
     * @param text
     * @return
     */
    private String[] cslToArray(String text) {
        String[] parts = text.split(",");
        ArrayList<String> list = new ArrayList<>();

        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }

        return list.toArray(new String[0]);
    }

    private String getFindTextboxText() {
        // 1. Stop here if the tags are empty
        EditText et = findViewById(R.id.findTextbox);
        String text = et.getText().toString();
        Log.v("getFindTextboxText", "text: " + text);
        return text;
    }

    /**
     * Looks at the tags textbox and gets the text.
     * @return
     */
    private String[] getTagsArray() {
        // 1. Stop here if the tags are empty
        EditText et = findViewById(R.id.tagsTextbox);
        String tagsText = et.getText().toString();
        return cslToArray(tagsText);
    }

    /**
     * Finds the appropriate "search result" imageview by index, resets it,
     * then sets the image if one is provided in the "ba" parameter.
     * @param index
     * @param ba
     */
    private void setSearchResultImageViewByIndex(int index, byte[] ba) {
        String ivIdName = "iv" + index;
        int ivId = getResources().getIdentifier(ivIdName, "id", getPackageName());
        ImageView iv = findViewById(ivId);

        iv.setImageBitmap(null);
        iv.setBackgroundColor(Color.DKGRAY);

        if (ba == null || ba.length == 0) {
            return;
        }

        Bitmap bmp = BitmapFactory.decodeByteArray(ba, 0, ba.length);

        iv.setImageBitmap(bmp);
    }

    private void setSearchResultTextViewByIndex(int index, String tags, String imageDate) {
        String tvIdName = "tv" + index;
        int tvId = getResources().getIdentifier(tvIdName, "id", getPackageName());
        TextView tv = findViewById(tvId);

        // "Unavailable\nMMM DD, YYYY - HH AMPM"
        tv.setText("Unavailable");

       if (imageDate == null || imageDate.isEmpty()) {
           return;
       }

        tv.setText(tags + "\n" + imageDate);
    }


    private void showSearchResults(String sql) {
        
        if (sql == null || sql.isEmpty()) {
            Log.e("ShowSearchResults", "SQL is null or empty.");
            return;
        }

        Log.v("ShowSearchResults", "SQL: " + sql);

        SQLiteDatabase mydb = null;
        
        int numSearchResults = 3;

        try {

            mydb = this.openOrCreateDatabase(DB_NAME, Context.MODE_PRIVATE, null);
            
            // Perform the search for the images and display them.
            Cursor c = mydb.rawQuery(sql, null);

            boolean hasData = false;
            int imageIndex = c.getColumnIndexOrThrow("IMAGE");
            int imageTagsIndex = c.getColumnIndexOrThrow("TAGS");
            int dateColIndex = c.getColumnIndexOrThrow("CREATED_AT");

            for (int i = 0; i < numSearchResults; i++) {

                if (i == 0) {
                    hasData = c.moveToFirst();
                }

                Log.v("ShowSearchResults", "index: " + i + ", hasData: " + hasData);

                byte[] ba = hasData ? c.getBlob(imageIndex) : null;
                String tags = hasData ? c.getString(imageTagsIndex) : "Unavailable";
                String imageDate = hasData ? c.getString(dateColIndex) : "MMM DD, YYYY - HH AMPM";

                setSearchResultImageViewByIndex(i + 1, ba);
                setSearchResultTextViewByIndex(i + 1, tags, imageDate);

                hasData = c.moveToNext();
            }
            
            c.close();
        } catch (SQLiteException e) {
            Log.e("DB_ERROR", "Database error: " + e.getMessage());
        } finally {
            if (mydb != null && mydb.isOpen()) {
                mydb.close();
            }
        }

    }

    /**
     * Performs a search query using the "find" textbox. If no tag is provided, then
     * the most recent.
     */
    private void findImages() {
        String findText = getFindTextboxText();

        Log.v("findImages", "findText: " + findText);

        String sql = "SELECT t1.image, GROUP_CONCAT(DISTINCT t2.TAG) AS TAGS, datetime(t1.CREATED_AT, 'localtime') AS CREATED_AT " +
                " FROM images as t1" +
                " INNER JOIN image_tags AS t2 ON t2.image_id = t1.id ";

                if (!findText.isEmpty()) {
                    sql += " WHERE LOWER(t2.tag) = LOWER('" + getFindTextboxText() + "') ";
                }


                sql += " GROUP BY t1.ID " +
                " ORDER BY t1.CREATED_AT DESC";

        showSearchResults(sql);
    }


    /**
     *
     * @param view
     */
    public void onClickSaveBtn(View view) {

        MyDrawingArea mda = findViewById(R.id.mydrawingarea_main);

        // 1. Get the bytes to be saved to the database
        byte[] ba = this.getBitmapAsBytes(mda);

        // 2. Get the tags that will be saved with this image.
        String[] tags = getTagsArray();

        // 3. The "created_at" column defaults to the current timestamp,
        // so we do not have to insert that.
        // Insert the values into the database. You'll need to insert the
        // image into the database.
        ContentValues cv = new ContentValues();
        cv.put("IMAGE", ba);

        // Insert the image into the database and save the new ID; that will be
        // useful for inserting the tags into the mapping table.
        SQLiteDatabase mydb = null;

        try {

            mydb = this.openOrCreateDatabase(DB_NAME, Context.MODE_PRIVATE, null);

            mydb.beginTransaction();

            long newId = mydb.insert("images", null, cv);

            if (newId == -1) {
                throw new SQLiteException("Failed to save image to the database.");
            }

            // If there are no tags, then return true
            if (tags.length == 0) {
                return;
            }



            for (String tag : tags) {
                if (tag == null) {
                    continue;
                }
                tag = tag.trim();
                if (tag.isEmpty()) {
                    continue;
                }

                cv.clear();
                cv.put("IMAGE_ID", newId);
                cv.put("TAG", tag);
                mydb.insert("image_tags", null, cv);
            }

            mydb.setTransactionSuccessful();

        } catch (SQLiteException e) {
            Log.e("DB_ERROR", "Database error: " + e.getMessage());
        } finally {

            if (mydb != null) {
                mydb.endTransaction();
            }

            if (mydb != null && mydb.isOpen()) {
                mydb.close();
            }
        }

        // clear the canvas on save
        mda.resetPath();

        // clear the tags textbox
        EditText tv = findViewById(R.id.tagsTextbox);
        tv.getText().clear();
    }

    public void onClickFindBtn(View view) {

        findImages();

    }
}