package com.onthecourt.app.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.onthecourt.app.R
import com.onthecourt.app.databinding.FragmentSportSelectionBinding

class SportSelectionFragment : Fragment() {

    private var _binding: FragmentSportSelectionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSportSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadUserName()
        setupSportCards()
    }

    private fun loadUserName() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (_binding == null) return@addOnSuccessListener
                val firstName = doc.getString("firstName") ?: ""
                if (firstName.isNotEmpty()) {
                    binding.tvWelcome.text = getString(R.string.welcome_user, firstName)
                }
            }
    }

    private fun setupSportCards() {
        binding.cardSoccer.setOnClickListener { navigateToMap("soccer") }
        binding.cardBasketball.setOnClickListener { navigateToMap("basketball") }
        binding.cardTennis.setOnClickListener { navigateToMap("tennis") }
    }

    private fun navigateToMap(sport: String) {
        val action = SportSelectionFragmentDirections
            .actionSportSelectionToCourtMap(sport)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}