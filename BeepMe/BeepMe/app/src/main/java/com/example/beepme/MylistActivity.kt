package com.example.beepme

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.graphics.Typeface
import android.widget.ScrollView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MylistActivity : AppCompatActivity() {
    private lateinit var tableLayout: TableLayout
    private lateinit var resetButton: Button
    private lateinit var savedDBHelper: SavedDBHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mylist)

        tableLayout = findViewById(R.id.tableLayout)
        resetButton = findViewById(R.id.resetButton)
        savedDBHelper = SavedDBHelper(this)

        resetButton.setOnClickListener {
            // 데이터 리셋 버튼 클릭 시
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    savedDBHelper.resetData()
                    loadDataAndDisplay()
                } catch (e: Exception) {
                    // 오류 발생 시 메시지를 출력
                    e.printStackTrace()
                    showError("데이터 리셋 중 오류가 발생했습니다.")
                }
            }
        }

        loadDataAndDisplay()
    }

    private fun loadDataAndDisplay() {
        // 데이터 로드 및 화면에 표시
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val data = loadDataFromDB()

                withContext(Dispatchers.Main) {
                    displayDataInTable(data)
                }
            } catch (e: Exception) {
                // 오류 발생 시 메시지를 출력
                e.printStackTrace()
                showError("데이터 로드 중 오류가 발생했습니다.")
            }
        }
    }

    private suspend fun loadDataFromDB(): List<Triple<String, String, String>> {
        // 데이터베이스에서 데이터 로드
        val result = mutableListOf<Triple<String, String, String>>()

        val sqlDB = savedDBHelper.writableDatabase

        val query = "SELECT * FROM savedTBL"
        val cursor = sqlDB.rawQuery(query, null)

        // 제품명, 알러지 정보, 제조사를 불러옴
        while (cursor.moveToNext()) {
            val prdlstNmValue = cursor.getString(1)
            val allergyValue = cursor.getString(2)
            val manufactureValue = cursor.getString(4)

            result.add(Triple(prdlstNmValue, allergyValue, manufactureValue))
        }

        cursor.close()
        sqlDB.close()

        return result
    }

    private fun displayDataInTable(data: List<Triple<String, String, String>>) {
        // 테이블에 데이터 표시
        tableLayout.removeAllViews()

        // 테이블 헤더 추가
        val headerRow = TableRow(this).apply {
            setBackgroundColor(Color.parseColor("#303030")) // 헤더의 배경색 설정
        }
        listOf("상품명", "알러지정보", "제조사").forEach { text ->
            val textView = TextView(this).apply {
                this.text = text
                textSize = 20f // 글자 크기를 20으로 변경
                setTypeface(null, Typeface.BOLD) // 볼드 처리
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                layoutParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            headerRow.addView(textView)
        }
        tableLayout.addView(headerRow)

        // 데이터 행 추가
        data.forEachIndexed { index, (prdlstNm, allergy, manufacture) ->
            val row = TableRow(this).apply {
                setBackgroundColor(if (index % 2 == 0) Color.parseColor("#FFFFFF") else Color.parseColor("#F0F0F0")) // 행마다 배경색 변경
            }

            listOf(prdlstNm, allergy, manufacture).forEach { text ->
                val textView = TextView(this).apply {
                    this.text = text
                    textSize = 16f
                    gravity = Gravity.CENTER
                    setTextColor(Color.BLACK) // 텍스트 색상 설정
                    layoutParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                row.addView(textView)
            }
            tableLayout.addView(row)
        }
    }

    private fun showError(errorMessage: String) {
        // 오류 메시지를 스크롤뷰에 출력
        val scrollView = findViewById<ScrollView>(R.id.scrollView)
        val errorTextView = TextView(this).apply {
            text = errorMessage
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(Color.RED)
        }
        scrollView.addView(errorTextView)
    }
}
