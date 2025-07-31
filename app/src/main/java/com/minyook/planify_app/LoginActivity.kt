package com.minyook.planify_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.minyook.planify_app.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Firebase
import com.google.firebase.auth.auth // Firebase.auth 확장 함수를 사용하기 위해 필요

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        // Google Sign-In Options 설정 (웹 클라이언트 ID 포함)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // string.xml에서 ID를 가져옴
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // ActivityResultLauncher 초기화 (Google 로그인용)
        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    account?.idToken?.let { firebaseAuthWithGoogle(it) }
                } catch (e: ApiException) {
                    Log.e("GoogleSignIn", "Google 로그인 실패: statusCode=${e.statusCode}, message=${e.message}")
                    Toast.makeText(this, "Google 로그인 실패: ${e.statusCode}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Google 로그인 취소", Toast.LENGTH_SHORT).show()
            }
        }

        // 로그인 버튼 클릭 리스너
        binding.buttonLogin.setOnClickListener {
            performLogin()
        }

        // 회원가입 텍스트 클릭 리스너
        binding.textViewSignup.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        // 간편 로그인 버튼 클릭 리스너 (Google 로그인)
        binding.imageViewSocialGoogle.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun performLogin() {
        val email = binding.editTextLoginId.text.toString().trim()
        val password = binding.editTextLoginPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "아이디(이메일)와 비밀번호를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Toast.makeText(this, "로그인 성공! 환영합니다, ${user?.email}", Toast.LENGTH_SHORT).show()
                    // TODO: 로그인 성공 시 다음 화면으로 이동 (예: MainActivity)
                     val intent = Intent(this, MainActivity::class.java)
                     startActivity(intent)
                     finish()
                } else {
                    Toast.makeText(this, "로그인 실패: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // Google 로그인 시작 함수
    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    // Firebase Authentication에 Google 계정으로 로그인 (ID 토큰 사용)
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Toast.makeText(this, "Google 로그인 성공! 환영합니다, ${user?.email}", Toast.LENGTH_SHORT).show()
                    // TODO: 로그인 성공 시 다음 화면으로 이동 (예: MainActivity)
                     val intent = Intent(this, MainActivity::class.java)
                     startActivity(intent)
                     finish()
                } else {
                    Toast.makeText(this, "Google 로그인 실패: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}