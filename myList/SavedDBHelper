//myList를 만들기 위한 db

package com.example.beepme;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SavedDBHelper extends SQLiteOpenHelper {
    public SavedDBHelper(Context context) {
        super(context, "savedDB", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE savedTBL(gNumber INTEGER, gName CHAR(20) PRIMARY KEY, allergy CHAR(20), nutrient CHAR(20), manufacture CHAR(20), source CHAR(20));");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS savedTBL");
        onCreate(db);
    }

    public void resetData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM savedTBL");
        db.close();
    }
}
