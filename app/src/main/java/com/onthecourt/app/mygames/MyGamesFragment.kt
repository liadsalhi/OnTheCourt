package com.onthecourt.app.mygames

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.onthecourt.app.databinding.FragmentMyGamesBinding
import com.onthecourt.app.game.adapter.GameAdapter
import com.onthecourt.app.model.Game
import com.onthecourt.app.model.hasPassed

class MyGamesFragment : Fragment() {

    private var _binding: FragmentMyGamesBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val activeAdapter = GameAdapter { game -> openGame(game) }
    private val pastAdapter = GameAdapter { game -> openGame(game) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyGamesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvActiveGames.layoutManager = LinearLayoutManager(requireContext())
        binding.rvActiveGames.adapter = activeAdapter

        binding.rvPastGames.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPastGames.adapter = pastAdapter

        loadGames()
    }

    private fun loadGames() {
        val uid = auth.currentUser?.uid ?: return
        binding.progressBar.visibility = View.VISIBLE

        db.collection("games")
            .whereArrayContains("players", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (_binding == null) return@addOnSuccessListener
                binding.progressBar.visibility = View.GONE
                val allGames = snapshot.documents.mapNotNull { it.toObject(Game::class.java) }
                    .sortedByDescending { it.createdAt }

                val now = System.currentTimeMillis()
                val active = allGames.filter { !it.hasPassed(now) }
                val past = allGames.filter { it.hasPassed(now) }

                activeAdapter.submitList(active)
                binding.tvNoActive.visibility = if (active.isEmpty()) View.VISIBLE else View.GONE
                binding.rvActiveGames.visibility = if (active.isEmpty()) View.GONE else View.VISIBLE

                pastAdapter.submitList(past)
                binding.tvNoPast.visibility = if (past.isEmpty()) View.VISIBLE else View.GONE
                binding.rvPastGames.visibility = if (past.isEmpty()) View.GONE else View.VISIBLE
            }
            .addOnFailureListener {
                if (_binding != null) binding.progressBar.visibility = View.GONE
            }
    }

    private fun openGame(game: Game) {
        val action = MyGamesFragmentDirections.actionMyGamesToGameDetail(
            gameId = game.gameId,
            isCreator = game.creatorUid == auth.currentUser?.uid
        )
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}