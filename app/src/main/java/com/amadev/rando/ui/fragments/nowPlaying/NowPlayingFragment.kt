package com.amadev.rando.ui.fragments.nowPlaying

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amadev.rando.R
import com.amadev.rando.adapter.EndlessRecyclerOnScrollListener
import com.amadev.rando.adapter.MoviesRecyclerViewAdapter
import com.amadev.rando.databinding.FragmentNowPlayingBinding
import com.amadev.rando.model.MovieDetailsResults
import org.koin.android.viewmodel.ext.android.viewModel

class NowPlayingFragment : Fragment() {

    private var _binding: FragmentNowPlayingBinding? = null
    private val binding get() = _binding!!
    private val nowPlayingViewModel: NowPlayingViewModel by viewModel()
    private val action = R.id.action_nowPlayingFragment_to_movieDetailsFragment
    lateinit var gridLayoutManager: GridLayoutManager
    lateinit var adapter: MoviesRecyclerViewAdapter

    companion object {
        var CURRENT_PAGE = 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNowPlayingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getTopRatedMovies()
        setUpRecyclerviewAdapter()
        setUpObservers()
        setUpOnClickListeners()
    }

    private fun setUpRecyclerviewAdapter() {
        gridLayoutManager = GridLayoutManager(requireContext(), 3)
        adapter =
            MoviesRecyclerViewAdapter(requireView(), requireContext(), arrayListOf(), action)
        binding.apply {
            nowPlayingRecyclerView.layoutManager = gridLayoutManager
            nowPlayingRecyclerView.adapter = adapter

        }
    }

    private fun setUpOnClickListeners() {
        binding.apply {
            backBtn.setOnClickListener {
                navigateToMainFragment()
            }
        }
    }

    private fun navigateToMainFragment() {
        findNavController().navigate(R.id.action_nowPlayingFragment_to_mainFragment)
    }

    private fun setUpObservers() {
        nowPlayingViewModel.nowPlayingMoviesResultsLiveData.observe(viewLifecycleOwner) {
            setUpRecyclerView(it, binding.nowPlayingRecyclerView)
        }
    }

    private fun setUpRecyclerView(
        list: ArrayList<MovieDetailsResults>,
        recyclerView: RecyclerView
    ) {
        adapter.notifyDataSetChanged()
        adapter.list.apply {
            clear()
            addAll(list)
        }
        recyclerView.addOnScrollListener(object :
            EndlessRecyclerOnScrollListener(gridLayoutManager) {
            override fun onLoadMore(current_page: Int) {
                getTopRatedMovies()
            }
        })
    }

    private fun getTopRatedMovies() {
        CURRENT_PAGE++
        nowPlayingViewModel.getPopularMovies(CURRENT_PAGE)
    }
}