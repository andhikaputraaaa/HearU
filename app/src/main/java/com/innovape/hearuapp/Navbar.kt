package com.innovape.hearuapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

class Navbar : Fragment() {

    interface OnNavigationClickListener {
        fun onHomeClick()
        fun onEditClick()
        fun onProfileClick()
    }

    private var listener: OnNavigationClickListener? = null
    private lateinit var navHome: ImageView
    private lateinit var navEdit: ImageView
    private lateinit var navProfile: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_navbar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navHome = view.findViewById(R.id.nav_home)
        navEdit = view.findViewById(R.id.nav_edit)
        navProfile = view.findViewById(R.id.nav_profile)

        navHome.setOnClickListener {
            setActiveItem(0)
            listener?.onHomeClick()
        }

        navEdit.setOnClickListener {
            setActiveItem(1)
            listener?.onEditClick()
        }

        navProfile.setOnClickListener {
            setActiveItem(2)
            listener?.onProfileClick()
        }
    }

    fun setActiveItem(position: Int) {
        // Reset all items to inactive
        navHome.isSelected = false
        navEdit.isSelected = false
        navProfile.isSelected = false

        // Set selected item to active
        when (position) {
            0 -> navHome.isSelected = true
            1 -> navEdit.isSelected = true
            2 -> navProfile.isSelected = true
        }
    }

    fun setOnNavigationClickListener(listener: OnNavigationClickListener) {
        this.listener = listener
    }
}
