package com.example.myapplication.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.repository.RecentPdfRepository
import com.example.myapplication.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var recentPdfViewModel: RecentPdfViewModel
    private lateinit var recentPdfAdapter: RecentPdfAdapter

    private val pickPdfLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                takeReadPermission(uri)

                val name = getFileName(uri)
                recentPdfViewModel.addRecentPdf(uri.toString(), name)

                val bundle = Bundle().apply {
                    putString("pdfUri", uri.toString())
                }

                findNavController().navigate(
                    R.id.action_homeFragment_to_pdfFragment,
                    bundle
                )
            } else {
                Toast.makeText(requireContext(), "No PDF selected", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecentPdfViewModel()
        setupRecentRecycler()
        observeRecentPdfs()
        setupClicks()
    }

    private fun setupRecentPdfViewModel() {
        val dao = AppDatabase.getInstance(requireContext()).recentPdfDao()
        val repository = RecentPdfRepository(dao)
        val factory = RecentPdfViewModelFactory(repository)
        recentPdfViewModel = ViewModelProvider(this, factory)[RecentPdfViewModel::class.java]
    }

    private fun setupRecentRecycler() {
        recentPdfAdapter = RecentPdfAdapter(
            onClick = { pdf ->
                openPdfFragment(pdf.uri)
            },
            onDelete = { pdf ->
                recentPdfViewModel.deleteByUri(pdf.uri)
            }
        )

        binding.rvRecentPdfs.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecentPdfs.adapter = recentPdfAdapter
    }

    private fun observeRecentPdfs() {
        recentPdfViewModel.recentPdfs.observe(viewLifecycleOwner) { list ->
            recentPdfAdapter.submitList(list)

            val isEmpty = list.isEmpty()

            binding.tvEmptyRecent.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.btnOpenLastPdf.isEnabled = !isEmpty
            binding.btnOpenLastPdf.alpha = if (isEmpty) 0.5f else 1f
            binding.tvLastPdfName.text = if (isEmpty) "No recent PDF yet" else list.first().name
        }
    }

    private fun setupClicks() {
        binding.btnOpenPdf.setOnClickListener {
            pickPdfLauncher.launch(arrayOf("application/pdf"))
        }

        binding.btnNotes.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_notesFragment)
        }

        binding.btnGpa.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_gpaFragment)
        }

        binding.btnOpenLastPdf.setOnClickListener {
            val list = recentPdfViewModel.recentPdfs.value
            if (!list.isNullOrEmpty()) {
                openPdfFragment(list.first().uri)
            } else {
                Toast.makeText(requireContext(), "No recent PDF found", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnClearRecent.setOnClickListener {
            recentPdfViewModel.clearAll()
        }
    }

    private fun openPdfFragment(uriString: String) {
        val bundle = Bundle().apply {
            putString("pdfUri", uriString)
        }
        findNavController().navigate(
            R.id.action_homeFragment_to_pdfFragment,
            bundle
        )
    }

    private fun takeReadPermission(uri: Uri) {
        try {
            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
        }
    }

    private fun getFileName(uri: Uri): String {
        var result = "PDF File"

        if (uri.scheme == "content") {
            requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && index != -1) {
                    result = cursor.getString(index)
                }
            }
        } else {
            val path = uri.path
            if (!path.isNullOrEmpty()) {
                result = path.substringAfterLast('/')
            }
        }

        return result
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}