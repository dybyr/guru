//myList에 들어갈 항목들을 보여주는 창

package com.example.beepme

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SavedActivity : AppCompatActivity() {
    private lateinit var linearLayout: LinearLayout
    private lateinit var resetButton: Button
    private lateinit var savedDBHelper: SavedDBHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved)

        linearLayout = findViewById(R.id.linearLayout)
        resetButton = findViewById(R.id.resetButton)
        savedDBHelper = SavedDBHelper(this)

        resetButton.setOnClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                savedDBHelper.resetData()
                loadDataAndDisplay()
            }
        }

        loadDataAndDisplay()
    }

    private fun loadDataAndDisplay() {
        GlobalScope.launch(Dispatchers.IO) {
            val data = loadDataFromDB()

            withContext(Dispatchers.Main) {

            }
        }
    }

    private suspend fun loadDataFromDB(): String {
        val result = StringBuilder()

        val sqlDB = savedDBHelper.writableDatabase

        val query = "SELECT * FROM savedTBL"
        val cursor = sqlDB.rawQuery(query, null)

        while (cursor.moveToNext()) {                              //savedTBL에서 상품명, 알러지정보, 제조사에 맞는 정보와 위치를 불러온다.
            val prdlstNmValue = cursor.getString(1)
            val allergyValue = cursor.getString(2)
            val manufactureValue = cursor.getString(4)

            // 결과를 동적으로 추가하는 TextView 생성
            val resultTextView = TextView(this)
            resultTextView.text = "상품명: $prdlstNmValue\n알러지정보: $allergyValue\n제조사: $manufactureValue\n"
            resultTextView.textSize = 16f

            // LinearLayout에 TextView 추가
            linearLayout.addView(resultTextView)

            result.append("상품명: $prdlstNmValue, 알러지정보: $allergyValue, 제조사: $manufactureValue\n")
        }

        cursor.close()
        sqlDB.close()

        return result.toString()
    }
}
