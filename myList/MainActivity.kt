package com.example.beepme

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.Manifest
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.beepme.databinding.ActivityMainBinding
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import javax.xml.parsers.DocumentBuilderFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var textToCode = ""

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                showCamera()
            } else {
                // 사용자에게 이 권한이 필요한 이유를 보여줍니다
                Toast.makeText(this, "카메라 권한이 필요합니다", Toast.LENGTH_SHORT).show()
            }
        }

    private val qrCodeLauncher =
        registerForActivityResult(ScanContract()) { result ->
            if (result.contents == null) {
                Toast.makeText(this, "취소됨", Toast.LENGTH_SHORT).show()
            } else {
                callScanResult(result.contents)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initBinding()
        initViews()
    }

    private fun initBinding() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun initViews() {
        binding.SaveListbtn.setOnClickListener {
            // Call the function to display saved data
            callSavedActivity()
        }

        binding.scanButton.setOnClickListener {
            checkPermissionAndShowActivity(this)
        }

        binding.historyButton.setOnClickListener {
            callHistoryActivity()
        }
    }

    private fun callHistoryActivity() {
        val editText = binding.textResult
        val message = editText.text.toString()

        val intent = Intent(this, HistoryActivity::class.java).apply {
            putExtra("EXTRA_MESSAGE", message)
        }

        startActivity(intent)
    }

    private fun callSavedActivity() {
        val intent = Intent(this, SavedActivity::class.java)
        startActivity(intent)
    }

    private fun showCamera() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
            setPrompt("Scan BAR code")
            setCameraId(0)
            setBeepEnabled(false)
            setBarcodeImageEnabled(true)
            setOrientationLocked(false)
        }

        qrCodeLauncher.launch(options)
    }

    private fun checkPermissionAndShowActivity(context: Context) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            showCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun callScanResult(barNum: String) {
        val intent = Intent(this, ScanResult::class.java).apply {
            putExtra("EXTRA_MESSAGE", barNum)
        }
        startActivity(intent)
    }

    private inner class NetworkThread(private var url: String) : Runnable {
        override fun run() {
            try {
                val xml: Document =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url)
                xml.documentElement.normalize()
                // 찾고자 하는 데이터가 어느 노드 아래에 있는지 확인
                val list: NodeList = xml.getElementsByTagName("row")
                // list.length-1 만큼 얻고자 하는 태그의 정보를 가져온다
                for (i in 0 until list.length) {
                    val n = list.item(i)
                    // 노드가 ELEMENT_NODE인 경우에만 처리
                    if (n.nodeType == Element.ELEMENT_NODE) {
                        val elem = n as Element

                        println("상품명 : ${elem.getElementsByTagName("PRDLST_NM").item(0).textContent}")

                        textToCode += "상품명 : ${elem.getElementsByTagName("PRDLST_NM").item(0).textContent}\n"
                    }
                }

                // UI 업데이트
                GlobalScope.launch(Dispatchers.Main) {
                    binding.textResult.text = textToCode
                }

            } catch (e: Exception) {
                Log.d("TTT", "오픈API" + e.toString())
            }
        }
    }
}
