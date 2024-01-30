package com.example.beepme

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import javax.xml.parsers.DocumentBuilderFactory

class ScanResult : AppCompatActivity() {
    lateinit var reportNum : String
    lateinit var barNumText: TextView
    lateinit var prdlstNm: TextView
    lateinit var allergy: TextView
    lateinit var nutrient: TextView
    lateinit var manufacture: TextView
    lateinit var myHelper: myDBHelper
    lateinit var sqlDB: SQLiteDatabase
    lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_result)

        prdlstNm = findViewById(R.id.prdlstNm)
        allergy = findViewById(R.id.allergy)
        nutrient = findViewById(R.id.nutrient)
        manufacture = findViewById(R.id.manufacture)
        barNumText = findViewById(R.id.barNumText)
        myHelper = myDBHelper(this)
        saveButton = findViewById(R.id.savebtn)

        val barNum = intent.getStringExtra("EXTRA_MESSAGE")
        var barnumT = "바코드번호: $barNum"

        if (barNum != null) {
            startNetworkThread(barNum)
            barNumText.setText(barnumT)
        }

        saveButton.setOnClickListener {
            if (::reportNum.isInitialized) {
                saveToDB(reportNum, "savebtn")
            } else {
                Toast.makeText(applicationContext, "보고서 번호를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveToDB(barNum: String, source: String) {
        val savedDBHelper = SavedDBHelper(this)
        val savedSQLDB = savedDBHelper.writableDatabase

        val insertQuery =                      //db에 어떤 데이터들이 들어갈지 틀을 짜준다.
            "INSERT INTO savedTBL(gNumber, gName, allergy, nutrient, manufacture) VALUES('$barNum', '${prdlstNm.text.toString()}', '${allergy.text.toString()}', '${nutrient.text.toString()}', '${manufacture.text.toString()}');"
            //"INSERT INTO savedTBL(gNumber, gName) VALUES('$barNum', '${prdlstNm.text.toString()}');"
        savedSQLDB.execSQL(insertQuery)

        savedSQLDB.close()
        Toast.makeText(applicationContext, "데이터가 출처 '$source'로 저장되었습니다.", Toast.LENGTH_SHORT).show()
    }
    private fun startNetworkThread(barCd:String) {
        // 키 값
        val key = "6c3d3ae1a7c649b08ff9"
        // API 정보를 가지고 있는 주소
        val url = "https://openapi.foodsafetykorea.go.kr/api/"+key+"/C005/xml/1/5/BAR_CD="+barCd
        // 쓰레드 생성
        val thread = Thread(NetworkThread(url))
        Toast.makeText(this, "번호: $barCd", Toast.LENGTH_SHORT).show()   //쓰레드 시작 확인용
        thread.start()// 쓰레드 시작
        thread.join()
        startHaccpNetworkThread(reportNum)
        Toast.makeText(this,"쓰레드종료", Toast.LENGTH_SHORT).show() //쓰레드 종료 확인용
        //saveToDB(barCd)
    }

    private fun startHaccpNetworkThread(reportNum:String) {
        var key = "%2FAzAbHNIGQenaz3boVd8pexlMAL5xSnuRAwQv5O9fdJw6qUSbbwhgxPQzaUcn3IojCkK5ET%2Fsbk8rHGRZWUg6w%3D%3D"
        var url = "https://apis.data.go.kr/B553748/CertImgListServiceV2/getCertImgListServiceV2?serviceKey="+key+"&prdlstReportNo="+reportNum
        // 쓰레드 생성
        val thread = Thread(HaccpNetworkThread(url))
        Toast.makeText(this, "번호: $reportNum", Toast.LENGTH_SHORT).show()   //쓰레드 시작 확인용
        thread.start()// 쓰레드 시작
        thread.join()
        Toast.makeText(this,"쓰레드2종료", Toast.LENGTH_SHORT).show() //쓰레드 종료 확인용
    }

    inner class myDBHelper(context: Context) : SQLiteOpenHelper(context, "groupDB", null, 1){    //이부분은 히스토리를 저장할 db라고 생각하고 작성했습니다.
        override fun onCreate(db: SQLiteDatabase?) {
            db!!.execSQL("CREATE TABLE groupTBL(gNumber INTEGER, gName CHAR(20) PRIMARY KEY);")
        }

        override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
            db!!.execSQL("DROP TABLE IF EXISTS groupTBL")
            onCreate(db)
        }
    }

    inner class NetworkThread(private var url: String): Runnable {
        override fun run() {
            try {
//                var textToCode: String= ""
                val xml : Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url)
                xml.documentElement.normalize()
                //찾고자 하는 데이터가 어느 노드 아래에 있는지 확인
                val list: NodeList = xml.getElementsByTagName("row")
                //list.length-1 만큼 얻고자 하는 태그의 정보를 가져온다
                for (i in 0 until list.length) {
                    val n = list.item(i)
                    // 노드가 ELEMENT_NODE인 경우에만 처리
                    if (n.nodeType == Element.ELEMENT_NODE) {
                        val elem = n as Element

                        reportNum=elem.getElementsByTagName("PRDLST_REPORT_NO").item(0).textContent
                        println("바코드 : ${elem.getElementsByTagName("BAR_CD").item(0).textContent}")   //logcat 확인용
                        /*
                        //                        textToCode = "${elem.getElementsByTagName("PRDLST_REPORT_NO").item(0).textContent}"
                                                var haccpKey = "%2FAzAbHNIGQenaz3boVd8pexlMAL5xSnuRAwQv5O9fdJw6qUSbbwhgxPQzaUcn3IojCkK5ET%2Fsbk8rHGRZWUg6w%3D%3D"
                                                var haccpUrl = "http://apis.data.go.kr/B553748/CertImgListServiceV2/serviceKey="+haccpKey+"&prdlstReportNo="+reportNum

                                                val thread = Thread(NetworkThreadHaccp(haccpUrl))
                                                thread.start()// 쓰레드 시작
                                                thread.join()*/
                        /*
                        // 상품 정보
                        textToCode += "상품명 : ${elem.getElementsByTagName("prdlstNm").item(0).textContent} \n"
                        textToCode += "알러지 정보 : ${elem.getElementsByTagName("allergy").item(0).textContent} \n"
                        textToCode += "제조사 : ${elem.getElementsByTagName("manufacture").item(0).textContent} \n"
                        */

                    }
                }
                /*
                                // runOnUiThread를 사용하여 UI 업데이트
                                runOnUiThread {
                                    // 쓰레드에서 얻은 데이터로 UI 업데이트
                                    barNumText.setText()
                                }
                */
            } catch (e: Exception) {
                Log.d("TTT", "오픈API"+e.toString())
            }
        }
    }

    inner class HaccpNetworkThread(private var url: String): Runnable {
        override fun run() {
            var prdlstNmT: String= ""
            var allergyT: String=""
            var nutrientT: String=""
            var manufactureT: String=""
            try {
                val xml : Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url)
                xml.documentElement.normalize()
                //찾고자 하는 데이터가 어느 노드 아래에 있는지 확인
                val list: NodeList = xml.getElementsByTagName("item")
                //list.length-1 만큼 얻고자 하는 태그의 정보를 가져온다
                for (i in 0 until list.length) {
                    val n = list.item(i)
                    // 노드가 ELEMENT_NODE인 경우에만 처리
                    if (n.nodeType == Element.ELEMENT_NODE) {
                        val elem = n as Element

                        println("상품명 : ${elem.getElementsByTagName("prdlstNm").item(0).textContent}")   //logcat 확인용

                        prdlstNmT = "상품명: ${elem.getElementsByTagName("prdlstNm").item(0).textContent}"
                        allergyT="알러지정보: ${elem.getElementsByTagName("allergy").item(0).textContent}"
                        nutrientT="영양성분: ${elem.getElementsByTagName("nutrient").item(0).textContent}"
                        manufactureT="제조사: ${elem.getElementsByTagName("manufacture").item(0).textContent}"

                        /*
                        // 상품 정보
                        textToCode += "상품명 : ${elem.getElementsByTagName("prdlstNm").item(0).textContent} \n"
                        textToCode += "알러지 정보 : ${elem.getElementsByTagName("allergy").item(0).textContent} \n"
                        textToCode += "제조사 : ${elem.getElementsByTagName("manufacture").item(0).textContent} \n"
                        */

                    }
                }

                // runOnUiThread를 사용하여 UI 업데이트
                runOnUiThread {
                    // 쓰레드에서 얻은 데이터로 UI 업데이트
                    prdlstNm.setText(prdlstNmT)  // 상품명
                    allergy.setText(allergyT)   // 알러지
                    nutrient.setText(nutrientT) // 영양성분
                    manufacture.setText(manufactureT)   //제조사
                }

            } catch (e: Exception) {
                Log.d("TTT", "오픈API"+e.toString())
            }
        }
    }
}
