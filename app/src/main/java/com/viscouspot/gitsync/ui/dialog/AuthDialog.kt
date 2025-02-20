package com.viscouspot.gitsync.ui.dialog

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.google.android.material.button.MaterialButton
import com.viscouspot.gitsync.R
import com.viscouspot.gitsync.ui.adapter.GitProviderAdapter
import com.viscouspot.gitsync.util.Helper
import com.viscouspot.gitsync.util.SettingsManager
import com.viscouspot.gitsync.util.provider.GitProviderManager


class AuthDialog(private val context: Context, private val settingsManager: SettingsManager, private val setGitCredentials: (username: String?, token: String?) -> Unit) : BaseDialog(context)  {
    private val providers = GitProviderManager.detailsMap
    private lateinit var oAuthContainer: ConstraintLayout
    private lateinit var oAuthButton: MaterialButton

    private lateinit var httpContainer: ConstraintLayout
    private lateinit var usernameInput: EditText
    private lateinit var tokenInput: EditText
    private lateinit var loginButton: MaterialButton

    private lateinit var sshContainer: ConstraintLayout
    private lateinit var pKeyButton: MaterialButton
    private lateinit var generateKeyButton: MaterialButton

    override fun onStart() {
        super.onStart()
        setContentView(R.layout.dialog_auth)

        val spinner = findViewById<Spinner>(R.id.gitProviderSpinner) ?: return
        val adapter = GitProviderAdapter(context, providers.values.toList())

        spinner.adapter = adapter
        spinner.post{
            spinner.dropDownWidth = spinner.width
        }

        oAuthContainer = findViewById(R.id.oAuthContainer) ?: return
        oAuthButton = findViewById(R.id.oAuthButton) ?: return

        httpContainer = findViewById(R.id.httpContainer) ?: return
        usernameInput = findViewById(R.id.usernameInput) ?: return
        tokenInput = findViewById(R.id.tokenInput) ?: return
        loginButton = findViewById(R.id.loginButton) ?: return

        sshContainer = findViewById(R.id.sshContainer) ?: return
        pKeyButton = findViewById(R.id.pKeyButton) ?: return
        generateKeyButton = findViewById(R.id.generateKeyButton) ?: return

        spinner.setSelection(providers.keys.toList().indexOf(settingsManager.getGitProvider()))

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val provider = providers.keys.toList()[position]
                settingsManager.setGitProvider(provider)
                updateInputs(provider)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun updateInputs(provider: GitProviderManager.Companion.Provider) {
        when (provider) {
            GitProviderManager.Companion.Provider.GITHUB,
            GitProviderManager.Companion.Provider.GITEA -> {
                oAuthContainer.visibility = View.VISIBLE
                httpContainer.visibility = View.GONE
                sshContainer.visibility = View.GONE

                oAuthButton.setOnClickListener {
                    val gitManager = GitProviderManager.getManager(context, settingsManager)
                    gitManager.launchOAuthFlow()
                    dismiss()
                }
            }
            GitProviderManager.Companion.Provider.HTTPS -> {
                httpContainer.visibility = View.VISIBLE
                oAuthContainer.visibility = View.GONE
                sshContainer.visibility = View.GONE

                usernameInput.doOnTextChanged { text, _, _, _ ->
                    if (text.isNullOrEmpty()) {
                        loginButton.isEnabled = false
                        loginButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.auth_green_secondary))
                    } else {
                        if (!tokenInput.text.isNullOrEmpty()) {
                            loginButton.isEnabled = true
                            loginButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.auth_green))
                        }
                    }
                }

                tokenInput.doOnTextChanged { text, _, _, _ ->
                    if (text.isNullOrEmpty()) {
                        loginButton.isEnabled = false
                        loginButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.auth_green_secondary))
                    } else {
                        if (!usernameInput.text.isNullOrEmpty()) {
                            loginButton.isEnabled = true
                            loginButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.auth_green))
                        }
                    }
                }

                loginButton.setOnClickListener {
                    setGitCredentials(usernameInput.text.toString(), tokenInput.text.toString())
                    dismiss()
                }
            }
            GitProviderManager.Companion.Provider.SSH -> {
                sshContainer.visibility = View.VISIBLE
                httpContainer.visibility = View.GONE
                oAuthContainer.visibility = View.GONE

                var key: String? = null
                pKeyButton.text = context.getString(R.string.ssh_key_example)
                pKeyButton.isEnabled = false
                pKeyButton.icon = AppCompatResources.getDrawable(context, R.drawable.copy_to_clipboard)
                pKeyButton.setIconTintResource(R.color.primary_light)

                generateKeyButton.setOnClickListener {
                    if (key == null) {
                        val keyPair = Helper.generateSSHKeyPair()

                        key = keyPair.first
                        pKeyButton.text = keyPair.second

                        pKeyButton.isEnabled = true
                        generateKeyButton.text = context.getString(R.string.confirm_key_saved)
                        generateKeyButton.isEnabled = false
                        generateKeyButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.auth_green_secondary))
                    } else {
                        setGitCredentials(null, key!!)
                        dismiss()
                    }
                }

                pKeyButton.setOnClickListener {
                    pKeyButton.icon = AppCompatResources.getDrawable(context, R.drawable.confirm_clipboard)
                    pKeyButton.setIconTintResource(R.color.auth_green)

                    val clipboard: ClipboardManager? = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                    val clip = ClipData.newPlainText(context.getString(R.string.copied_text), pKeyButton.text)
                    clipboard?.setPrimaryClip(clip)

                    generateKeyButton.isEnabled = true
                    generateKeyButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.auth_green))
                }
            }
        }
    }
}