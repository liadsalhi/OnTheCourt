package com.onthecourt.app.game

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import java.util.Calendar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.onthecourt.app.R
import com.onthecourt.app.databinding.FragmentCreateGameBinding
import com.onthecourt.app.model.Game
import com.onthecourt.app.model.User
import com.onthecourt.app.model.fullName
import com.onthecourt.app.util.SportUtil
import com.onthecourt.app.util.UserFetcher

class CreateGameFragment : Fragment() {

    private var _binding: FragmentCreateGameBinding? = null
    private val binding get() = _binding!!
    private val args: CreateGameFragmentArgs by navArgs()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val selectedFriendUids = mutableListOf<String>()
    private var friendsList: List<User> = emptyList()
    private var currentUser: User? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvCourtInfo.text = getString(
            R.string.court_info_format,
            SportUtil.displayName(requireContext(), args.sport),
            args.courtName
        ) + "\n${args.city}"

        loadFriends()

        binding.btnInviteFriends.setOnClickListener { showFriendPicker() }
        binding.btnCreate.setOnClickListener { createGame() }
    }

    private fun loadFriends() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (_binding == null) return@addOnSuccessListener
                currentUser = doc.toObject(User::class.java)
                val friendUids = doc.get("friends") as? List<String> ?: emptyList()
                UserFetcher.fetchByIds(db, friendUids) { users ->
                    friendsList = users
                }
            }
    }

    private fun showFriendPicker() {
        if (friendsList.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.msg_no_friends_invite), Toast.LENGTH_SHORT).show()
            return
        }
        val names = friendsList.map { it.fullName }.toTypedArray()
        val checked = BooleanArray(friendsList.size) { i -> selectedFriendUids.contains(friendsList[i].uid) }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.btn_invite_friends))
            .setMultiChoiceItems(names, checked) { _, which, isChecked ->
                val uid = friendsList[which].uid
                if (isChecked) selectedFriendUids.add(uid) else selectedFriendUids.remove(uid)
            }
            .setPositiveButton(getString(R.string.btn_done)) { _, _ ->
                if (selectedFriendUids.isNotEmpty()) {
                    binding.btnInviteFriends.text = getString(R.string.invite_friends_selected, selectedFriendUids.size)
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun createGame() {
        val dayStr = binding.etDay.text?.toString()?.trim() ?: ""
        val monthStr = binding.etMonth.text?.toString()?.trim() ?: ""
        val yearStr = binding.etYear.text?.toString()?.trim() ?: ""
        val time = binding.etTime.text?.toString()?.trim() ?: ""
        val maxPlayersStr = binding.etMaxPlayers.text?.toString()?.trim() ?: ""
        val numTeamsStr = binding.etNumTeams.text?.toString()?.trim() ?: ""
        val durationStr = binding.etDuration.text?.toString()?.trim() ?: ""
        val description = binding.etDescription.text?.toString()?.trim() ?: ""

        var valid = true
        val day = dayStr.toIntOrNull()
        val month = monthStr.toIntOrNull()
        val year = yearStr.toIntOrNull()
        when {
            dayStr.isEmpty() || monthStr.isEmpty() || yearStr.isEmpty() -> {
                setDateError(getString(R.string.error_required)); valid = false
            }
            day == null || month == null || year == null || !isValidDate(day, month, year) -> {
                setDateError(getString(R.string.error_invalid_date)); valid = false
            }
            else -> setDateError(null)
        }
        when {
            time.isEmpty() -> { binding.tilTime.error = getString(R.string.error_required); valid = false }
            !isValidTime(time) -> { binding.tilTime.error = getString(R.string.error_invalid_time); valid = false }
            else -> binding.tilTime.error = null
        }
        if (maxPlayersStr.isEmpty()) { binding.tilMaxPlayers.error = getString(R.string.error_required); valid = false } else binding.tilMaxPlayers.error = null
        if (numTeamsStr.isEmpty()) { binding.tilNumTeams.error = getString(R.string.error_required); valid = false } else binding.tilNumTeams.error = null
        if (durationStr.isEmpty()) { binding.tilDuration.error = getString(R.string.error_required); valid = false } else binding.tilDuration.error = null
        if (!valid) return
        val dateStr = String.format("%02d/%02d/%04d", day!!, month!!, year!!)

        val uid = auth.currentUser?.uid ?: return
        val maxPlayers = maxPlayersStr.toIntOrNull() ?: 0
        val numTeams = numTeamsStr.toIntOrNull() ?: 0
        val duration = durationStr.toIntOrNull() ?: 0

        binding.btnCreate.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        val gameRef = db.collection("games").document()
        val creatorName = currentUser?.fullName ?: ""

        // Invited friends join as players immediately (no separate invite/accept flow exists
        // yet), capped at maxPlayers since the creator's own slot is already counted.
        val invitedPlayers = friendsList
            .filter { selectedFriendUids.contains(it.uid) }
            .take((maxPlayers - 1).coerceAtLeast(0))
        val allPlayerUids = listOf(uid) + invitedPlayers.map { it.uid }
        val allPlayerNames = mapOf(uid to creatorName) + invitedPlayers.associate { it.uid to it.fullName }

        val game = Game(
            gameId = gameRef.id,
            creatorUid = uid,
            sport = args.sport,
            city = args.city,
            courtName = args.courtName,
            date = dateStr,
            time = time,
            maxPlayers = maxPlayers,
            numTeams = numTeams,
            matchDurationMinutes = duration,
            description = description,
            players = allPlayerUids,
            playerNames = allPlayerNames,
            isOpen = allPlayerUids.size < maxPlayers,
            startTimestamp = startTimestampFor(day, month, year, time),
            createdAt = System.currentTimeMillis()
        )

        gameRef.set(game)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                if (invitedPlayers.isNotEmpty()) {
                    Toast.makeText(requireContext(),
                        getString(R.string.msg_game_created_invites, invitedPlayers.size),
                        Toast.LENGTH_SHORT).show()
                }
                val action = CreateGameFragmentDirections.actionCreateGameToGameDetail(
                    gameId = gameRef.id,
                    isCreator = true
                )
                findNavController().navigate(action)
            }
            .addOnFailureListener { e ->
                binding.btnCreate.isEnabled = true
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), getString(R.string.msg_create_game_failed, e.message), Toast.LENGTH_LONG).show()
            }
    }

    private fun isValidTime(time: String): Boolean {
        return Regex("^([01]?[0-9]|2[0-3]):([0-5][0-9])$").matches(time)
    }

    // Uses a strict (non-lenient) Calendar so an out-of-range combination like
    // Feb 30 is rejected instead of silently rolling over into March.
    private fun isValidDate(day: Int, month: Int, year: Int): Boolean {
        return try {
            Calendar.getInstance().apply {
                isLenient = false
                set(year, month - 1, day, 0, 0, 0)
            }.time
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    private fun setDateError(message: String?) {
        binding.tilDay.error = message
        binding.tilMonth.error = message
        binding.tilYear.error = message
    }

    private fun startTimestampFor(day: Int, month: Int, year: Int, time: String): Long {
        val (hour, minute) = time.split(":").map { it.toInt() }
        return Calendar.getInstance().apply {
            set(year, month - 1, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}