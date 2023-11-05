/*
 * Copyright 2019-2020 Ernst Jan Plugge <rmc@dds.nl>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smouldering_durtles.wk.fragments

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.Preference.SummaryProvider
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.TwoStatePreference
import com.smouldering_durtles.wk.Actment
import com.smouldering_durtles.wk.Constants
import com.smouldering_durtles.wk.GlobalSettings
import com.smouldering_durtles.wk.R
import com.smouldering_durtles.wk.activities.AboutActivity
import com.smouldering_durtles.wk.activities.AbstractActivity
import com.smouldering_durtles.wk.activities.BackupActivity
import com.smouldering_durtles.wk.activities.DataImportExportActivity
import com.smouldering_durtles.wk.activities.FontImportActivity
import com.smouldering_durtles.wk.activities.FontSelectionActivity
import com.smouldering_durtles.wk.activities.KeyboardHelpActivity
import com.smouldering_durtles.wk.activities.SupportActivity
import com.smouldering_durtles.wk.activities.ThemeCustomizationActivity
import com.smouldering_durtles.wk.api.ApiState
import com.smouldering_durtles.wk.components.NumberRangePreference
import com.smouldering_durtles.wk.components.NumberRangePreferenceDialogFragment
import com.smouldering_durtles.wk.components.TaggedUrlPreference
import com.smouldering_durtles.wk.components.TaggedUrlPreferenceDialogFragment
import com.smouldering_durtles.wk.jobs.ResetDatabaseJob
import com.smouldering_durtles.wk.livedata.LiveApiState
import com.smouldering_durtles.wk.services.JobRunnerService
import com.smouldering_durtles.wk.util.AudioUtil
import com.smouldering_durtles.wk.util.DbLogger
import com.smouldering_durtles.wk.util.ObjectSupport
import com.smouldering_durtles.wk.util.TextUtil
import com.smouldering_durtles.wk.util.ThemeUtil
import java.util.Objects

/**
 * Fragment for preferences.
 */
class PreferencesFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    private fun onViewCreatedBase(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setBackgroundColor(ThemeUtil.getColor(R.attr.colorBackground))
        val enableAdvanced = findPreference<Preference>("enable_advanced")
        if (enableAdvanced != null) {
            enableAdvanced.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference: Preference, newValue: Any? ->
                    ObjectSupport.safe(false) {
                        val enabled = ObjectSupport.isTrue(newValue)
                        if (enabled && !GlobalSettings.getAdvancedEnabled()) {
                            AlertDialog.Builder(view.context)
                                .setTitle("Enable advanced settings?")
                                .setMessage(TextUtil.renderHtml(Constants.ENABLE_ADVANCED_WARNING))
                                .setIcon(R.drawable.ic_baseline_warning_24px)
                                .setNegativeButton("No") { dialog: DialogInterface?, which: Int ->
                                    ObjectSupport.safe {
                                        GlobalSettings.setAdvancedEnabled(false)
                                        setVisibility("advanced_lesson_settings", false)
                                        setVisibility("advanced_review_settings", false)
                                        setVisibility("advanced_self_study_settings", false)
                                        setVisibility("advanced_other_settings", false)
                                        (preference as TwoStatePreference).isChecked = false
                                    }
                                }
                                .setPositiveButton("Yes") { dialog: DialogInterface?, which: Int ->
                                    ObjectSupport.safe {
                                        GlobalSettings.setAdvancedEnabled(true)
                                        setVisibility("advanced_lesson_settings", true)
                                        setVisibility("advanced_review_settings", true)
                                        setVisibility("advanced_self_study_settings", true)
                                        setVisibility("advanced_other_settings", true)
                                        (preference as TwoStatePreference).isChecked = true
                                    }
                                }.create().show()
                            return@safe false
                        } else {
                            setVisibility("advanced_lesson_settings", enabled)
                            setVisibility("advanced_review_settings", enabled)
                            setVisibility("advanced_self_study_settings", enabled)
                            setVisibility("advanced_other_settings", enabled)
                        }
                        true
                    }
                }
        }
        setOnPreferenceClick("reset_database") { preference: Preference ->
            ObjectSupport.safe(false) {
                AlertDialog.Builder(preference.context)
                    .setTitle("Reset database?")
                    .setMessage(TextUtil.renderHtml(Constants.RESET_DATABASE_WARNING))
                    .setIcon(R.drawable.ic_baseline_warning_24px)
                    .setNegativeButton("No") { dialog: DialogInterface?, which: Int -> }
                    .setPositiveButton("Yes") { dialog: DialogInterface?, which: Int ->
                        ObjectSupport.safe {
                            JobRunnerService.schedule(ResetDatabaseJob::class.java, "")
                            goToMainActivity()
                        }
                    }.create().show()
                true
            }
        }
        setOnPreferenceClick("reset_tutorials") { preference: Preference ->
            ObjectSupport.safe(false) {
                AlertDialog.Builder(preference.context)
                    .setTitle("Reset confirmations and tutorials?")
                    .setMessage(TextUtil.renderHtml(Constants.RESET_TUTORIALS_WARNING))
                    .setIcon(R.drawable.ic_baseline_warning_24px)
                    .setNegativeButton("No") { dialog: DialogInterface?, which: Int -> }
                    .setPositiveButton("Yes") { dialog: DialogInterface?, which: Int ->
                        ObjectSupport.safe {
                            GlobalSettings.resetConfirmationsAndTutorials()
                            Toast.makeText(
                                preference.context,
                                "Confirmations and tutorials reset",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }.create().show()
                true
            }
        }
        setOnPreferenceClick("backup_settings") { preference: Preference ->
            AlertDialog.Builder(preference.context)
                .setTitle("Backup settings?")
                .setMessage("Are you sure you want to backup your settings?")
                .setIcon(R.drawable.ic_baseline_warning_24px)
                .setNegativeButton("No") { dialog: DialogInterface?, which: Int -> }
                .setPositiveButton("Yes") { dialog: DialogInterface?, which: Int ->
                    try {
                        startBackup()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }.create().show()
            true
        }
        setOnPreferenceClick("restore_settings") { preference: Preference ->
            AlertDialog.Builder(preference.context)
                .setTitle("Restore settings?")
                .setMessage("Are you sure you want to restore your settings?")
                .setIcon(R.drawable.ic_baseline_warning_24px)
                .setNegativeButton("No") { dialog: DialogInterface?, which: Int -> }
                .setPositiveButton("Yes") { dialog: DialogInterface?, which: Int ->
                    try {
                        startRestore()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }.create().show()
            true
        }
        setOnPreferenceClick("upload_debug_log") { preference: Preference ->
            ObjectSupport.safe(false) {
                AlertDialog.Builder(preference.context)
                    .setTitle("Upload debug log?")
                    .setMessage(TextUtil.renderHtml(Constants.UPLOAD_DEBUG_LOG_WARNING))
                    .setIcon(R.drawable.ic_baseline_warning_24px)
                    .setNegativeButton("No") { dialog: DialogInterface?, which: Int -> }
                    .setPositiveButton("Yes") { dialog: DialogInterface?, which: Int ->
                        ObjectSupport.safe {
                            ObjectSupport.runAsync(
                                this, { DbLogger.uploadLog() }
                            ) { result: Boolean? ->
                                if (result != null && result) {
                                    Toast.makeText(
                                        requireContext(),
                                        "Upload successful, thanks!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        requireContext(),
                                        "Upload failed",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }.create().show()
                true
            }
        }
        val audioLocation = findPreference<ListPreference?>("audio_location")
        if (audioLocation != null) {
            val locationValues = AudioUtil.getLocationValues()
            val locations = AudioUtil.getLocations(locationValues)
            audioLocation.entries = locations.toArray<String>(arrayOf<String>())
            audioLocation.entryValues = locationValues.toArray<String>(arrayOf<String>())
            audioLocation.isVisible = true
        }
        setVisibility("api_key_help", LiveApiState.getInstance().get() != ApiState.OK)
        setVisibility("advanced_lesson_settings", GlobalSettings.getAdvancedEnabled())
        setVisibility("advanced_review_settings", GlobalSettings.getAdvancedEnabled())
        setVisibility("advanced_self_study_settings", GlobalSettings.getAdvancedEnabled())
        setVisibility("advanced_other_settings", GlobalSettings.getAdvancedEnabled())
        setVisibility("ime_hint_reading", Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        setVisibility("ime_hint_meaning", Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        setVisibility("web_password", true)
        setSummaryHtml("api_key_help", Constants.API_KEY_PERMISSION_NOTICE)
        setSummaryHtml("experimental_status", Constants.EXPERIMENTAL_PREFERENCE_STATUS_NOTICE)
        setSummaryHtml("lesson_subject_selection", Constants.SUBJECT_SELECTION_NOTICE)
        setSummaryHtml("review_subject_selection", Constants.SUBJECT_SELECTION_NOTICE)
        setSummaryHtml("self_study_subject_selection", Constants.SUBJECT_SELECTION_NOTICE)
        setNumberInputType("overdue_threshold")
        setNumberInputType("max_lesson_session_size")
        setNumberInputType("max_review_session_size")
        setNumberInputType("max_self_study_session_size")
        setDecimalNumberInputType("next_button_delay")
        setOnClickGoToActivity("about_this_app", AboutActivity::class.java)
        setOnClickGoToActivity("support_and_feedback", SupportActivity::class.java)
        setOnClickGoToActivity("theme_customization", ThemeCustomizationActivity::class.java)
        setOnClickGoToActivity("font_selection", FontSelectionActivity::class.java)
        setOnClickGoToActivity("font_import", FontImportActivity::class.java)
        setOnClickGoToActivity("keyboard_help", KeyboardHelpActivity::class.java)
        setOnClickGoToActivity("data_import_export", DataImportExportActivity::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ObjectSupport.safe { onViewCreatedBase(view, savedInstanceState) }
    }

    override fun onResume() {
        ObjectSupport.safe {
            super.onResume()
            val args = arguments
            var title: CharSequence? = null
            if (args != null) {
                val key = args.getString(ARG_PREFERENCE_ROOT)
                if (key != null) {
                    val preference = findPreference<Preference>(key)
                    if (preference != null) {
                        title = preference.title
                    }
                }
            }
            if (title == null) {
                title = "Settings"
            }
            val activity: Activity? = activity
            if (activity is Actment) {
                val toolbar = (activity as Actment).toolbar
                if (toolbar != null) {
                    toolbar.title = title
                }
            }
        }
    }

    @Suppress("deprecation")
    override fun onDisplayPreferenceDialog(preference: Preference) {
        ObjectSupport.safe {
            if (preference is TaggedUrlPreference) {
                if (parentFragmentManager.findFragmentByTag("TaggedUrlPreference") != null) {
                    return@safe
                }
                val f: DialogFragment =
                    TaggedUrlPreferenceDialogFragment.newInstance(preference.getKey())
                f.setTargetFragment(this, 0)
                f.show(parentFragmentManager, "TaggedUrlPreference")
                return@safe
            }
            if (preference is NumberRangePreference) {
                if (parentFragmentManager.findFragmentByTag("NumberRangePreference") != null) {
                    return@safe
                }
                val f: DialogFragment =
                    NumberRangePreferenceDialogFragment.newInstance(preference.getKey())
                f.setTargetFragment(this, 0)
                f.show(parentFragmentManager, "NumberRangePreference")
                return@safe
            }
            super.onDisplayPreferenceDialog(preference)
        }
    }

    private fun goToActivity(clas: Class<out AbstractActivity?>) {
        val a: Activity? = activity
        if (a is Actment) {
            (a as Actment).goToActivity(clas)
        }
    }

    private fun goToMainActivity() {
        val a: Activity? = activity
        if (a is Actment) {
            (a as Actment).goToMainActivity()
        }
    }

    private fun setOnPreferenceClick(
        key: CharSequence,
        listener: Preference.OnPreferenceClickListener?
    ) {
        ObjectSupport.safe {
            val pref = findPreference<Preference>(key)
            if (pref != null) {
                pref.onPreferenceClickListener = listener
            }
        }
    }

    private fun setOnClickGoToActivity(key: CharSequence, clas: Class<out AbstractActivity?>) {
        ObjectSupport.safe {
            setOnPreferenceClick(key) { preference: Preference? ->
                ObjectSupport.safe { goToActivity(clas) }
                true
            }
        }
    }

    private fun startBackup() {
        ObjectSupport.safe {
            val intent = Intent(activity, BackupActivity::class.java)
            intent.action = "com.smouldering_durtles.wk.BACKUP"
            startActivity(intent)
        }
    }

    private fun startRestore() {
        ObjectSupport.safe {
            val intent = Intent(activity, BackupActivity::class.java)
            intent.action = "com.smouldering_durtles.wk.RESTORE"
            startActivity(intent)
        }
    }

    private fun setNumberInputType(key: CharSequence) {
        ObjectSupport.safe {
            val pref = findPreference<EditTextPreference?>(key)
            pref?.setOnBindEditTextListener { editText: EditText ->
                ObjectSupport.safe {
                    editText.inputType = InputType.TYPE_CLASS_NUMBER
                    editText.setSelection(editText.text.length)
                }
            }
        }
    }

    private fun setSummaryHtml(key: CharSequence, html: String) {
        ObjectSupport.safe {
            val pref = findPreference<Preference>(key)
            if (pref != null) {
                pref.summary = TextUtil.renderHtml(html)
            }
        }
    }

    private fun setDecimalNumberInputType(key: CharSequence) {
        ObjectSupport.safe {
            val pref = findPreference<EditTextPreference?>(key)
            pref?.setOnBindEditTextListener { editText: EditText ->
                ObjectSupport.safe {
                    editText.inputType =
                        InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                    editText.setSelection(editText.text.length)
                }
            }
        }
    }

    private fun setVisibility(key: CharSequence, visible: Boolean) {
        ObjectSupport.safe {
            val pref = findPreference<Preference>(key)
            if (pref != null) {
                pref.isVisible = visible
            }
        }
        val nightThemePreference =
            Objects.requireNonNull(findPreference<ListPreference>("nightTheme"))

// Set the summary provider to update the summary when the preference changes
        nightThemePreference.summaryProvider = SummaryProvider { preference: Preference ->
            val listPreference = preference as ListPreference
            val entry = Objects.requireNonNull(listPreference.entry)
            "$entry is used in system wide dark mode"
        }

// Set the initial summary based on the current value
        nightThemePreference.summary =
            nightThemePreference.entry.toString() + " is used in system wide dark mode"

// Listen for changes to update the summary accordingly
        nightThemePreference.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any ->
                val index = nightThemePreference.findIndexOfValue(newValue.toString())
                if (index >= 0) {
                    val entry = nightThemePreference.entries[index]
                    nightThemePreference.summary = "$entry is used when system dark mode is on"
                }
                true // True to update the state of the Preference with the new value
            }
    }
}