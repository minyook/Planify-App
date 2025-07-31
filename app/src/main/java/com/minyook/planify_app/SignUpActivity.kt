package com.minyook.planify_app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.minyook.planify_app.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
// import android.util.Log // 로그를 원한다면 주석 해제
// import com.google.firebase.firestore.FirebaseFirestore // Firestore를 사용한다면 주석 해제

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth

    // NOTE: 중복확인 버튼을 제거했으므로, isEmailChecked와 isEmailDuplicate 변수는 더 이상 필요 없습니다.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        // "회원가입" 버튼 클릭 리스너 설정
        binding.buttonSignup.setOnClickListener {
            performSignup()
        }

        // "로그인" 텍스트 클릭 리스너 설정 (로그인 화면으로 이동)
        binding.textViewLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        // NOTE: "중복확인" 버튼이 XML에서 제거되었으므로, 해당 클릭 리스너도 제거합니다.
        // binding.buttonCheckDuplicate.setOnClickListener {
        //     checkEmailIdDuplicate()
        // }
    }

    /**
     * 회원가입 버튼 클릭 시 호출되며, 입력된 정보를 검증하고 Firebase에 사용자 계정을 생성합니다.
     * 이메일 유효성 및 중복 검사는 Firebase createUserWithEmailAndPassword 과정에서 자동으로 처리됩니다.
     */
    private fun performSignup() {
        val name = binding.editTextSignupName.text.toString().trim()
        val email = binding.editTextSignupId.text.toString().trim() // '아이디' 필드를 '이메일'로 사용
        val password = binding.editTextSignupPassword.text.toString().trim()
        val passwordConfirm = binding.editTextSignupConfirmPassword.text.toString().trim()

        // 1. 모든 필드가 비어있지 않은지 검사
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty()) {
            Toast.makeText(this, "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. 이메일 형식 유효성 검사
        // 중복확인 버튼을 제거했으므로, 회원가입 시도 시 이 단계에서 이메일 형식을 바로 검사합니다.
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "유효하지 않은 이메일 형식입니다.", Toast.LENGTH_LONG).show()
            return
        }

        // 3. 비밀번호와 비밀번호 확인 필드가 일치하는지 검사
        if (password != passwordConfirm) {
            Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 4. 비밀번호 최소 길이 검사 (Firebase Auth 요구 사항: 6자 이상)
        if (password.length < 6) {
            Toast.makeText(this, "비밀번호는 6자 이상이어야 합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 5. Firebase Authentication을 사용하여 이메일과 비밀번호로 사용자 계정 생성 시도
        // 이 과정에서 Firebase는 자동으로 이메일의 중복 여부를 확인하며, 중복 시 오류를 반환합니다.
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Toast.makeText(this, "회원가입 성공! ${user?.email}님 환영합니다.", Toast.LENGTH_SHORT).show()

                    // TODO: (선택 사항) 사용자 이름(name)과 같은 추가 정보를 Firestore 또는 Realtime Database에 저장
                    // val db = FirebaseFirestore.getInstance()
                    // val userData = hashMapOf(
                    //     "name" to name,
                    //     "email" to email
                    // )
                    // user?.uid?.let { uid ->
                    //     db.collection("users").document(uid)
                    //         .set(userData)
                    //         .addOnSuccessListener { Log.d("SignUpActivity", "User data saved for UID: $uid") }
                    //         .addOnFailureListener { e -> Log.w("SignUpActivity", "Error saving user data", e) }
                    // }

                    // 회원가입 성공 후 LoginActivity로 이동
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish() // 현재 SignUpActivity를 스택에서 제거
                } else {
                    // 회원가입 실패 처리 (Firebase Authentication 오류)
                    val errorCode = (task.exception as? com.google.firebase.auth.FirebaseAuthException)?.errorCode
                    val errorMessage = task.exception?.message ?: "회원가입 중 알 수 없는 오류 발생"

                    // Firebase 오류 코드 중 이메일 중복에 해당하는 경우 사용자에게 특정 메시지를 표시
                    if (errorCode == "ERROR_EMAIL_ALREADY_IN_USE") {
                        Toast.makeText(this, "회원가입 실패: 이미 사용 중인 이메일입니다. 다른 이메일을 사용해주세요.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "회원가입 실패: $errorMessage", Toast.LENGTH_LONG).show()
                    }
                    // Log.e("SignUpActivity", "Signup failed. Error code: $errorCode, Message: $errorMessage", task.exception)
                }
            }
    }
}