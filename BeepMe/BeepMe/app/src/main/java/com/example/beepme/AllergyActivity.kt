package com.example.beepme
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.core.view.size

class AllergyActivity : AppCompatActivity() {

    lateinit var gridView: GridLayout
    lateinit var btndone: Button
    lateinit var btnchange: Button
    private lateinit var dbHelper: DBHelper
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_allergy)

        dbHelper = DBHelper(this)
        gridView = findViewById(R.id.gridLayout)
        sharedPreferences = getPreferences(Context.MODE_PRIVATE)

        // 알러지 리스트
        val allergy = arrayOf(
            "호두", "게", "새우", "오징어", "복숭아", "토마토", "닭고기", "돼지고기",
            "난류", "우유", "메밀", "땅콩", "대두", "밀", "잣", "고등어", "소고기", "아황산류", "조개류"
        )

        // 그리드 레이아웃에 버튼 추가
        for (item in allergy) {
            val button = Button(this)
            button.text = item
            button.tag = item // 버튼에 고유한 태그 설정
            button.setOnClickListener {
                toggleButtonSelection(button)
                saveButtonSelection(button)
            }
            val params = GridLayout.LayoutParams()
            params.width = 0 // 너비를 0으로 설정
            params.height = GridLayout.LayoutParams.WRAP_CONTENT
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1)
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f) // 가중치를 1로 설정
            params.setMargins(8, 8, 8, 8) // 버튼 간격 조절
            button.layoutParams = params
            gridView.addView(button)
            restoreButtonSelection(button)
        }

        // 완료 버튼을 누르면 데이터베이스에 선택한 알러지 저장
        btndone = findViewById(R.id.btndone)
        btndone.setOnClickListener {
            try {
                saveSelectedItemsToDatabase()
                Toast.makeText(this, "알러지 선택이 저장되었습니다.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "알러지 선택 저장 중 오류 발생", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }

        // 알러지를 다시 선택한 후에 수정 버튼을 누르면 수정된 알러지 데이터베이스에 저장
        btnchange = findViewById(R.id.btnchange)
        btnchange.setOnClickListener {
            try {
                updateDatabase()
                Toast.makeText(this, "알러지 선택이 수정되었습니다.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "알러지 선택 수정 중 오류 발생", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun toggleButtonSelection(button: Button) {
        button.isSelected = !button.isSelected
        if (button.isSelected) {
            button.setBackgroundColor(ContextCompat.getColor(this, R.color.yellow))
        } else {
            button.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        }
    }

    private fun restoreButtonSelection(button: Button) {
        val isSelected = sharedPreferences.getBoolean(button.tag.toString(), false)
        button.isSelected = isSelected
        if (isSelected) {
            button.setBackgroundColor(ContextCompat.getColor(this, R.color.yellow))
        } else {
            button.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        }
    }

    private fun saveButtonSelection(button: Button) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(button.tag.toString(), button.isSelected)
        editor.apply()
    }

    private fun saveSelectedItemsToDatabase() {
        val selectedItems = mutableListOf<String>()

        for (i in 0 until gridView.childCount) {
            val button = gridView.getChildAt(i) as Button
            if (button.isSelected) {
                selectedItems.add(button.tag.toString())
            }
        }

        dbHelper.saveSelectedItems(selectedItems)
    }

    private fun updateDatabase() {
        val selectedItems = mutableListOf<String>()

        for (i in 0 until gridView.childCount) {
            val button = gridView.getChildAt(i) as Button
            if (button.isSelected) {
                selectedItems.add(button.tag.toString())
            }
        }

        dbHelper.updateSelectedItems(selectedItems)
    }

    private class DBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
        companion object {
            const val DATABASE_NAME = "selected_items"
            const val DATABASE_VERSION = 1
            const val TABLE_NAME = "selected_items"
            const val COLUMN_NAME = "item_name"
        }

        override fun onCreate(db: SQLiteDatabase?) {
            val createTableQuery = "CREATE TABLE $TABLE_NAME ($COLUMN_NAME TEXT)"
            db?.execSQL(createTableQuery)
        }

        override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        }

        fun saveSelectedItems(selectedItems: List<String>) {
            val db = writableDatabase

            for (item in selectedItems) {
                val values = ContentValues().apply {
                    put(COLUMN_NAME, item)
                }
                db.insert(TABLE_NAME, null, values)
            }
            db.close()
        }

        fun updateSelectedItems(selectedItems: List<String>) {
            val db = writableDatabase
            db.beginTransaction()

            try {
                db.delete(TABLE_NAME, null, null)

                for (item in selectedItems) {
                    val values = ContentValues().apply {
                        put(COLUMN_NAME, item)
                    }
                    db.insert(TABLE_NAME, null, values)
                }

                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }

            db.close()
        }
    }
}
