package com.example.beepme

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
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

    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        reportNumT = "" // 품목 번호 변수
        prdlstNm = findViewById(R.id.prdlstNm) //상품명 textView
        allergy = findViewById(R.id.allergy) //알레르기성분 textView
        nutrient = findViewById(R.id.nutrient) //영양성분 textView
        manufacture = findViewById(R.id.manufacture) //제조사 textView
        barNum = findViewById(R.id.barNum) //바코드번호 textView
        myHelper = MyDBHelper(this) // 내부 클래스인 MyDBHelper 클래스의 객체

        //바코드 스캔했을 때 바코드 정보를 scan 페이지(현재 페이지)로 받아옴
        val barNumT = intent.getStringExtra("EXTRA_MESSAGE") ?: ""

        // 바코드 번호가 ""가 아닌 경우
        if (barNumT != "") {
            startBarCdNetworkThread(barNumT) // 바코드연계제품 api 호출 스레드 함수
            barNum.setText(barNumT) // 스캔한 바코드 번호로 textView 업데이트
        }

    }

    // 바코드연계제품(식약처) api와 연결하는 스레드를 시작하는 함수. 인식한 바코드 번호를 매개변수로 받아서 api의 호출변수로 사용
    private fun startBarCdNetworkThread(barNum : String) {
        // 키 값
        val key = "6c3d3ae1a7c649b08ff9"
        // API 정보를 가지고 있는 주소
        val url = "https://openapi.foodsafetykorea.go.kr/api/"+key+"/C005/xml/1/5/BAR_CD="+barNum
        val thread = Thread(BarCdNetworkThread(url)) // 스레드 생성
        Toast.makeText(this,"검색 중",Toast.LENGTH_SHORT) // 검색 중 토스트 메시지
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
                    prdlstNm.setText(prdlstNmT) // 상품명
                    allergy.setText(allergyT) // 알러지성분
                    nutrient.setText(nutrientT) // 영양성분
                    manufacture.setText(manufactureT) //제조사

                    //알아낸 상품 정보를 데이터베이스에 저장
                    saveToDB(barNum, prdlstNmT)
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

}