package com.minyook.planify_app

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View // View import
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.minyook.planify_app.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

// Places SDK를 위한 임포트
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.android.libraries.places.api.net.PlacesClient

// BottomSheetBehavior 임포트
import com.google.android.material.bottomsheet.BottomSheetBehavior
import androidx.constraintlayout.widget.ConstraintLayout // bottom_sheet_layout의 타입이 ConstraintLayout이므로 임포트

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient

    // BottomSheetBehavior 인스턴스 선언
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>

    // 위치 권한 요청을 위한 ActivityResultLauncher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MapsActivity", "위치 권한이 부여되었습니다.")
            getDeviceLocation()
        } else {
            Log.d("MapsActivity", "위치 권한이 거부되었습니다.")
            Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Places API 클라이언트 초기화: AndroidManifest.xml에서 API 키 가져오기
        val apiKey = applicationContext.packageManager.getApplicationInfo(
            applicationContext.packageName,
            PackageManager.GET_META_DATA
        ).metaData["com.google.android.geo.API_KEY"] as String
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, apiKey)
        }
        placesClient = Places.createClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // BottomSheetBehavior 초기화
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheetLayout)
        // 기본 상태를 STATE_COLLAPSED로 설정 (peekHeight만큼 올라온 상태)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        // 필요하다면 바텀 시트 상태 변화 리스너 추가 (선택 사항)
        // bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
        //     override fun onStateChanged(bottomSheet: View, newState: Int) {
        //         when (newState) {
        //             BottomSheetBehavior.STATE_EXPANDED -> Log.d("BottomSheet", "확장됨")
        //             BottomSheetBehavior.STATE_COLLAPSED -> Log.d("BottomSheet", "접힘")
        //             BottomSheetBehavior.STATE_DRAGGING -> Log.d("BottomSheet", "드래그 중")
        //             BottomSheetBehavior.STATE_SETTLING -> Log.d("BottomSheet", "정착 중")
        //             BottomSheetBehavior.STATE_HIDDEN -> Log.d("BottomSheet", "숨겨짐")
        //             BottomSheetBehavior.STATE_HALF_EXPANDED -> Log.d("BottomSheet", "절반 확장됨")
        //         }
        //     }
        //     override fun onSlide(bottomSheet: View, slideOffset: Float) {
        //         // 슬라이드 진행률에 따른 동작 (예: 알파 값 변경)
        //     }
        // })


        // 검색 아이콘 클릭 리스너
        binding.searchIconImageView.setOnClickListener {
            performSearch()
        }

        // 검색 EditText 엔터키 리스너
        binding.searchEditText.setOnEditorActionListener { v: TextView?, actionId: Int, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true // 이벤트 소비
            } else {
                false // 이벤트 처리 안함
            }
        }

        // 체크인/체크아웃 EditText 클릭 리스너 설정 (캘린더 기능 유지)
        binding.checkInEditText.setOnClickListener {
            showDatePickerDialog(binding.checkInEditText)
        }

        binding.checkOutEditText.setOnClickListener {
            showDatePickerDialog(binding.checkOutEditText)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        checkLocationPermission()

        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.d("MapsActivity", "위치 권한이 이미 있습니다.")
                getDeviceLocation()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Log.d("MapsActivity", "위치 권한 설명 필요.")
                Toast.makeText(this, "이 앱은 현재 위치를 표시하기 위해 위치 권한이 필요합니다.", Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            else -> {
                Log.d("MapsActivity", "위치 권한 요청.")
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun getDeviceLocation() {
        try {
            mMap.isMyLocationEnabled = true
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val currentLocation = LatLng(location.latitude, location.longitude)
                        Log.d("MapsActivity", "현재 위치: ${currentLocation.latitude}, ${currentLocation.longitude}")
                        mMap.addMarker(MarkerOptions().position(currentLocation).title("현재 위치"))
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f))
                    } else {
                        Log.w("MapsActivity", "현재 위치를 찾을 수 없습니다.")
                        Toast.makeText(this, "현재 위치를 찾을 수 없습니다.", Toast.LENGTH_LONG).show()
                        val defaultLocation = LatLng(37.5665, 126.9780) // 서울 시청 기본값
                        mMap.addMarker(MarkerOptions().position(defaultLocation).title("기본 위치 (서울)"))
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f))
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MapsActivity", "위치 가져오기 실패: ${e.message}")
                    Toast.makeText(this, "위치 가져오기 실패: ${e.message}", Toast.LENGTH_LONG).show()
                    val defaultLocation = LatLng(37.5665, 126.9780) // 서울 시청 기본값
                    mMap.addMarker(MarkerOptions().position(defaultLocation).title("기본 위치 (서울)"))
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f))
                }
        } catch (e: SecurityException) {
            Log.e("MapsActivity", "위치 권한 없음: ${e.message}")
            Toast.makeText(this, "위치 권한이 없습니다. 앱 설정에서 권한을 허용해주세요.", Toast.LENGTH_LONG).show()
        }
    }

    // 날짜 선택 다이얼로그 표시 함수
    private fun showDatePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(selectedYear, selectedMonth, selectedDayOfMonth)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                editText.setText(dateFormat.format(selectedCalendar.time))
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    // 검색 실행 함수: 지역을 검색하고 지도를 이동시킵니다.
    private fun performSearch() {
        val query = binding.searchEditText.text.toString().trim()
        if (query.isEmpty()) {
            Toast.makeText(this, "검색어를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        // 키보드 숨기기
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)

        // FindAutocompletePredictionsRequest를 사용하여 장소 예측 요청
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .setCountries("KR") // 한국 내에서만 검색하도록 제한 (선택 사항)
            .build()

        placesClient.findAutocompletePredictions(request).addOnSuccessListener { response: FindAutocompletePredictionsResponse ->
            if (response.autocompletePredictions.isNotEmpty()) {
                val firstPrediction = response.autocompletePredictions[0] // 첫 번째 예측 결과 사용
                val placeId = firstPrediction.placeId

                // FetchPlaceRequest를 사용하여 장소의 상세 정보 (좌표 포함) 가져오기
                // 장소 이름(NAME), 좌표(LAT_LNG), 주소(ADDRESS) 필드만 요청
                val placeFields = listOf(Place.Field.LAT_LNG, Place.Field.NAME, Place.Field.ADDRESS)
                val fetchPlaceRequest = FetchPlaceRequest.builder(placeId, placeFields).build()

                placesClient.fetchPlace(fetchPlaceRequest).addOnSuccessListener { fetchResponse: FetchPlaceResponse ->
                    val place = fetchResponse.place
                    place.latLng?.let { latLng ->
                        mMap.clear() // 기존 마커 지우기
                        mMap.addMarker(MarkerOptions().position(latLng).title(place.name))
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                        Toast.makeText(this, "${place.name}으로 이동했습니다.", Toast.LENGTH_SHORT).show()

                        // TODO: 이곳에서 주변 관광지 검색 로직 추가 (아래 주석 참고)
                        // searchNearbyTouristAttractions(latLng)

                    } ?: run {
                        Toast.makeText(this, "장소의 좌표를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener { exception: Exception ->
                    Log.e("MapsActivity", "장소 정보 가져오기 실패: ${exception.message}", exception)
                    Toast.makeText(this, "장소 정보 가져오기 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { exception: Exception ->
            Log.e("MapsActivity", "장소 예측 실패: ${exception.message}", exception)
            Toast.makeText(this, "장소 검색 중 오류 발생: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // TODO: 주변 관광지 검색 함수 (나중에 구현할 때 이 주석을 제거하고 구현하세요)
    // 이 함수는 Places API의 Nearby Search 또는 Text Search를 사용하여 구현될 수 있습니다.
    /*
    private fun searchNearbyTouristAttractions(location: LatLng) {
        // 예시: Text Search를 사용하여 주변 관광지 검색 (더 정교한 필터링 가능)
        val queryForAttractions = "관광지"
        val fields = listOf(Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)

        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(queryForAttractions)
            .setLocationBias(RectangularBounds.newInstance(
                LatLng(location.latitude - 0.05, location.longitude - 0.05), // 대략 5km 반경
                LatLng(location.latitude + 0.05, location.longitude + 0.05)
            ))
            .setTypes(listOf(Place.Type.TOURIST_ATTRACTION, Place.Type.POINT_OF_INTEREST, Place.Type.MUSEUM))
            .build()

        placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
            for (prediction in response.autocompletePredictions) {
                val fetchPlaceRequest = FetchPlaceRequest.builder(prediction.placeId, fields).build()
                placesClient.fetchPlace(fetchPlaceRequest).addOnSuccessListener { fetchResponse ->
                    val place = fetchResponse.place
                    place.latLng?.let { attractionLatLng ->
                        mMap.addMarker(MarkerOptions().position(attractionLatLng).title(place.name + " (관광지)"))
                        Log.d("MapsActivity", "주변 관광지 발견: ${place.name}")
                        // binding.selectedPlacesEditText.append("${place.name} x\n") // UI 업데이트 예시
                    }
                }.addOnFailureListener {
                    Log.e("MapsActivity", "개별 관광지 정보 가져오기 실패: ${it.message}")
                }
            }
        }.addOnFailureListener {
            Log.e("MapsActivity", "주변 관광지 검색 예측 실패: ${it.message}")
        }
    }
    */
}