package com.onthecourt.app.map

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.onthecourt.app.R
import com.onthecourt.app.databinding.FragmentCourtMapBinding
import com.onthecourt.app.map.adapter.CourtAdapter
import com.onthecourt.app.util.LocationUtil
import com.onthecourt.app.util.SportUtil
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale

class CourtMapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentCourtMapBinding? = null
    private val binding get() = _binding!!
    private val args: CourtMapFragmentArgs by navArgs()

    private var googleMap: GoogleMap? = null
    private var currentCity: String = ""
    private lateinit var courtAdapter: CourtAdapter

    // Stores marker data: markerId -> Pair(courtName, latLng string)
    private val markerCourtMap = mutableMapOf<String, String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCourtMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvSportLabel.text = getString(R.string.showing_sport_courts, SportUtil.displayName(requireContext(), args.sport))

        setupCourtsList()
        loadUserCity()
        setupMap()
        setupSearchBar()

        binding.btnSearch.setOnClickListener { searchCity() }
    }

    private fun setupCourtsList() {
        courtAdapter = CourtAdapter(args.sport) { item -> navigateToCourtDetail(item.name) }
        binding.rvCourts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCourts.adapter = courtAdapter
    }

    private fun loadUserCity() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (_binding == null) return@addOnSuccessListener
                val city = doc.getString("city") ?: ""
                if (city.isNotEmpty()) {
                    currentCity = city
                    binding.etCity.setText(city)
                    searchCity()
                }
            }
    }

    private fun setupMap() {
        val mapFragment = SupportMapFragment.newInstance()
        childFragmentManager.beginTransaction()
            .replace(R.id.map_container, mapFragment)
            .commit()
        mapFragment.getMapAsync(this)
    }

    private fun setupSearchBar() {
        binding.etCity.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                searchCity()
                true
            } else false
        }
    }

    private fun searchCity() {
        val city = binding.etCity.text?.toString()?.trim() ?: ""
        if (city.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.error_enter_city), Toast.LENGTH_SHORT).show()
            return
        }
        currentCity = city
        binding.progressBar.visibility = View.VISIBLE

        // Tied to the view's lifecycle so this is cancelled automatically if the
        // user navigates away before the geocoding call finishes.
        viewLifecycleOwner.lifecycleScope.launch {
            val latLng = geocodeCity(city)
            withContext(Dispatchers.Main) {
                if (_binding == null) return@withContext
                binding.progressBar.visibility = View.GONE
                if (latLng != null) {
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM_LEVEL))
                    fetchNearbyCourts(latLng)
                } else {
                    Toast.makeText(requireContext(), getString(R.string.msg_city_not_found, city), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun geocodeCity(city: String): LatLng? {
        return try {
            @Suppress("DEPRECATION")
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val results = geocoder.getFromLocationName(city, 1)
            if (!results.isNullOrEmpty()) {
                LatLng(results[0].latitude, results[0].longitude)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchNearbyCourts(center: LatLng) {
        val apiKey = getString(R.string.serpapi_key)
        val query = "מגרש ${SportUtil.displayName(requireContext(), args.sport)} ב$currentCity"
        // hl=iw requests Hebrew results. This is the old ISO code for Hebrew -
        // SerpApi rejects the modern "he" code with a 400 error, so "iw" must
        // be used even though it looks outdated.
        val url = "https://serpapi.com/search.json" +
            "?engine=google_maps" +
            "&q=${URLEncoder.encode(query, "UTF-8")}" +
            "&ll=@${center.latitude},${center.longitude},14z" +
            "&type=search" +
            "&hl=iw" +
            "&api_key=$apiKey"

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                val response = httpClient.newCall(request).execute()
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                // Expected shape: { "local_results": [ { "title", "address",
                // "gps_coordinates": { "latitude", "longitude" } }, ... ] }
                // Entries missing coordinates are skipped since we need them to
                // match this court to games created for it later.
                val results = json.optJSONArray("local_results")

                val items = mutableListOf<CourtAdapter.CourtListItem>()
                val markers = mutableListOf<Triple<String, LatLng, String>>()

                if (results != null) {
                    for (i in 0 until results.length()) {
                        val place = results.getJSONObject(i)
                        val coords = place.optJSONObject("gps_coordinates") ?: continue
                        if (!coords.has("latitude") || !coords.has("longitude")) continue
                        val lat = coords.getDouble("latitude")
                        val lng = coords.getDouble("longitude")
                        val name = place.optString("title", getString(R.string.title_court))
                        val address = place.optString("address", "")

                        // Rounded key so the same court matches across separate
                        // searches/users even if the API returns very slightly
                        // different precision each time (see LocationUtil).
                        val latLngKey = LocationUtil.latLngKey(lat, lng)
                        items.add(CourtAdapter.CourtListItem(name, address, latLngKey))
                        markers.add(Triple(name, LatLng(lat, lng), latLngKey))
                    }
                }

                withContext(Dispatchers.Main) {
                    if (_binding == null) return@withContext
                    googleMap?.clear()
                    markerCourtMap.clear()

                    if (items.isEmpty()) {
                        courtAdapter.submitList(emptyList())
                        binding.tvNoCourts.visibility = View.VISIBLE
                        Toast.makeText(requireContext(),
                            getString(R.string.msg_no_courts_found, SportUtil.displayName(requireContext(), args.sport)),
                            Toast.LENGTH_LONG).show()
                    } else {
                        binding.tvNoCourts.visibility = View.GONE
                        courtAdapter.submitList(items)
                        markers.forEach { (name, latLng, latLngStr) ->
                            val marker = googleMap?.addMarker(
                                MarkerOptions()
                                    .position(latLng)
                                    .title(name)
                                    .snippet(getString(R.string.view_games))
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                            )
                            marker?.let { markerCourtMap[it.id] = name }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (_binding == null) return@withContext
                    addFallbackMarkers(center)
                }
            }
        }
    }

    private fun addFallbackMarkers(center: LatLng) {
        googleMap?.clear()
        markerCourtMap.clear()
        val courtName = "${SportUtil.emoji(args.sport)} " +
            getString(R.string.fallback_court_name, SportUtil.displayName(requireContext(), args.sport), currentCity)
        val latLngStr = LocationUtil.latLngKey(center.latitude, center.longitude)
        val marker = googleMap?.addMarker(
            MarkerOptions()
                .position(center)
                .title(courtName)
                .snippet(getString(R.string.view_games))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
        )
        marker?.let { markerCourtMap[it.id] = courtName }

        binding.tvNoCourts.visibility = View.GONE
        courtAdapter.submitList(listOf(CourtAdapter.CourtListItem(courtName, "", latLngStr)))
        Toast.makeText(requireContext(), getString(R.string.msg_search_unavailable), Toast.LENGTH_LONG).show()
    }

    private fun navigateToCourtDetail(courtName: String) {
        val action = CourtMapFragmentDirections.actionCourtMapToCourtDetail(
            courtName = courtName,
            sport = args.sport,
            city = currentCity
        )
        findNavController().navigate(action)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
        }

        map.setOnInfoWindowClickListener { marker ->
            val courtName = markerCourtMap[marker.id] ?: return@setOnInfoWindowClickListener
            navigateToCourtDetail(courtName)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val DEFAULT_ZOOM_LEVEL = 13f

        // Shared across fragment instances instead of creating a new client
        // (and a new connection pool) every time this screen opens.
        private val httpClient = OkHttpClient()
    }
}
