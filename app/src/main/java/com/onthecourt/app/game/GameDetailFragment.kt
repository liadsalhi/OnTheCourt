package com.onthecourt.app.game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.onthecourt.app.R
import com.onthecourt.app.databinding.FragmentGameDetailBinding
import com.onthecourt.app.game.adapter.PlayerAdapter
import com.onthecourt.app.model.Game
import com.onthecourt.app.model.User
import com.onthecourt.app.model.hasPassed
import com.onthecourt.app.model.fullName
import com.onthecourt.app.util.FriendActions
import com.onthecourt.app.util.GameActions
import com.onthecourt.app.util.SportUtil
import com.onthecourt.app.util.UserFetcher

class GameDetailFragment : Fragment() {

    private var _binding: FragmentGameDetailBinding? = null
    private val binding get() = _binding!!
    private val args: GameDetailFragmentArgs by navArgs()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var gameListener: ListenerRegistration? = null
    private var currentGame: Game? = null
    private var currentUser: User? = null
    private lateinit var playerAdapter: PlayerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGameDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playerAdapter = PlayerAdapter(
            currentUid = auth.currentUser?.uid ?: "",
            onAddFriend = { uid -> sendFriendRequest(uid) }
        )

        binding.rvPlayers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPlayers.adapter = playerAdapter

        loadCurrentUser()
        listenToGame()
    }

    private fun loadCurrentUser() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (_binding == null) return@addOnSuccessListener
                currentUser = doc.toObject(User::class.java)
                updateActionButton()
                // The player list's "friend" badges depend on currentUser.friends. The game
                // listener below can fire before this one-time load finishes, in which case
                // it renders with no friend info — refresh it here once currentUser is ready.
                currentGame?.let { loadPlayers(it.players) }
            }
    }

    private fun listenToGame() {
        binding.progressBar.visibility = View.VISIBLE
        gameListener = db.collection("games").document(args.gameId)
            .addSnapshotListener { snapshot, error ->
                binding.progressBar.visibility = View.GONE
                if (error != null || snapshot == null) return@addSnapshotListener

                val game = snapshot.toObject(Game::class.java) ?: return@addSnapshotListener
                currentGame = game
                displayGame(game)
                loadPlayers(game.players)
                updateActionButton()
            }
    }

    private fun displayGame(game: Game) {
        binding.tvSport.text = "${SportUtil.emoji(game.sport)} ${SportUtil.displayName(requireContext(), game.sport)}"
        binding.tvCourtName.text = game.courtName
        binding.tvDate.text = game.date
        binding.tvTime.text = game.time
        binding.tvTeams.text = getString(R.string.teams_count, game.numTeams)
        binding.tvDuration.text = getString(R.string.duration_minutes, game.matchDurationMinutes)
        binding.tvPlayerCount.text = getString(R.string.players_count, game.players.size, game.maxPlayers)

        if (game.description.isNotEmpty()) {
            binding.tvDescription.text = game.description
            binding.tvDescription.visibility = View.VISIBLE
        } else {
            binding.tvDescription.visibility = View.GONE
        }
    }

    private fun loadPlayers(playerUids: List<String>) {
        if (playerUids.isEmpty()) {
            playerAdapter.submitList(emptyList())
            return
        }
        val friendUids = currentUser?.friends ?: emptyList()

        UserFetcher.fetchByIds(db, playerUids) { players ->
            if (_binding == null) return@fetchByIds
            val currentUid = auth.currentUser?.uid ?: ""
            val items = playerUids.mapNotNull { uid ->
                players.find { it.uid == uid }
            }.map { user ->
                PlayerAdapter.PlayerItem(
                    uid = user.uid,
                    firstName = user.firstName,
                    lastName = user.lastName,
                    isCurrentUser = user.uid == currentUid,
                    isFriend = friendUids.contains(user.uid)
                )
            }
            playerAdapter.submitList(items)
        }
    }

    // Decides what the single action button at the bottom of the screen shows/does.
    // Checked in this order: the creator can always close the game (until it starts);
    // otherwise a player who already joined has nothing to do; otherwise anyone can join
    // while it's open and hasn't started yet; anything else shows a disabled reason.
    private fun updateActionButton() {
        val game = currentGame ?: return
        val uid = auth.currentUser?.uid ?: return
        val hasStarted = game.hasPassed()

        when {
            args.isCreator || game.creatorUid == uid -> {
                if (hasStarted) {
                    setActionButtonVisible(false)
                } else {
                    setActionButtonVisible(true)
                    binding.btnAction.isEnabled = true
                    binding.btnAction.text = getString(R.string.btn_close_game)
                    binding.btnAction.setOnClickListener { closeGame() }
                }
            }
            game.players.contains(uid) -> {
                setActionButtonVisible(false)
            }
            game.isOpen && !hasStarted -> {
                setActionButtonVisible(true)
                binding.btnAction.isEnabled = true
                binding.btnAction.text = getString(R.string.btn_join_game)
                binding.btnAction.setOnClickListener { joinGame() }
            }
            else -> {
                setActionButtonVisible(true)
                binding.btnAction.isEnabled = false
                binding.btnAction.setOnClickListener(null)
                binding.btnAction.text = if (hasStarted)
                    getString(R.string.msg_game_already_started)
                else
                    getString(R.string.msg_registration_closed)
            }
        }
    }

    // The scroll area's bottom margin only needs to reserve space for btn_action while
    // it's actually shown — otherwise the reserved space is wasted scrollable room.
    private fun setActionButtonVisible(visible: Boolean) {
        binding.btnAction.visibility = if (visible) View.VISIBLE else View.GONE
        val params = binding.scrollContent.layoutParams as ViewGroup.MarginLayoutParams
        params.bottomMargin = if (visible) {
            resources.getDimensionPixelSize(R.dimen.action_button_reserved_space)
        } else 0
        binding.scrollContent.layoutParams = params
    }

    private fun joinGame() {
        val game = currentGame ?: return
        val uid = auth.currentUser?.uid ?: return
        binding.btnAction.isEnabled = false
        val myName = currentUser?.fullName ?: ""

        GameActions.addPlayer(
            db = db,
            game = game,
            uid = uid,
            name = myName,
            onSuccess = {
                Toast.makeText(requireContext(), getString(R.string.joined_game), Toast.LENGTH_SHORT).show()
            },
            onFailure = { e ->
                if (_binding != null) binding.btnAction.isEnabled = true
                Toast.makeText(requireContext(), getString(R.string.msg_join_failed, e.message), Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun closeGame() {
        db.collection("games").document(args.gameId)
            .update("open", false)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), getString(R.string.msg_game_closed), Toast.LENGTH_SHORT).show()
                binding.btnAction.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), getString(R.string.msg_close_failed, e.message), Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendFriendRequest(targetUid: String) {
        val myUid = auth.currentUser?.uid ?: return
        FriendActions.sendRequest(
            db = db,
            fromUid = myUid,
            targetUid = targetUid,
            onAlreadyFriends = {
                Toast.makeText(requireContext(), getString(R.string.msg_already_friends), Toast.LENGTH_SHORT).show()
            },
            onAlreadySent = {
                Toast.makeText(requireContext(), getString(R.string.msg_request_already_sent), Toast.LENGTH_SHORT).show()
            },
            onSuccess = {
                Toast.makeText(requireContext(), getString(R.string.friend_request_sent), Toast.LENGTH_SHORT).show()
            },
            onFailure = { e ->
                Toast.makeText(requireContext(), getString(R.string.msg_friend_error, e.message), Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        gameListener?.remove()
        _binding = null
    }
}