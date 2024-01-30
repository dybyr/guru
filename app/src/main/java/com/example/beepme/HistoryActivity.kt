package com.example.beepme

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class HistoryActivity : AppCompatActivity() {

    lateinit var his1DetailButton: Button
    lateinit var his1BarCd: TextView
    lateinit var his1PrdNm: TextView
    lateinit var his2DetailButton: Button
    lateinit var his2BarCd: TextView
    lateinit var his2PrdNm: TextView
    lateinit var his3DetailButton: Button
    lateinit var his3BarCd: TextView
    lateinit var his3PrdNm: TextView
    lateinit var myHelper: MyDBHelper
    lateinit var sqlDB: SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        his1DetailButton = findViewById(R.id.his1DetailButton) // 최근 본 바코드(1) 정보 자세히 보기 버튼
        his1BarCd = findViewById(R.id.his1BarCd) // 최근 본 바코드(1)의 바코드 번호 textView
        his1PrdNm = findViewById(R.id.his1PrdNm) // 최근 본 바코드(1)의 상품명 textView
        his2DetailButton = findViewById(R.id.his2DetailButton) // 최근 본 바코드(2) 정보 자세히 보기 버튼
        his2BarCd = findViewById(R.id.his2BarCd) // 최근 본 바코드(2)의 바코드 번호 textView
        his2PrdNm = findViewById(R.id.his2PrdNm) // 최근 본 바코드(2)의 상품명 textView
        his3DetailButton = findViewById(R.id.his3DetailButton) // 최근 본 바코드(3) 정보 자세히 보기 버튼
        his3BarCd = findViewById(R.id.his3BarCd) // 최근 본 바코드(3)의 바코드 번호 textView
        his3PrdNm = findViewById(R.id.his3PrdNm) // 최근 본 바코드(3)의 상품명 textView
        myHelper = MyDBHelper(this) // 내부 클래스인 MyDBHelper 클래스의 객체

        showDB() // 최근 본 바코드 정보를 UI로 출력

        // 최근 본 바코드(1) 버튼 클릭 시 동작
        his1DetailButton.setOnClickListener {
            val barNm = his1BarCd.text?.toString() ?: "" // 최근 본 바코드(1)의 바코드 번호

            // 바코드 번호가 빈 문자열이 아닌 경우
            if(barNm != ""){
                callScanActivity(barNm) // 해당 바코드 정보를 매개 변수로 callScanActivity 함수 호출
            }
        }

        // 최근 본 바코드(2) 버튼 클릭 시 동작
        his2DetailButton.setOnClickListener {
            val barNm = his2BarCd.text?.toString() ?: "" // 최근 본 바코드(2)의 바코드 번호

            // 바코드 번호가 빈 문자열이 아닌 경우
            if(barNm != ""){
                callScanActivity(barNm) // 해당 바코드 정보를 매개 변수로 callScanActivity 함수 호출
            }
        }

        // 최근 본 바코드(3) 버튼 클릭 시 동작
        his3DetailButton.setOnClickListener {
            val barNm = his3BarCd.text?.toString() ?: "" // 최근 본 바코드(3)의 바코드 번호

            // 바코드 번호가 빈 문자열이 아닌 경우
            if(barNm != ""){
                callScanActivity(barNm) // 해당 바코드 정보를 매개 변수로 callScanActivity 함수 호출
            }
        }

    }

    // ScanActivity 페이지 호출
    private fun callScanActivity(barNum : String) {
        val intent = Intent(this,ScanActivity::class.java) // intent 생성
        intent.putExtra("EXTRA_MESSAGE",barNum) // 인식한 바코드 번호를 intent에 담아 ScanActivity 페이지로 전송
        startActivity(intent) // 액티비티 호출
    }

    // SQLite 데이터베이스 사용을 위한 클래스(데이터베이스 이름 : historyDB)
    inner class MyDBHelper(context : Context) : SQLiteOpenHelper(context, "historyDB", null, 1){
        override fun onCreate(db : SQLiteDatabase?) {
            // 데이터베이스 테이블 이름 : historyTBL
            // 데이터 베이스 열 항목 : hisBarCd(바코드 번호), hisPrdNm(상품명) *바코드 번호는 중복 불가
            db!!.execSQL("CREATE TABLE historyTBL(hisBarCd CHAR(20) PRIMARY KEY, hisPrdNm NVARCHAR);")
        }

        override fun onUpgrade(db : SQLiteDatabase?, oldVersion : Int, newVersion : Int) {
            db!!.execSQL("DROP TABLE IF EXISTS historyTBL")
            onCreate(db)
        }
    }

    // 데이터베이스의 정보로 ui 업데이트
    private fun showDB() {
        sqlDB = myHelper.readableDatabase // 읽기,쓰기 모두 가능한 데이터베이스

        // 데이터베이스에서 데이터를 역순으로 검색해 cursor 객체에 반환(그래야 최근 본 바코드 순서대로 출력됨)
        val cursor: Cursor
        cursor = sqlDB.rawQuery("SELECT * FROM historyTBL ORDER BY ROWID DESC;", null)

        val barCdViewArray = arrayOf(his1BarCd, his2BarCd, his3BarCd) // 바코드 번호 textView 배열
        val prdNmViewArray = arrayOf(his1PrdNm, his2PrdNm, his3PrdNm) // 상품명 textView 배열

        var index = 0  // 바코드 번호와 상품명 textView 배열의 인덱스

        // 데이터가 있는 경우
        if (cursor.moveToFirst()) {
            do {
                barCdViewArray[index].setText(cursor.getString(0)) // cursor에서 읽은 정보로 바코드 번호 ui 업데이트
                prdNmViewArray[index].setText(cursor.getString(1)) // cursor에서 읽은 정보로 상품명 ui 업데이트
                index++ // 인덱스 1 증가
            } while (cursor.moveToNext()) // 다음 행이 유효한 경우
        }

        cursor.close()
        sqlDB.close()
        Toast.makeText(applicationContext,"조회됨", Toast.LENGTH_SHORT).show()
    }

}