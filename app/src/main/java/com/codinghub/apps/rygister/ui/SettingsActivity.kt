package com.codinghub.apps.rygister.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.SeekBar
import androidx.lifecycle.ViewModelProviders
import com.codinghub.apps.rygister.R
import com.codinghub.apps.rygister.model.preferences.AppPrefs
import com.codinghub.apps.rygister.viewmodel.SettingsViewModel
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : AppCompatActivity() {
    private lateinit var settingsViewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        this.title = getString(R.string.menu_settings)

        settingsViewModel = ViewModelProviders.of(this).get(SettingsViewModel::class.java)

        assert(supportActionBar != null)   //null check

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.elevation = 0.0f
        updateUI()

        autoSnapSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                settingsViewModel.saveAutoSnapMode(true)
            } else {
                settingsViewModel.saveAutoSnapMode(false)
            }
        }

        boundingBoxSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                settingsViewModel.saveShowBoundingBoxState(true)
            } else {
                settingsViewModel.saveShowBoundingBoxState(false)
            }
        }

        distanceSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                updateDistanceTextView(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                settingsViewModel.saveSnapDistance(seekBar.progress * 20000)
            }
        })

        similaritySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                updateSimilarityTextView(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                settingsViewModel.saveSimilarity(seekBar.progress)
            }
        })



    }

    private fun updateUI() {
        //notification
        //auto snap
        autoSnapSwitch.isChecked = AppPrefs.getAutoSnapMode()
        boundingBoxSwitch.isChecked = AppPrefs.getShowBoundingBoxState()

        distanceSeekBar.progress = AppPrefs.getSnapDistance() / 20000
        updateDistanceTextView(distanceSeekBar.progress)

        similaritySeekBar.progress = AppPrefs.getSimilarity()
        updateSimilarityTextView(similaritySeekBar.progress)

    }

    private fun updateDistanceTextView(distance: Int) {
        distanceTextView.text = "Distance : ${distance.toString()}"
    }

    private fun updateSimilarityTextView(distance: Int) {
        similarityTextView.text = "Similarity Matching : ${distance.toString()} %"
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

}
