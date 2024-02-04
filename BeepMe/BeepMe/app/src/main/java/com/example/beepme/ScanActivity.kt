package com.example.beepme

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import javax.xml.parsers.DocumentBuilderFactory

class ScanActivity : AppCompatActivity() {

    lateinit var reportNumT : String
    lateinit var barNum : TextView
    lateinit var prdlstNm : TextView
    lateinit var allergy : TextView
    lateinit var nutrient : TextView
    lateinit var manufacture : TextView
    lateinit var myHelper : MyDBHelper
    lateinit var sqlDB : SQLiteDatabase
    lateinit var saveAtMyInfo : Button

    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        reportNumT = "" // 품목 번호 변수
        prdlstNm = findViewById(R.id.prdlstNm) //상품명 textView
        allergy = findViewById(R.id.allergy) //알레르기성분 textView
        nutrient = findViewById(R.id.nutrient) //영양성분 textView
        manufacture = findViewById(R.id.manufacture) //제조사 textView
        barNum = findViewById(R.id.barNum) //바코드번호 textView
        saveAtMyInfo = findViewById(R.id.saveAtMyInfo) // Mylist에 저장하는 버튼
        myHelper = MyDBHelper(this) // 내부 클래스인 MyDBHelper 클래스의 객체

        //바코드 스캔했을 때 바코드 정보를 scan 페이지(현재 페이지)로 받아옴
        val barNumT = intent.getStringExtra("EXTRA_MESSAGE") ?: ""

        // 바코드 번호가 ""가 아닌 경우
        if (barNumT != "") {
            startBarCdNetworkThread(barNumT) // 바코드연계제품 api 호출 스레드 함수
            barNum.setText(barNumT) // 스캔한 바코드 번호로 textView 업데이트
        }

        // saveAyMyInfo 버튼을 누르면 db에 저장되도록 함
        saveAtMyInfo.setOnClickListener {
            if (::reportNumT.isInitialized) {
                saveToDB2(reportNumT, "LIST")
            } else {
                Toast.makeText(applicationContext, "보고서 번호를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // SavedDBHelper에 데이터를 넣도록 함.
    private fun saveToDB2(barNum: String, source: String) {
        val savedDBHelper = SavedDBHelper(this)
        val savedSQLDB = savedDBHelper.writableDatabase

        val insertQuery =                      //db에 어떤 데이터들이 들어갈지 틀을 짜준다.
            "INSERT INTO savedTBL(barNum, Name, allergy, nutrient, manufacture) VALUES('$barNum', '${prdlstNm.text.toString()}', '${allergy.text.toString()}', '${nutrient.text.toString()}', '${manufacture.text.toString()}');"
        savedSQLDB.execSQL(insertQuery)

        savedSQLDB.close()
        Toast.makeText(applicationContext, "데이터가 '$source'로 저장되었습니다.", Toast.LENGTH_SHORT).show()
    }

    // 바코드연계제품(식약처) api와 연결하는 스레드를 시작하는 함수. 인식한 바코드 번호를 매개변수로 받아서 api의 호출변수로 사용
    private fun startBarCdNetworkThread(barNum : String) {
        // 키 값
        val key = "6c3d3ae1a7c649b08ff9"
        // API 정보를 가지고 있는 주소
        val url = "https://openapi.foodsafetykorea.go.kr/api/"+key+"/C005/xml/1/5/BAR_CD="+barNum
        val thread = Thread(BarCdNetworkThread(url)) // 스레드 생성
        Toast.makeText(this,"검색 중",Toast.LENGTH_SHORT).show() // 검색 중 토스트 메시지
        thread.start() // 스레드 시작
        thread.join() // BarCdNetworkThread가 종료한 후 HaccpNetworkThread가 실행(reportNum 전달 필요)
        if (reportNumT != ""){ // 요소가 있는 경우
            startHaccpNetworkThread(barNum, reportNumT) // haccp api 스레드 호출 함수
        }
    }

    // 바코드연계제품(식약처) api 호출 클래스
    inner class BarCdNetworkThread(private var url : String) : Runnable {
        override fun run() {
            try {
                // xml 문서를 파싱하여 Document 객체로 변환
                val xml : Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url)
                xml.documentElement.normalize() // 문서 구조 표준화하여 중복된 공백 제거
                val count = xml.getElementsByTagName("total_count").item(0).textContent.toInt() // 검색결과 개수

                // 검색 결과가 0개가 아닌 경우에만 실행
                if(count != 0){
                    val list : NodeList = xml.getElementsByTagName("row") // row 태그를 가진 요소 목록 저장

                    // list에 있는 모든 요소에 대해 반복
                    for (i in 0 until list.length) {
                        val n = list.item(i) // 현재 인덱스에 해당하는 요소

                        // 노드가 ELEMENT_NODE인 경우에만 처리
                        if (n.nodeType == Element.ELEMENT_NODE) {
                            val elem = n as Element // 가져온 요소 Element로 형변환 하여 저장

                            // 품목번호. 요소가 없는 경우는 빈 문자열 저장
                            val reportNumNode = elem.getElementsByTagName("PRDLST_REPORT_NO").item(0)
                            reportNumT = if (reportNumNode != null) reportNumNode.textContent else ""
                        }
                    }
                }

            } catch (e : Exception) {
                Log.d("TTT", "오픈API"+e.toString()) // logcat에 예외 정보 출력
            }
        }
    }

    // haccp api와 연결하는 스레드를 시작하는 함수. 품목번호를 매개변수로 받아서 api의 호출변수로 사용
    private fun startHaccpNetworkThread(barNum : String, reportNum : String) {
        // 키 값
        val key = "%2FAzAbHNIGQenaz3boVd8pexlMAL5xSnuRAwQv5O9fdJw6qUSbbwhgxPQzaUcn3IojCkK5ET%2Fsbk8rHGRZWUg6w%3D%3D"
        // API 정보를 가지고 있는 주소
        val url = "https://apis.data.go.kr/B553748/CertImgListServiceV2/getCertImgListServiceV2?serviceKey="+key+"&prdlstReportNo="+reportNum
        val thread = Thread(HaccpNetworkThread(barNum, url)) // 스레드 생성
        thread.start()// 스레드 시작
    }


    // Haccp 제품 api 호출 클래스
    inner class HaccpNetworkThread(private var barNum : String, private var url : String) : Runnable {
        override fun run() {
            var prdlstNmT = ""
            var allergyT = ""
            var nutrientT = ""
            var manufactureT = ""

            try {
                // xml 문서를 파싱하여 Document 객체로 변환
                val xml : Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url)
                xml.documentElement.normalize() // 문서 구조 표준화하여 중복된 공백 제거
                val list : NodeList = xml.getElementsByTagName("item") // item 태그를 가진 요소 목록 저장

                // list에 있는 모든 요소에 대해 반복
                for (i in 0 until list.length) {
                    val n = list.item(i) // 현재 인덱스에 해당하는 요소

                    // 노드가 ELEMENT_NODE인 경우에만 처리
                    if (n.nodeType == Element.ELEMENT_NODE) {
                        val elem = n as Element // 가져온 요소 Element로 형변환 하여 저장

                        //상품명. 요소가 없는 경우는 빈 문자열 저장
                        val prdlstNmNode = elem.getElementsByTagName("prdlstNm").item(0)
                        prdlstNmT = if (prdlstNmNode != null) prdlstNmNode.textContent else ""

                        //알레르기성분. 요소가 없는 경우는 빈 문자열 저장
                        val allergyNode = elem.getElementsByTagName("allergy").item(0)
                        allergyT = if (allergyNode != null) allergyNode.textContent else ""

                        //영양성분. 요소가 없는 경우는 빈 문자열 저장
                        val nutrientNode = elem.getElementsByTagName("nutrient").item(0)
                        nutrientT = if (nutrientNode != null) nutrientNode.textContent else ""

                        //제조사. 요소가 없는 경우는 빈 문자열 저장
                        val manufactureNode = elem.getElementsByTagName("manufacture").item(0)
                        manufactureT = if (manufactureNode != null) manufactureNode.textContent else ""

                    }
                }

                // runOnUiThread를 사용하여 UI 업데이트
                runOnUiThread {
                    // 쓰레드에서 얻은 데이터로 UI 업데이트
                    prdlstNm.text = prdlstNmT // 상품명
                    nutrient.text = nutrientT // 영양성분
                    manufacture.text = manufactureT //제조사

                    //알아낸 상품 정보를 데이터베이스에 저장
                    saveToDB(barNum, prdlstNmT)

                    // 알러지 체크
                    checkAllergy(allergyT,allergy)
                }

            } catch (e : Exception) {
                Log.d("TTT", "오픈API"+e.toString()) // logcat에 예외 정보 출력
            }
        }
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

    // 데이터 베이스에 저장
    // 데이터 베이스에 저장
    private fun saveToDB(barcd : String, prdnm : String) {
        sqlDB = myHelper.writableDatabase // 읽기,쓰기 모두 가능한 데이터베이스

        val query = "SELECT COUNT(*) FROM historyTBL" // 테이블 전체 행 개수 가져오는 쿼리
        val cursor = sqlDB.rawQuery(query, null) // query를 실행한 결과
        cursor.moveToFirst() // 첫 번째 행으로 cursor 이동
        val rowCount = cursor.getInt(0) // 행 수
        cursor.close()

        // 현재 바코드 번호 historyTBL 선택
        val queryBarcd = "SELECT * FROM historyTBL WHERE hisBarCd = ?"
        val cursorBarcd = sqlDB.rawQuery(queryBarcd, arrayOf(barcd))

        // 바코드 정보를 추가, 삭제하는 쿼리
        val deleteQuery = "DELETE FROM historyTBL WHERE hisBarCd = '$barcd';"
        val deleteOldestQuery = "DELETE FROM historyTBL WHERE hisBarCd IN (SELECT hisBarCd FROM historyTBL ORDER BY ROWID ASC LIMIT 1)"
        val insertQuery = "INSERT INTO historyTBL VALUES('$barcd','$prdnm');"


        if(cursorBarcd.moveToFirst()){ // 현재 추가하려는 값과 중복된 값이 있는 경우
            sqlDB.execSQL(deleteQuery) // 중복된 값을 가진 행 데이터베이스에서 삭제(현재 값이 최근 값으로 추가되도록)
        } else{ // 중복된 값이 없는 경우
            if(rowCount >= 3){  // 데이터 3개 이상인 경우
                sqlDB.execSQL(deleteOldestQuery) // 가장 오래된 값을 가진 행 데이터베이스에서 삭제
            }
        }
        sqlDB.execSQL(insertQuery) // 바코드 정보 데이터베이스에 추가
        cursorBarcd.close()
        sqlDB.close()
    }

    //데이터베이스 관리
    inner class AllergyDBHelper(context: Context) : SQLiteOpenHelper(context, "selected_items", null, 1) {

        val TABLE_NAME = "selected_items"
        val COLUMN_NAME = "item_name"

        //데이터베이스가 처음 생성될 때 호출하는 메서드
        override fun onCreate(db: SQLiteDatabase?) {
            //테이블을 생성하는 SQL 쿼리 정의 및 실행
            val createTableQuery = "CREATE TABLE $TABLE_NAME ($COLUMN_NAME TEXT)"
            db?.execSQL(createTableQuery)
        }

        //데이터베이스의 버전이 변경될 때 호출하는 메서드
        override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
            // 데이터베이스 업데이트가 필요한 경우 여기서 처리 가능
        }
    }

    // 데이터베이스의 정보로 ui 업데이트
    private fun checkAllergy(prdAllergyText:String, prdAllergy: TextView) {
        var allergyDBHelper = AllergyDBHelper(this)

        sqlDB = allergyDBHelper.readableDatabase // 읽기 가능한 데이터베이스
        // 나의 알레르기 성분이 있을 때만 부분적으로 표시하기 위해 span 형태의 string으로 생성
        var prdAllergySpan = SpannableStringBuilder(prdAllergyText)

        // 데이터베이스에서 데이터를 검색해 cursor 객체에 반환
        val cursor: Cursor
        cursor = sqlDB.rawQuery("SELECT * FROM selected_items;", null)

        // 나의 알레르기가 있고 제품에 알레르기 성분 정보가 있을 때
        if (cursor.moveToFirst() && prdAllergyText !="") {
            do {
                var myAllergy = cursor.getString(0) // 나의 알레르기
                var isMyAllergy = prdAllergyText.indexOf(myAllergy) // 나의 알레르기가 제품에 포함되는지 여부
                var length = myAllergy.length // 알레르기 문자열 길이
                if (isMyAllergy != -1){
                    // 나의 알레르기가 포함되어 있다면 그 부분을 눈에 잘 들어오게 해당 문자 빨갛고 크고 밑줄 있게 설정
                    val colorBlueSpan = ForegroundColorSpan(Color.argb(200,255,0,0))
                    prdAllergySpan.setSpan(colorBlueSpan, isMyAllergy, isMyAllergy+length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    prdAllergySpan.setSpan(UnderlineSpan(), isMyAllergy, isMyAllergy+length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    prdAllergySpan.setSpan(RelativeSizeSpan(1.5f), isMyAllergy, isMyAllergy+length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            } while (cursor.moveToNext()) // 다음 행이 유효한 경우

            // 알러지 성분 정보를 나타내는 textView의 텍스트로 생성한 span형태 string을 지정
            prdAllergy.text = prdAllergySpan
        }

        cursor.close()
        sqlDB.close()
    }
}