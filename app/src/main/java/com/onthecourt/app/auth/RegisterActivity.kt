package com.onthecourt.app.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.onthecourt.app.BaseActivity
import com.onthecourt.app.MainActivity
import com.onthecourt.app.R
import com.onthecourt.app.databinding.ActivityRegisterBinding
import com.onthecourt.app.model.User
import com.onthecourt.app.util.AvatarHelper

class RegisterActivity : BaseActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAvatarPreview()

        binding.btnSignUp.setOnClickListener { attemptRegister() }
        binding.tvLogIn.setOnClickListener { finish() }
    }

    private fun setupAvatarPreview() {
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { updateAvatar() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        binding.etFirstName.addTextChangedListener(watcher)
        binding.etLastName.addTextChangedListener(watcher)
        updateAvatar()
    }

    private fun updateAvatar() {
        val first = binding.etFirstName.text?.toString() ?: ""
        val last = binding.etLastName.text?.toString() ?: ""
        val initials = AvatarHelper.getInitials(first, last)
        binding.tvAvatarInitials.text = initials

        // Use a fixed color for preview; actual color uses uid after creation
        binding.tvAvatarInitials.background.mutate().setTint(0xFF0D2B55.toInt())
    }

    private fun attemptRegister() {
        val firstName = binding.etFirstName.text?.toString()?.trim() ?: ""
        val lastName = binding.etLastName.text?.toString()?.trim() ?: ""
        val city = binding.etCity.text?.toString()?.trim() ?: ""
        val ageStr = binding.etAge.text?.toString()?.trim() ?: ""
        val email = binding.etEmail.text?.toString()?.trim() ?: ""
        val password = binding.etPassword.text?.toString() ?: ""

        var valid = true
        if (firstName.isEmpty()) { binding.tilFirstName.error = getString(R.string.error_required); valid = false } else binding.tilFirstName.error = null
        if (lastName.isEmpty()) { binding.tilLastName.error = getString(R.string.error_required); valid = false } else binding.tilLastName.error = null
        if (city.isEmpty()) { binding.tilCity.error = getString(R.string.error_required); valid = false } else binding.tilCity.error = null
        if (ageStr.isEmpty()) { binding.tilAge.error = getString(R.string.error_required); valid = false } else binding.tilAge.error = null
        when {
            email.isEmpty() -> { binding.tilEmail.error = getString(R.string.error_required); valid = false }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> { binding.tilEmail.error = getString(R.string.error_invalid_email); valid = false }
            else -> binding.tilEmail.error = null
        }
        if (password.length < 6) { binding.tilPassword.error = getString(R.string.error_min_password); valid = false } else binding.tilPassword.error = null
        if (!valid) return

        val age = ageStr.toIntOrNull() ?: 0
        binding.btnSignUp.isEnabled = false

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user!!.uid
                val user = User(
                    uid = uid,
                    firstName = firstName,
                    lastName = lastName,
                    city = city,
                    age = age,
                    email = email
                )
                db.collection("users").document(uid).set(user)
                    .addOnSuccessListener {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener { e ->
                        // The Auth account already exists at this point but has no profile
                        // document. Delete it so the user isn't stuck (can't log in properly,
                        // can't re-register with the same email either).
                        result.user!!.delete()
                        binding.btnSignUp.isEnabled = true
                        Toast.makeText(this, getString(R.string.error_save_profile_failed, e.message), Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                binding.btnSignUp.isEnabled = true
                if (e is FirebaseAuthUserCollisionException) {
                    binding.tilEmail.error = getString(R.string.error_email_in_use)
                } else {
                    Toast.makeText(this, e.message ?: getString(R.string.error_registration_failed), Toast.LENGTH_LONG).show()
                }
            }
    }
}