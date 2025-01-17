package de.digural.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import de.digural.app.AppConstants
import de.digural.app.R

@AndroidEntryPoint
class InfoFragment : Fragment() {

    private lateinit var wvInfo: WebView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        wvInfo = view.findViewById(R.id.wvInfo)
    }

    override fun onResume() {
        super.onResume()
        wvInfo.loadUrl(de.digural.app.AppConstants.DIGURAL_INFO_URL)
    }
}