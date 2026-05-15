package com.example.campusconnect.view

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.campusconnect.R
import com.example.campusconnect.databinding.FragmentChatListBinding
import com.google.android.material.tabs.TabLayoutMediator

class ChatListFragment : Fragment() {

    private var _binding: FragmentChatListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewPager()

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSearch.setOnClickListener {
            if (binding.cvSearch.visibility == View.VISIBLE) {
                binding.cvSearch.visibility = View.GONE
                dispatchSearch("")
            } else {
                binding.cvSearch.visibility = View.VISIBLE
                binding.etSearchChats.requestFocus()
            }
        }

        binding.etSearchChats.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                dispatchSearch(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnNewChat.setOnClickListener {
            findNavController().navigate(R.id.action_chatListFragment_to_userListFragment)
        }
    }

    private fun dispatchSearch(query: String) {
        val fragment = childFragmentManager.findFragmentByTag("f0")
        if (fragment is ChatTabFragment) {
            fragment.filter(query)
        }
    }

    private fun setupViewPager() {
        val adapter = ChatPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "CHATS"
                1 -> "STATUS"
                else -> "CALLS"
            }
        }.attach()
    }

    class ChatPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 3
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ChatTabFragment()
                1 -> StatusTabFragment()
                else -> CallsTabFragment()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
