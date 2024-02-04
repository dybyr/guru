package com.example.beepme

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class HistoryActivity : AppCompatActivity() {

    lateinit var layoutHistory: LinearLayout
    lateinit var myHelper: MyDBHelper
    lateinit var sqlDB: SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        layoutHistory = findViewById(R.id.layoutHistory)
        myHelper = MyDBHelper(this) // 내부 클래스인 MyDBHelper 클래스의 객체

        showDB() // 최근 본 바코드 정보를 UI로 출력
    }

    // ScanActivity 페이지 호출
    private fun callScanActivity(barNum: String) {
        if (barNum.isNotEmpty()) {
            val intent = Intent(this, ScanActivity::class.java)
            intent.putExtra("EXTRA_MESSAGE", barNum)
            startActivity(intent)
        } else {
            showToast("바코드 번호가 없습니다.")
        }
    }

    // SQLite 데이터베이스 사용을 위한 클래스(데이터베이스 이름 : historyDB)
    inner class MyDBHelper(context: Context) : SQLiteOpenHelper(context, "historyDB", null, 1) {
        override fun onCreate(db: SQLiteDatabase?) {
            db?.execSQL("CREATE TABLE historyTBL(hisBarCd CHAR(20) PRIMARY KEY, hisPrdNm NVARCHAR);")
        }

        override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
            db?.execSQL("DROP TABLE IF EXISTS historyTBL")
            onCreate(db)
        }
    }

    // 데이터베이스의 정보로 UI 업데이트
    private fun showDB() {
        sqlDB = myHelper.readableDatabase // 읽기, 쓰기 모두 가능한 데이터베이스

        val cursor: Cursor
        cursor = sqlDB.rawQuery("SELECT * FROM historyTBL ORDER BY ROWID DESC;", null)

        var num = 0

        if (cursor.moveToFirst()) {
            do {
                var hisBarCd = cursor.getString(0)
                var hisPrdNm = cursor.getString(1)

                var layoutItem: LinearLayout = LinearLayout(this)
                layoutItem.orientation = LinearLayout.HORIZONTAL
                layoutItem.setPadding(20, 10, 20, 10)
                layoutItem.id = num
                layoutItem.setTag(hisBarCd)

                var addHisButton = Button(this)
                addHisButton.text = "🔍"
                layoutItem.addView(addHisButton)

                var addHisBarCd = TextView(this)
                addHisBarCd.text = hisBarCd
                addHisBarCd.width = 350
                addHisBarCd.setPadding(30, 0, 30, 0)
                layoutItem.addView(addHisBarCd)

                var addHisPrdNm = TextView(this)
                addHisPrdNm.text = hisPrdNm
                layoutItem.addView(addHisPrdNm)

                addHisButton.setOnClickListener {
                    callScanActivity(hisBarCd)
                }

                layoutHistory.addView(layoutItem)
                num++

            } while (cursor.moveToNext())
        } else {
            showToast("데이터가 없습니다.")
        }

        cursor.close()
        sqlDB.close()
    }

    // 메시지를 출력하는 함수
    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }
}
