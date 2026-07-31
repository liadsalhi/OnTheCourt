package com.onthecourt.app.friends

import android.app.AlertDialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.onthecourt.app.R
import com.onthecourt.app.databinding.FragmentFriendsBinding
import com.onthecourt.app.friends.adapter.FriendAdapter
import com.onthecourt.app.friends.adapter.FriendRequestAdapter
import com.onthecourt.app.friends.adapter.UserSearchAdapter
import com.onthecourt.app.model.Game
import com.onthecourt.app.model.User
import com.onthecourt.app.model.fullName
import com.onthecourt.app.util.FriendActions
import com.onthecourt.app.util.GameActions
import com.onthecourt.app.util.SportUtil
import com.onthecourt.app.util.UserFetcher

class FriendsFragment : Fragment() {

    private var _binding: FragmentFriendsBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var userListener: ListenerRegistration? = null
    private var currentUser: User? = null

    private lateinit var requestAdapter: FriendRequestAdapter
    private lateinit var friendAdapter: FriendAdapter
    private lateinit var searchAdapter: UserSearchAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requestAdapter = FriendRequestAdapter(
            onAccept = { user -> acceptFriend(user) },
            onDecline = { user -> declineFriend(user) }
        )
        friendAdapter = FriendAdapter(
            onInvite = { user -> showInviteDialog(user) },
            onClick = { user -> navigateToChat(user) }
        )
        searchAdapter = UserSearchAdapter { user -> sendFriendRequestTo(user) }

        binding.rvFriendRequests.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFriendRequests.adapter = requestAdapter

        binding.rvFriends.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFriends.adapter = friendAdapter

        binding.rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSearchResults.adapter = searchAdapter

        binding.btnFriendSearch.setOnClickListener { searchUsers() }
        binding.etFriendSearch.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                searchUsers()
                true
            } else false
        }

        listenToCurrentUser()
    }

    private fun searchUsers() {
        val query = binding.etFriendSearch.text?.toString()?.trim()?.lowercase() ?: ""
        if (query.isEmpty()) return
        val myUid = auth.currentUser?.uid ?: return
        binding.progressBar.visibility = View.VISIBLE

        db.collection("users").limit(50).get()
            .addOnSuccessListener { snapshot ->
                if (_binding == null) return@addOnSuccessListener
                binding.progressBar.visibility = View.GONE
                val myFriends = currentUser?.friends ?: emptyList()
                val results = snapshot.documents.mapNotNull { it.toObject(User::class.java) }
                    .filter { it.uid != myUid && !myFriends.contains(it.uid) }
                    .filter { it.fullName.lowercase().contains(query) }

                searchAdapter.submitList(results)
                binding.rvSearchResults.visibility = if (results.isEmpty()) View.GONE else View.VISIBLE
                binding.tvNoSearchResults.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener {
                if (_binding != null) binding.progressBar.visibility = View.GONE
            }
    }

    private fun sendFriendRequestTo(target: User) {
        val myUid = auth.currentUser?.uid ?: return
        FriendActions.sendRequest(
            db = db,
            fromUid = myUid,
            targetUid = target.uid,
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

    private fun navigateToChat(friend: User) {
        val action = FriendsFragmentDirections.actionFriendsToChat(
            friendUid = friend.uid,
            friendName = friend.fullName
        )
        findNavController().navigate(action)
    }

    private fun listenToCurrentUser() {
        val uid = auth.currentUser?.uid ?: return
        binding.progressBar.visibility = View.VISIBLE

        userListener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                binding.progressBar.visibility = View.GONE
                if (error != null || snapshot == null) return@addSnapshotListener

                val user = snapshot.toObject(User::class.java) ?: return@addSnapshotListener
                currentUser = user

                loadFriendRequests(user.friendRequests)
                loadFriends(user.friends)
            }
    }

    private fun loadFriendRequests(requestUids: List<String>) {
        if (requestUids.isEmpty()) {
            requestAdapter.submitList(emptyList())
            binding.tvNoRequests.visibility = View.VISIBLE
            return
        }
        binding.tvNoRequests.visibility = View.GONE

        UserFetcher.fetchByIds(db, requestUids) { users ->
            if (_binding == null) return@fetchByIds
            requestAdapter.submitList(users)
            binding.tvNoRequests.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun loadFriends(friendUids: List<String>) {
        if (friendUids.isEmpty()) {
            friendAdapter.submitList(emptyList())
            binding.tvNoFriends.visibility = View.VISIBLE
            return
        }
        binding.tvNoFriends.visibility = View.GONE

        UserFetcher.fetchByIds(db, friendUids) { users ->
            if (_binding == null) return@fetchByIds
            friendAdapter.submitList(users)
            binding.tvNoFriends.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun acceptFriend(requester: User) {
        val myUid = auth.currentUser?.uid ?: return
        FriendActions.acceptRequest(
            db = db,
            myUid = myUid,
            requesterUid = requester.uid,
            onSuccess = {
                Toast.makeText(requireContext(), getString(R.string.msg_friend_added, requester.firstName), Toast.LENGTH_SHORT).show()
            },
            onFailure = { e ->
                Toast.makeText(requireContext(), getString(R.string.msg_friend_error, e.message), Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun declineFriend(requester: User) {
        val myUid = auth.currentUser?.uid ?: return
        FriendActions.declineRequest(
            db = db,
            myUid = myUid,
            requesterUid = requester.uid,
            onFailure = { e ->
                Toast.makeText(requireContext(), getString(R.string.msg_friend_error, e.message), Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun showInviteDialog(friend: User) {
        val myUid = auth.currentUser?.uid ?: return
        binding.progressBar.visibility = View.VISIBLE

        db.collection("games")
            .whereArrayContains("players", myUid)
            .whereEqualTo("open", true)
            .get()
            .addOnSuccessListener { snapshot ->
                if (_binding == null) return@addOnSuccessListener
                binding.progressBar.visibility = View.GONE
                val games = snapshot.documents.mapNotNull { it.toObject(Game::class.java) }
                    .filter { !it.players.contains(friend.uid) && it.players.size < it.maxPlayers }

                if (games.isEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.msg_no_active_games_invite, friend.firstName), Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val gameLabels = games.map {
                    getString(R.string.game_label_format, SportUtil.displayName(requireContext(), it.sport), it.courtName, it.time)
                }.toTypedArray()

                AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.title_invite_to_game, friend.firstName))
                    .setItems(gameLabels) { _, which ->
                        val game = games[which]
                        inviteFriendToGame(friend, game)
                    }
                    .setNegativeButton(getString(R.string.btn_cancel), null)
                    .show()
            }
            .addOnFailureListener {
                if (_binding != null) binding.progressBar.visibility = View.GONE
            }
    }

    private fun inviteFriendToGame(friend: User, game: Game) {
        GameActions.addPlayer(
            db = db,
            game = game,
            uid = friend.uid,
            name = friend.fullName,
            onSuccess = {
                Toast.makeText(requireContext(), getString(R.string.msg_friend_added_to_game, friend.firstName), Toast.LENGTH_SHORT).show()
            },
            onFailure = { e ->
                Toast.makeText(requireContext(), getString(R.string.msg_invite_failed, e.message), Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        userListener?.remove()
        _binding = null
    }
}