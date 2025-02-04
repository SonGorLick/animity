package com.kl3jvi.animity.ui.fragments.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.kl3jvi.animity.databinding.FragmentFavoritesBinding
import com.kl3jvi.animity.favoriteAnime
import com.kl3jvi.animity.ui.activities.main.MainActivity
import com.kl3jvi.animity.utils.Constants.Companion.showSnack
import com.kl3jvi.animity.utils.NetworkUtils.isConnectedToInternet
import com.kl3jvi.animity.utils.collectFlow
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@AndroidEntryPoint
class FavoritesFragment : Fragment() {

    val viewModel: FavoritesViewModel by viewModels()
    private lateinit var binding: FragmentFavoritesBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentFavoritesBinding
        .inflate(inflater)
        .also { binding = it }
        .run { root }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private var shouldRefreshFavorites: Boolean = false


    private fun initViews() {
        binding.swipeLayout.setOnRefreshListener {
            shouldRefreshFavorites = !shouldRefreshFavorites
            observeAniList()
        }
    }


    private fun observeAniList() {
        collectFlow(viewModel.favoritesList) { favoritesUiState ->
            binding.favoritesRecycler.layoutManager = GridLayoutManager(requireContext(), 3)
            binding.favoritesRecycler.withModels {
                when (favoritesUiState) {

                    is FavoritesUiState.Error -> {
                        showSnack(binding.root, "Error getting favorites")
                        binding.nothingSaved.isVisible = true
                    }

                    FavoritesUiState.Loading -> showLoading(true)


                    is FavoritesUiState.Success -> {
                        showLoading(false)
                        favoritesUiState.data.forEach { media ->
                            favoriteAnime {
                                id(media.idAniList)
                                clickListener { _ ->
                                    val directions =
                                        FavoritesFragmentDirections.actionNavigationFavoritesToNavigationDetails(
                                            media
                                        )
                                    findNavController().navigate(directions)
                                }
                                animeInfo(media)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.swipeLayout.isRefreshing = isLoading
        binding.favoritesRecycler.isVisible = !isLoading
    }


    override fun onResume() {
        super.onResume()
        if (requireActivity() is MainActivity) {
            (activity as MainActivity?)?.showBottomNavBar()
        }
    }


    override fun onStart() {
        super.onStart()
        handleNetworkChanges()
    }

    private fun handleNetworkChanges() {
        requireActivity().isConnectedToInternet(viewLifecycleOwner) { isConnected ->
            if (isConnected) observeAniList()
            binding.noInternetResult.noInternet.isVisible = !isConnected
            binding.favoritesRecycler.isVisible = isConnected
        }
    }
}
