package tv.mango.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import tv.mango.app.databinding.FragmentWebviewHomeBinding

/**
 * Home, hosted as a WebView loading the Mango-Homepage frontend
 * (`homepage/`, built to `app/src/main/assets/homepage/`) rather than a
 * native Views screen.
 *
 * See `homepage/` for the actual interface and its own D-pad
 * spatial-navigation engine, which drives itself entirely inside the page
 * using the arrow-key/Enter/Escape events a WebView already forwards from a
 * Fire TV remote - nothing here participates in focus beyond handing it to
 * the WebView once the page has loaded.
 */
class WebViewHomeFragment : Fragment() {

    private var binding: FragmentWebviewHomeBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = FragmentWebviewHomeBinding.inflate(inflater, container, false)
        binding = view
        return view.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val webView = binding?.homeWebView ?: return

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        webView.loadUrl(HOME_URL)
        webView.post { webView.requestFocus() }
    }

    override fun onDestroyView() {
        binding?.homeWebView?.apply {
            stopLoading()
            (parent as? ViewGroup)?.removeView(this)
            destroy()
        }
        binding = null
        super.onDestroyView()
    }

    private companion object {
        /** The homepage build's own index, bundled offline under assets/. */
        const val HOME_URL = "file:///android_asset/homepage/index.html"
    }
}
