package com.amadev.rando.ui.fragments.mainFragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.amadev.rando.R
import com.amadev.rando.adapter.MoviesGridRecyclerViewAdapter
import com.amadev.rando.adapter.MoviesRecyclerViewAdapter
import com.amadev.rando.adapter.UpcomingMoviesRecyclerViewAdapter
import com.amadev.rando.databinding.FragmentMainBinding
import com.amadev.rando.model.MovieDetailsResults
import com.amadev.rando.ui.dialogs.logout.LogoutDialog
import com.amadev.rando.util.Util.isNetworkAvailable
import com.amadev.rando.util.Util.showToast
import com.google.android.material.navigation.NavigationView
import org.koin.android.viewmodel.ext.android.viewModel

class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private val mainFragmentViewModel: MainFragmentViewModel by viewModel()
    private val action = R.id.action_mainFragment_to_movieDetailsFragment
    private var isUserLoggedIn: Boolean = false

    private lateinit var navView: NavigationView
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var gridLayoutManager: GridLayoutManager
    private lateinit var topRatedAdapter: MoviesRecyclerViewAdapter
    private lateinit var popularAdapter: MoviesRecyclerViewAdapter
    private lateinit var nowPlayingAdapter: MoviesRecyclerViewAdapter
    private lateinit var upcomingAdapter: UpcomingMoviesRecyclerViewAdapter
    private lateinit var searchAdapter: MoviesGridRecyclerViewAdapter

    companion object {
        const val DELAY_TIME = 1500L
        val currentPage = (0..10).random()
        var searchViewsOpen = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (isNetworkAvailable(requireContext())) {
            setUpProgressBarVisibility(true)
            setUpSearchViewsVisibility()
            setUpRecyclerViewAdapters()
            setUpObservers()
            getMovies()
            checkIsUserLoggedIn()
            provideUsername()
            setUpOnClickListeners()
            setUpSearchMoviesEditText()
            setUpOnBackPressedCallback()
            setUpDrawer()
            setUpBottomNavMenu()
        } else {
            showToast(requireContext(), getString(R.string.noInternetConnection))
        }
    }

    private fun setUpSearchViewsVisibility() {
        if (searchViewsOpen) setUpSearchOnViewsVisibility() else setUpSearchOffViewsVisibility()
    }

    private fun setUpProgressBarVisibility(visible: Boolean) {
        binding.progressBar.apply {
            visibility = if (visible) View.VISIBLE else View.GONE
        }
    }

    private fun setUpRecyclerViewAdapters() {
        setUpTopRatedRecyclerviewAdapter()
        setUpPopularRecyclerviewAdapter()
        setUpNowPlayingRecyclerviewAdapter()
        setUpUpcomingRecyclerviewAdapter()
        setUpSearchedMoviesRecyclerViewAdapter()
    }

    private fun sendEmail(title: String) {
        val selectorIntent = Intent(Intent.ACTION_SENDTO)
        selectorIntent.data = Uri.parse("mailto:")

        val emailIntent = Intent(Intent.ACTION_SEND)
        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf("simpleapsdeveloper@gmail.com"))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, title)
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Write your message here")
        emailIntent.selector = selectorIntent

        activity?.startActivity(Intent.createChooser(emailIntent, "Email to..."))
    }

    private fun setUpOnBackPressedCallback() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.searchMoviesEditText.visibility == View.VISIBLE) {
                    searchViewsOpen = false
                    setUpSearchOffViewsVisibility()
                } else if (!isUserLoggedIn) {
                    navigateToSignInOrSignUpFragment()
                } else {
                    activity?.finishAffinity()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun searchMovies(query: String) {
        mainFragmentViewModel.getSearchedMovies(query)
    }

    private fun setUpSearchMoviesEditText() {
        binding.searchMoviesEditText.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s != null) {
                    if (s.isEmpty().not()) {
                        Handler(Looper.myLooper()!!).postDelayed({
                            searchMovies(s.toString())
                        }, DELAY_TIME)
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setUpDrawer() {
        navView = binding.navView
        navView.getHeaderView(0)
        navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.drawerLogout -> provideLogoutDialog()
                R.id.drawerContact -> sendEmail(getString(R.string.sineContact))
                R.id.drawerFeedback -> sendEmail(getString(R.string.sineFeedback))
            }
            true
        }
    }

    private fun setUpBottomNavMenu() {
        binding.bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.bottomNavSearch -> {
                    searchViewsOpen = true
                    setUpSearchOnViewsVisibility()
                }
                R.id.bottomNavFavorites -> navigateToFavoritesFragment()
                R.id.bottomNavMenu -> openDrawer()
            }
            true
        }
    }

    private fun openDrawer() {
        binding.drawer.openDrawer(Gravity.LEFT)
    }

    private fun setUpOnClickListeners() {
        binding.apply {
            searchOff.setOnClickListener {
                searchViewsOpen = false
                clearSearchedText()
                setUpSearchOffViewsVisibility()
            }
            topRatedMore.setOnClickListener {
                navigateToTopRatedFragment()
            }
            upcomingMore.setOnClickListener {
                navigateToUpcomingFragment()
            }
            nowPlayingMore.setOnClickListener {
                navigateToNowPlayingFragment()
            }
            popularMore.setOnClickListener {
                navigateToPopularFragment()
            }
        }
    }

    private fun provideLogoutDialog() {
        val dialog = LogoutDialog()
        dialog.show(childFragmentManager, null)
    }

    private fun setUpNavHeaderUsername(username: String?) {
        val usernameTv = navView.getHeaderView(0).findViewById<TextView>(R.id.username)
        usernameTv.text = username
    }

    private fun provideUsername() {
        mainFragmentViewModel.provideUsername()
    }

    private fun checkIsUserLoggedIn() {
        mainFragmentViewModel.isUserLoggedIn()
    }

    private fun navigateToFavoritesFragment() {
        if (isUserLoggedIn) {
            findNavController().navigate(R.id.action_mainFragment_to_favoritesFragment)
        } else {
            showToast(requireContext(), getString(R.string.youMustBeLoggedIn))
        }
    }

    private fun navigateToSignInOrSignUpFragment() {
        findNavController().navigate(R.id.action_mainFragment_to_signinOrSignUpFragment)
    }

    private fun navigateToUpcomingFragment() {
        findNavController().navigate(R.id.action_mainFragment_to_upcomingFragment)
    }

    private fun navigateToPopularFragment() {
        findNavController().navigate(R.id.action_mainFragment_to_popularFragment)
    }

    private fun navigateToTopRatedFragment() {
        findNavController().navigate(R.id.action_mainFragment_to_topRatedFragment)
    }

    private fun navigateToNowPlayingFragment() {
        findNavController().navigate(R.id.action_mainFragment_to_nowPlayingFragment)
    }

    private fun clearSearchedText() {
        binding.searchMoviesEditText.text.clear()
    }

    private fun setUpViewVisibilityGone(view: View) {
        view.visibility = View.GONE
    }

    private fun setUpViewVisibilityVisible(view: View) {
        view.visibility = View.VISIBLE
    }

    private fun setUpObservers() {
        mainFragmentViewModel.apply {
            binding.apply {
                topRatedMoviesResultsLiveData.observe(viewLifecycleOwner) {
                    setUpProgressBarVisibility(false)
                    setUpMoviesHorizontalRecyclerView(it, topRatedAdapter)
                }
                popularMoviesResultsLiveData.observe(viewLifecycleOwner) {
                    setUpMoviesHorizontalRecyclerView(it, popularAdapter)
                }
                nowPlayingMoviesLiveData.observe(viewLifecycleOwner) {
                    setUpMoviesHorizontalRecyclerView(it, nowPlayingAdapter)
                }
                upcomingMoviesResultsLiveData.observe(viewLifecycleOwner) {
                    setUpUpcomingMoviesHorizontalRecyclerViewAdapter(it, upcomingAdapter)
                }
                searchedMoviesLiveData.observe(viewLifecycleOwner) {
                    setUpGridRecyclerView(it, searchAdapter)
                }
                isUserLoggedIn.observe(viewLifecycleOwner) {
                    this@MainFragment.isUserLoggedIn = it
                }
                username.observe(viewLifecycleOwner) {
                    setUpNavHeaderUsername(it)
                }
            }
        }
    }

    private fun getMovies() {
        mainFragmentViewModel.apply {
            Handler(Looper.myLooper()!!).postDelayed({
                getTopRatedMovies(currentPage)
                getPopularMovies(currentPage)
                getUpcomingMovies(currentPage)
                getNowPlayingMovies(currentPage)
            }, DELAY_TIME)
        }
    }

    private fun setUpSearchedMoviesRecyclerViewAdapter() {
        gridLayoutManager = GridLayoutManager(requireContext(), 3)
        searchAdapter =
            MoviesGridRecyclerViewAdapter(requireView(), requireContext(), arrayListOf(), action)
        searchAdapter.apply {
            binding.searchedMoviesRecyclerView.apply {
                layoutManager = gridLayoutManager
                adapter = searchAdapter
            }
        }
    }

    private fun setUpTopRatedRecyclerviewAdapter() {
        linearLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        topRatedAdapter =
            MoviesRecyclerViewAdapter(requireView(), requireContext(), arrayListOf(), action)
        binding.apply {
            topRatedRecyclerView.apply {
                layoutManager = linearLayoutManager
                adapter = topRatedAdapter
            }
        }
    }

    private fun setUpPopularRecyclerviewAdapter() {
        linearLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        popularAdapter =
            MoviesRecyclerViewAdapter(requireView(), requireContext(), arrayListOf(), action)
        binding.apply {
            popularRecyclerView.apply {
                layoutManager = linearLayoutManager
                adapter = popularAdapter
            }
        }
    }

    private fun setUpNowPlayingRecyclerviewAdapter() {
        linearLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        nowPlayingAdapter =
            MoviesRecyclerViewAdapter(requireView(), requireContext(), arrayListOf(), action)
        binding.apply {
            nowPlayingRecyclerView.apply {
                layoutManager = linearLayoutManager
                adapter = nowPlayingAdapter
            }
        }
    }

    private fun setUpGridRecyclerView(
        list: ArrayList<MovieDetailsResults>,
        adapter: MoviesGridRecyclerViewAdapter
    ) {
        adapter.notifyDataSetChanged()
        adapter.list.apply {
            clear()
            addAll(list)
        }

    }

    private fun setUpMoviesHorizontalRecyclerView(
        list: ArrayList<MovieDetailsResults>,
        adapter: MoviesRecyclerViewAdapter
    ) {
        adapter.notifyDataSetChanged()
        adapter.list.apply {
            clear()
            addAll(list)
        }
    }

    private fun setUpUpcomingRecyclerviewAdapter() {
        upcomingAdapter =
            UpcomingMoviesRecyclerViewAdapter(requireView(), requireContext(), arrayListOf())
        binding.apply {
            upcomingRecyclerView.apply {
                layoutManager = upcomingRecyclerView.getCarouselLayoutManager()
                adapter = upcomingAdapter
                setInfinite(true)
                setAlpha(true)
            }
        }
    }

    private fun setUpUpcomingMoviesHorizontalRecyclerViewAdapter(
        list: ArrayList<MovieDetailsResults>,
        adapter: UpcomingMoviesRecyclerViewAdapter
    ) {
        adapter.notifyDataSetChanged()
        adapter.list.apply {
            clear()
            addAll(list)
        }
    }

    private fun setUpSearchOnViewsVisibility() {
        searchViewsOpen = true
        binding.apply {
            setUpViewVisibilityGone(upcoming)
            setUpViewVisibilityGone(upcomingMore)
            setUpViewVisibilityGone(upcomingRecyclerView)
            setUpViewVisibilityGone(topRated)
            setUpViewVisibilityGone(topRatedMore)
            setUpViewVisibilityGone(topRatedRecyclerView)
            setUpViewVisibilityGone(popular)
            setUpViewVisibilityGone(popularMore)
            setUpViewVisibilityGone(popularRecyclerView)
            setUpViewVisibilityGone(nowPlaying)
            setUpViewVisibilityGone(nowPlayingMore)
            setUpViewVisibilityGone(nowPlayingRecyclerView)
            setUpViewVisibilityVisible(searchMoviesEditText)
            setUpViewVisibilityVisible(searchedMoviesRecyclerView)
            setUpViewVisibilityVisible(searchedMoviesResults)
            setUpViewVisibilityVisible(searchOff)
        }
    }

    private fun setUpSearchOffViewsVisibility() {
        binding.apply {
            setUpViewVisibilityVisible(upcoming)
            setUpViewVisibilityVisible(upcomingMore)
            setUpViewVisibilityVisible(upcomingRecyclerView)
            setUpViewVisibilityVisible(topRated)
            setUpViewVisibilityVisible(topRatedMore)
            setUpViewVisibilityVisible(topRatedRecyclerView)
            setUpViewVisibilityVisible(popular)
            setUpViewVisibilityVisible(popularMore)
            setUpViewVisibilityVisible(popularRecyclerView)
            setUpViewVisibilityVisible(nowPlaying)
            setUpViewVisibilityVisible(nowPlayingMore)
            setUpViewVisibilityVisible(nowPlayingRecyclerView)
            setUpViewVisibilityGone(searchedMoviesRecyclerView)
            setUpViewVisibilityGone(searchedMoviesResults)
            setUpViewVisibilityGone(searchOff)
            setUpViewVisibilityGone(searchMoviesEditText)
        }
    }
}
