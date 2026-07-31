package com.onthecourt.app.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.onthecourt.app.databinding.FragmentCourtDetailBinding
import com.onthecourt.app.game.adapter.GameAdapter
import com.onthecourt.app.model.Game
import com.onthecourt.app.model.hasPassed
import com.onthecourt.app.util.SportUtil

class CourtDetailFragment : Fragment() {

    private var _binding: FragmentCourtDetailBinding? = null
    private val binding get() = _binding!!
    private val args: CourtDetailFragmentArgs by navArgs()
    private val db = FirebaseFirestore.getInstance()
    private val gameAdapter = GameAdapter { game -> openGame(game) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCourtDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvCourtName.text = args.courtName
        binding.tvSport.text = SportUtil.displayName(requireContext(), args.sport)

        binding.rvGames.layoutManager = LinearLayoutManager(requireContext())
        binding.rvGames.adapter = gameAdapter

        binding.btnCreateGame.setOnClickListener {
            val action = CourtDetailFragmentDirections.actionCourtDetailToCreateGame(
                courtName = args.courtName,
                sport = args.sport,
                city = args.city
            )
            findNavController().navigate(action)
        }

        loadGames()
    }

    private fun loadGames() {
        binding.progressBar.visibility = View.VISIBLE
        // Matched by court name + city (not coordinates) so a game shows up for
        // every user who opens the same court, even if their own search returned
        // slightly different GPS coordinates for it.
        db.collection("games")
            .whereEqualTo("courtName", args.courtName)
            .whereEqualTo("city", args.city)
            .whereEqualTo("sport", args.sport)
            .whereEqualTo("open", true)
            .get()
            .addOnSuccessListener { snapshot ->
                if (_binding == null) return@addOnSuccessListener
                binding.progressBar.visibility = View.GONE
                val games = snapshot.documents.mapNotNull { it.toObject(Game::class.java) }
                    .filterNot { it.hasPassed() }
                gameAdapter.submitList(games)
                binding.tvNoGames.visibility = if (games.isEmpty()) View.VISIBLE else View.GONE
                binding.rvGames.visibility = if (games.isEmpty()) View.GONE else View.VISIBLE
            }
            .addOnFailureListener {
                if (_binding != null) binding.progressBar.visibility = View.GONE
            }
    }

    private fun openGame(game: Game) {
        val action = CourtDetailFragmentDirections.actionCourtDetailToGameDetail(
            gameId = game.gameId,
            isCreator = false
        )
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}