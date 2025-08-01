package com.minyook.planify_app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth // Firebase Auth import 추가

class MypageActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth // Firebase Auth 인스턴스 선언

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mypage) // activity_mypage.xml 레이아웃 사용

        auth = FirebaseAuth.getInstance() // Firebase Auth 초기화

        val userInfoTextView: TextView = findViewById(R.id.user_info_text_view)
        val logoutButton: Button = findViewById(R.id.logout_from_mypage_button)

        // 현재 로그인된 사용자 정보 표시
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // 사용자 이름 또는 이메일 표시. displayName이 없을 경우 이메일 사용.
            userInfoTextView.text = "환영합니다, ${currentUser.displayName ?: currentUser.email} 님!"
        } else {
            // 로그인되지 않은 경우 (이 경우는 MyPageActivity에 직접 접근했을 때 발생 가능)
            userInfoTextView.text = "로그인되지 않았습니다."
            // 로그인 화면으로 강제 이동하거나 MyPageActivity 종료
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return // 이후 코드 실행 방지
        }

        // 마이페이지에서 로그아웃 버튼 클릭 리스너
        logoutButton.setOnClickListener {
            auth.signOut() // Firebase에서 로그아웃을 수행합니다.

            // 로그아웃 성공 후 사용자에게 알리고 메인 화면으로 이동
            Toast.makeText(this, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show()

            // 메인 화면으로 돌아가기 위해 모든 이전 액티비티를 클리어하고 새 태스크로 MainActivity 시작
            // 이렇게 하면 MainActivity가 새로 시작되면서 onResume()이 호출되고
            // updateDrawerHeaderBasedOnLoginStatus()에서 로그인 상태를 다시 확인하게 됩니다.
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish() // MyPageActivity 종료
        }
    }
}