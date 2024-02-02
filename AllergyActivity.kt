package com.example.beepme

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView

class AllergyActivity : AppCompatActivity() {

    lateinit var listView: ListView
    lateinit var adapter: ArrayAdapter<String>
    lateinit var btndone: Button
    lateinit var btnchange : Button
    private lateinit var dbHelper: DBHelper
    private val SELECTED_ITEMS_KEY = "selected_items_key"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_allergy)

        // 알러지 리스트
        val allergy = arrayOf("호두", "게", "새우", "오징어", "복숭아", "토마토", "닭고기", "돼지고기", "난류", "우유", "메밀", "땅콩", "대두", "밀", "잣", "고등어", "소고기", "아황산류", "조개류")

        dbHelper = DBHelper(this)
        listView = findViewById(R.id.listView)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, allergy)
        listView.adapter = adapter
        listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE

        savedInstanceState?.getStringArray(SELECTED_ITEMS_KEY)?.let { selectedItems ->
            for (i in 0 until listView.count) {
                listView.setItemChecked(i, selectedItems.contains(listView.getItemAtPosition(i) as String))
            }
        }


        listView.setOnItemClickListener { _, _, position, _ ->
            // 선택 상태가 변경될 때마다 호출되는 리스너
            handleItemSelection(position)
        }


        //완료 버튼을 누르면 데이터베이스에 선택한 알러지 저장
        btndone = findViewById(R.id.btndone)
        btndone.setOnClickListener {
            saveSelectedItemsToDatabase() //클릭 이벤트가 발생했을 때, saveSelectedItemsToDatabase() 함수를 호출하고 이 함수는 선택한 알러지를 데이터베이스에 저장하는 역할
        }

        //알러지를 다시 선택한 후에 수정 버튼을 누르면 수정된 알러지 데이터베이스에 저장
        btnchange = findViewById(R.id.btnchange)
        btnchange.setOnClickListener {
            updateDatabase()  // 클릭 이벤트가 발생했을 때, updateDatabase()함수를 호출하고, 이 함수는 다시 선택한 알러지를 데이터베이스에 업데이트하여 저장하는 역할
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the checked state before configuration changes
        val selectedItems = mutableListOf<String>()
        for (i in 0 until listView.count) {
            if (listView.isItemChecked(i)) {
                selectedItems.add(listView.getItemAtPosition(i) as String)
            }
        }
        outState.putStringArray(SELECTED_ITEMS_KEY, selectedItems.toTypedArray())
    }



    private fun handleItemSelection(position: Int) {
        // 주어진 위치(position)에 있는 항목의 선택 여부를 처리하는 함수
        val isSelected = listView.isItemChecked(position)
        // 주어진 위치의 항목이 현재 선택되었는지 확인
    }

    //데이터베이스에 선택된 알러지 항목을 저장하는 함수
    private fun saveSelectedItemsToDatabase() {
        val selectedItems = mutableListOf<String>() //빈 문자열을 저장할 가변 리스트를 생성



        for (i in 0 until listView.count) { //0부터 리스트뷰의 항목 개수 직전까지 반복하면서 각 알러지 항목의 선택 여부를 확인
            if (listView.isItemChecked(i)) { //현재 위치(i)의 알러지 항목이 선택되었는지 확인
                selectedItems.add(listView.getItemAtPosition(i) as String) //만약 해당 알러지 항목이 선택되었다면, selectItems 리스트에 해당 알러지 항목을 추가하고 형변환을 수행하여 문자열로 저장
            }
        }

        // 선택된 알러지 항목들이 담긴 리스트를 받아서 데이터베이스에 저장
        dbHelper.saveSelectedItems(selectedItems)
    }

    //데이터베이스에 업데이트 하는 함수
    private fun updateDatabase() {
        val selectedItems = mutableListOf<String>() //빈 문자열을 저장할 가변 리스트를 생성

        //리스트뷰의 항목 개수만큼 반복하면서 각 알러지 항목의 선택 여부를 확인
        for (i in 0 until listView.count) {
            if (listView.isItemChecked(i)) { //현재 위치(i)의 알러지 항목이 선택되었는지 확인
                selectedItems.add(listView.getItemAtPosition(i) as String)
                //현재 위치의(i)의 알러지 항목을 가져옴
                //as String을 통해 형변환을 수행하여 문자열로 저장
            }
        }

        val db = dbHelper.writableDatabase //dbHelper 객체의 writableDatabase를 사용하여 쓰기 가능한 데이터베이스를 가져옴
        db.beginTransaction() //트랜잭션 시작

        try {
            // 기존 테이블에서 모든 데이터를 삭제
            db.delete(DBHelper.TABLE_NAME, null, null)

            // 업데이트된 선택 항목들을 데이터베이스에 저장
            for (item in selectedItems) {
                val values = ContentValues().apply { //ContentValues()를 사용하여 데이터베이스에 추가할 값을 설정
                    put(DBHelper.COLUMN_NAME, item)
                }
                db.insert(DBHelper.TABLE_NAME, null, values) //db.insert를 사용하여 데이터베이스 테이블에 행을 추가
            }

            // 트랜잭션을 성공적으로 완료
            db.setTransactionSuccessful()
        } finally {
            // 트랜잭션을 종료
            db.endTransaction()
        }

        // 데이터베이스를 닫음
        db.close()
    }

    //데이터베이스 관리
    private class DBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        // 클래스의 인스턴스 없이 접근할 수 있는 멤버를 정의
        companion object {
            //데이터베이스의 이름, 버전, 테이블 이름, 열 이름을 정의
            const val DATABASE_NAME = "selected_items"
            const val DATABASE_VERSION = 1
            const val TABLE_NAME = "selected_items"
            const val COLUMN_NAME = "item_name"
        }

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

        //선택된 항목을 데이터베이스에 저장하는 메서드
        fun saveSelectedItems(selectedItems: List<String>) {
            //쓰기 가능한 데이터베이스를 가져옴
            val db = writableDatabase

            //선택된 항목들을 반복하면서 ContentValues를 생성하여 데이터베이스에 삽입함
            for (item in selectedItems) {
                val values = ContentValues().apply {
                    put(COLUMN_NAME, item)
                }
                db.insert(TABLE_NAME, null, values)
            }
            db.close() //데이터베이스를 닫음
        }

    }
}
