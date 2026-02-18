package com.example.decena

import android.app.AlertDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.imageview.ShapeableImageView
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {

    private lateinit var tvUsername: TextView
    private lateinit var imgAvatar: ShapeableImageView
    private lateinit var fieldFirst: View
    private lateinit var fieldLast: View
    private lateinit var fieldPhone: View
    private lateinit var fieldAbout: View

    private lateinit var viewModel: ProfileViewModel
    private lateinit var databaseHelper: ProfileDatabaseHelper

    // Track current values
    private var currentFirstName = "First Name"
    private var currentLastName = "Last Name"
    private var currentPhone = "Phone Number"
    private var currentAbout = "About You"

    // For camera and gallery
    private var currentPhotoUri: Uri? = null

    // Activity Result Launchers
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            saveAvatarUri(it)
            imgAvatar.setImageURI(it)
            Toast.makeText(context, "Profile picture updated", Toast.LENGTH_SHORT).show()
        }
    }

    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && currentPhotoUri != null) {
            saveAvatarUri(currentPhotoUri!!)
            imgAvatar.setImageURI(currentPhotoUri)
            Toast.makeText(context, "Profile picture updated", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(context, "Camera permission needed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            initializeViews(view)
            setupViewModel()
            setupClickListeners()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeViews(view: View) {
        tvUsername = view.findViewById(R.id.tvUsername)
        imgAvatar = view.findViewById(R.id.imgAvatar)
        fieldFirst = view.findViewById(R.id.fieldFirst)
        fieldLast = view.findViewById(R.id.fieldLast)
        fieldPhone = view.findViewById(R.id.fieldPhone)
        fieldAbout = view.findViewById(R.id.fieldAbout)

        // Set initial text for fields
        updateFieldText(fieldFirst, "First Name: $currentFirstName")
        updateFieldText(fieldLast, "Last Name: $currentLastName")
        updateFieldText(fieldPhone, "Phone: $currentPhone")
        updateFieldText(fieldAbout, "About: $currentAbout")
    }

    private fun updateFieldText(fieldView: View, text: String) {
        try {
            // Since your item_profile.xml has a TextView without ID,
            // we can try to cast the view itself if it's a TextView
            if (fieldView is TextView) {
                fieldView.text = text
            } else {
                // If it's a container (like CardView), try to find a TextView inside
                val textView = fieldView.findViewById<TextView>(android.R.id.text1)
                textView?.text = text
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getFieldText(fieldView: View): String {
        return try {
            if (fieldView is TextView) {
                fieldView.text.toString()
            } else {
                val textView = fieldView.findViewById<TextView>(android.R.id.text1)
                textView?.text?.toString() ?: ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun setupViewModel() {
        databaseHelper = ProfileDatabaseHelper(requireContext())
        val factory = ProfileViewModelFactory(databaseHelper)
        viewModel = ViewModelProvider(requireActivity(), factory).get(ProfileViewModel::class.java)

        viewModel.profile.observe(viewLifecycleOwner) { profile ->
            profile?.let {
                updateUI(it)
            }
        }
    }

    private fun setupClickListeners() {
        imgAvatar.setOnClickListener {
            showImagePickerDialog()
        }

        fieldFirst.setOnClickListener { showEditDialog("First Name", currentFirstName) }
        fieldLast.setOnClickListener { showEditDialog("Last Name", currentLastName) }
        fieldPhone.setOnClickListener { showEditDialog("Phone", currentPhone) }
        fieldAbout.setOnClickListener { showEditDialog("About", currentAbout) }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")

        AlertDialog.Builder(requireContext())
            .setTitle("Change Profile Picture")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndOpen()
                    1 -> pickImageLauncher.launch("image/*")
                    2 -> Toast.makeText(context, "Canceled", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        val photoFile = createImageFile()
        photoFile?.let {
            currentPhotoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                it
            )
            takePhotoLauncher.launch(currentPhotoUri)
        }
    }

    private fun createImageFile(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(null)
        return try {
            File.createTempFile(
                "JPEG_${timeStamp}_",
                ".jpg",
                storageDir
            )
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to create image file", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun saveAvatarUri(uri: Uri) {
        val currentProfile = viewModel.profile.value ?: return
        val updatedProfile = currentProfile.copy(avatarUri = uri.toString())
        viewModel.updateProfile(updatedProfile)
    }

    private fun updateUI(profile: Profile) {
        tvUsername.text = profile.username

        currentFirstName = profile.firstName.ifEmpty { "First Name" }
        currentLastName = profile.lastName.ifEmpty { "Last Name" }
        currentPhone = profile.phone.ifEmpty { "Phone Number" }
        currentAbout = profile.about.ifEmpty { "About You" }

        // Load avatar if exists
        if (profile.avatarUri.isNotEmpty()) {
            try {
                val uri = Uri.parse(profile.avatarUri)
                imgAvatar.setImageURI(uri)
            } catch (e: Exception) {
                imgAvatar.setImageResource(android.R.drawable.ic_menu_camera)
            }
        } else {
            imgAvatar.setImageResource(android.R.drawable.ic_menu_camera)
        }

        // Update field displays
        updateFieldText(fieldFirst, "First Name: $currentFirstName")
        updateFieldText(fieldLast, "Last Name: $currentLastName")
        updateFieldText(fieldPhone, "Phone: $currentPhone")
        updateFieldText(fieldAbout, "About: $currentAbout")
    }

    private fun showEditDialog(fieldName: String, currentValue: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_text, null)
        val etInput = dialogView.findViewById<EditText>(R.id.etInput)
        etInput.setText(currentValue)
        etInput.hint = "Enter $fieldName"

        AlertDialog.Builder(requireContext())
            .setTitle("Edit $fieldName")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newValue = etInput.text.toString().trim()
                if (newValue.isNotEmpty()) {
                    saveField(fieldName, newValue)
                } else {
                    Toast.makeText(context, "Please enter a value", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveField(fieldName: String, newValue: String) {
        val currentProfile = viewModel.profile.value ?: return

        val updatedProfile = when (fieldName) {
            "First Name" -> {
                currentFirstName = newValue
                currentProfile.copy(
                    firstName = newValue,
                    username = "$newValue $currentLastName".trim()
                )
            }
            "Last Name" -> {
                currentLastName = newValue
                currentProfile.copy(
                    lastName = newValue,
                    username = "$currentFirstName $newValue".trim()
                )
            }
            "Phone" -> {
                currentPhone = newValue
                currentProfile.copy(phone = newValue)
            }
            "About" -> {
                currentAbout = newValue
                currentProfile.copy(about = newValue)
            }
            else -> currentProfile
        }

        viewModel.updateProfile(updatedProfile)
        Toast.makeText(context, "$fieldName updated", Toast.LENGTH_SHORT).show()
    }
}