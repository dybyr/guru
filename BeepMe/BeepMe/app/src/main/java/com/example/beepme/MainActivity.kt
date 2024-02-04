package com.example.beepme

import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import android.content.Context
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class MainActivity : AppCompatActivity() {

    lateinit var listButton: Button
    lateinit var scanButton: Button
    lateinit var historyButton: Button
    lateinit var allergyButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listButton = findViewById(R.id.listButton) // 리스트(저장한 항목) 버튼
        scanButton = findViewById(R.id.scanButton) // 스캔 버튼
        historyButton = findViewById(R.id.historyButton) // 히스토리(최근 본 바코드) 버튼
        allergyButton = findViewById(R.id.allergyButton)  //알러지(나의 알러지) 버튼

        listButton.setOnClickListener {
            callSavedActivity()  // MyListActivity 페이지 호출 함수
        }

        // 스캔 버튼 클릭 시 동작
        scanButton.setOnClickListener {
            checkPermissionAndActivity(this) // 카메라 권한 확인 및 동작 함수
        }

        // 히스토리 버튼 클릭 시 동작
        historyButton.setOnClickListener {
            callHistoryActivity() // HistoryActivity 페이지 호출 함수
        }

        // 알러지 버튼 클릭 시 동작
        allergyButton.setOnClickListener {
            callAllergyActivity() // AllergyActivity 페이지 호출 함수
        }
    }

    private fun callAllergyActivity() {
        val intent = Intent(this, AllergyActivity::class.java) // intent 생성
        startActivity(intent) // 액티비티 호출
    }

    // HistoryActivity 페이지 호출
    private fun callHistoryActivity() {
        val intent = Intent(this, HistoryActivity::class.java) // intent 생성
        startActivity(intent) // 액티비티 호출
    }

    // 카메라 권한 확인 및 동작
    private fun checkPermissionAndActivity(context: Context) {
        val cameraPermission = Manifest.permission.CAMERA

        // 카메라 권한이 이미 허용되어 있는 경우
        if (ContextCompat.checkSelfPermission(context, cameraPermission) == PackageManager.PERMISSION_GRANTED) {
            showCamera() // 카메라 화면 표시 함수 호출
        } else { // 카메라 권한이 없는 경우
            // 사용자가 이전에 권한 요청을 거부한 경우
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, cameraPermission)) {
                Toast.makeText(context, "카메라 권한이 필요합니다", Toast.LENGTH_LONG).show()
            } else { // 사용자가 권한 요청을 거부하지 않은 경우
                requestPermissionLauncher.launch(cameraPermission) // 카메라 권한 요청 함수 호출
            }
        }
    }

    // MyListActivity 페이지 호출
    private fun callSavedActivity() {
        val intent = Intent(this, MylistActivity::class.java)
        startActivity(intent)
    }

    // 권한 요청
    private var requestPermissionLauncher: ActivityResultLauncher<String> =
        // 권한 요청 결과 처리 함수
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) { // 권한 승인
                showCamera() // 카메라 화면 표시 함수 호출
            } else { // 권한 미승인
                Toast.makeText(this, "카메라 권한이 필요합니다", Toast.LENGTH_LONG).show()
            }
        }

    // 카메라 표시
    private fun showCamera() {
        // 스캔 옵션 설정
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES) // 스캔할 바코드 유형 모든 타입으로 설정
        options.setPrompt("Scan BAR code") // 스캔 화면에 메시지 표시
        options.setCameraId(0) // 사용할 카메라 ID 설정(후면:0, 전면:1)
        options.setBeepEnabled(true) // 스캔 시 삡 소리 사용 여부 설정
        options.setBarcodeImageEnabled(true) // 스캔한 바코드 이미지 저장
        options.setOrientationLocked(false) // 화면 방향 잠금 해제

        barCodeLauncher.launch(options) // 설정한 스캔 옵션으로 바코드 스캔 함수 호출
    }

    // 바코드 스캔
    private var barCodeLauncher: ActivityResultLauncher<ScanOptions> =
        // 스캔 결과 처리 함수
        registerForActivityResult(ScanContract()) { result ->
            if (result.contents == null) { // 스캔이 취소된 경우
                Toast.makeText(this, "스캔이 취소되었습니다", Toast.LENGTH_SHORT).show()
            } else { // 스캔 결과가 있을 경우
                Toast.makeText(this, "인식한 바코드 번호: ${result.contents}", Toast.LENGTH_SHORT).show()
                callScanActivity(result.contents) // 스캔된 바코드를 매개변수로 전달하여 callScanActivity 함수 호출
            }
        }

    // ScanActivity 페이지 호출
    private fun callScanActivity(barNum: String) {
        val intent = Intent(this, ScanActivity::class.java) // intent 생성
        intent.putExtra("EXTRA_MESSAGE", barNum) // 인식한 바코드 번호를 intent에 담아 ScanActivity 페이지로 전송
        startActivity(intent) // 액티비티 호출
    }
}
