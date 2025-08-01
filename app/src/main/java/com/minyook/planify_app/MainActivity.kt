package com.minyook.planify_app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.minyook.planify_app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    // 네비게이션 드로어 헤더의 뷰들을 전역 변수로 선언하여 onCreate 밖에서도 접근 가능하도록 합니다.
    private var loginSection: LinearLayout? = null
    private var userInfoSection: LinearLayout? = null
    private var userNameTextView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance() // Firebase Auth 초기화

        // 1. 검색창 기능 구현
        binding.searchIconInBar.setOnClickListener {
            performSearch()
        }
        binding.searchEditText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }

        // 2. 메뉴바 기능 구현
        binding.menuIconImageView.setOnClickListener {
            updateDrawerHeaderBasedOnLoginStatus()
            binding.drawerLayout.openDrawer(GravityCompat.END)
        }

        // 네비게이션 드로어 헤더 및 메뉴 항목 클릭 리스너 설정
        val navigationView = binding.navigationView
        navigationView.itemIconTintList = null // 아이콘 색조 비활성화

        // 헤더 뷰 참조를 onCreate에서 한번만 수행하고 전역 변수에 할당합니다.
        val headerView = navigationView.getHeaderView(0)
        loginSection = headerView.findViewById(R.id.nav_header_login_section)
        userInfoSection = headerView.findViewById(R.id.nav_header_user_info_section)
        userNameTextView = headerView.findViewById(R.id.user_name_text_view)

        // 로그인 섹션 클릭 리스너 (로그인 Activity로 이동)
        loginSection?.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            binding.drawerLayout.closeDrawer(GravityCompat.END)
        }

        // 사용자 정보 섹션 클릭 리스너 (마이페이지로 이동)
        userInfoSection?.setOnClickListener {
            // 마이페이지 이동 전 로그인 상태 확인
            if (isUserLoggedIn()) {
                Toast.makeText(this, "마이페이지로 이동합니다.", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MypageActivity::class.java)
                startActivity(intent)
            } else {
                showLoginRequiredMessage()
            }
            binding.drawerLayout.closeDrawer(GravityCompat.END)
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_view_planner -> {
                    // 플래너 보러가기 전 로그인 상태 확인
                    if (isUserLoggedIn()) {
                        Toast.makeText(this, "플래너 보러가기", Toast.LENGTH_SHORT).show()
                        // TODO: PlannerListActivity 등으로 이동
                        // 예시: val intent = Intent(this, PlannerListActivity::class.java)
                        //       startActivity(intent)
                    } else {
                        showLoginRequiredMessage()
                    }
                }
                R.id.nav_create_planner -> {
                    // 플래너 만들러가기 전 로그인 상태 확인
                    if (isUserLoggedIn()) {
                        Toast.makeText(this, "플래너 만들러가기", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MapsActivity::class.java) // 테스트용
                        startActivity(intent)
                    } else {
                        showLoginRequiredMessage()
                    }
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            true
        }

        // 나의 플래너 & AI 플래너 카드 클릭 리스너
        binding.MyPlannerArea.setOnClickListener {
            // 나의 플래너 화면 이동 전 로그인 상태 확인
            if (isUserLoggedIn()) {
                Toast.makeText(this, "나의 플래너 화면으로 이동합니다.", Toast.LENGTH_SHORT).show()
                // TODO: 실제 나의 플래너 화면으로 이동하는 Intent를 여기에 구현하세요.
                // val intent = Intent(this, MyPlannerActivity::class.java)
                //     startActivity(intent)
            } else {
                showLoginRequiredMessage()
            }
        }

        binding.AIPlannerArea.setOnClickListener {
            // AI 플래너 화면 이동 전 로그인 상태 확인
            if (isUserLoggedIn()) {
                Toast.makeText(this, "AI 플래너 화면으로 이동합니다.", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MapsActivity::class.java) // 테스트용
                startActivity(intent)
            } else {
                showLoginRequiredMessage()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateDrawerHeaderBasedOnLoginStatus()
    }

    private fun updateDrawerHeaderBasedOnLoginStatus() {
        val currentUser = auth.currentUser

        if (loginSection != null && userInfoSection != null && userNameTextView != null) {
            if (currentUser != null) {
                loginSection?.visibility = View.GONE
                userInfoSection?.visibility = View.VISIBLE
                userNameTextView?.text = currentUser.displayName ?: currentUser.email ?: "사용자"
            } else {
                loginSection?.visibility = View.VISIBLE
                userInfoSection?.visibility = View.GONE
            }
        }
    }

    // 사용자 로그인 상태를 확인하는 도우미 함수
    private fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    // 로그인 필요 메시지를 보여주고 로그인 액티비티로 이동하는 함수
    private fun showLoginRequiredMessage() {
        Toast.makeText(this, "로그인을 먼저 해주세요.", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    private fun performSearch() {
        val searchQuery = binding.searchEditText.text.toString().trim()
        if (searchQuery.isNotEmpty()) {
            // 검색 실행 전 로그인 상태 확인
            if (isUserLoggedIn()) {
                val intent = Intent(this, MapsActivity::class.java).apply {
                    putExtra("SEARCH_QUERY", searchQuery)
                }
                startActivity(intent)
            } else {
                showLoginRequiredMessage()
            }
        } else {
            Toast.makeText(this, "검색어를 입력해주세요.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }
}