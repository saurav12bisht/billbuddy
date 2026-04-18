package com.mobile.fingram.ui.summary

import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.fragment.app.viewModels
import com.mobile.fingram.databinding.FragmentSummaryBinding
import com.mobile.fingram.ui.common.BaseFragment
import com.mobile.fingram.ui.common.util.AdManager
import com.mobile.fingram.ui.common.util.CurrencyFormatter
import kotlinx.coroutines.launch
import com.mobile.fingram.data.export.ExportImportManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SummaryFragment : BaseFragment<FragmentSummaryBinding>(FragmentSummaryBinding::inflate) {

    private val viewModel: SummaryViewModel by viewModels()

    override fun onInit() {
        // Handle window insets for edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        viewModel.balance.observe(viewLifecycleOwner) { b ->
            binding.tvTotalOutstanding.text = "Total Outstanding: ${CurrencyFormatter.formatPaiseToRupee(b.netOutstanding)}"
        }
        viewModel.customersWithBalance.observe(viewLifecycleOwner) { list ->
            binding.tvCustomerCount.text = "Total Customers: ${list.size}"
        }
        binding.btnExport.setOnClickListener {
            AdManager.showInterstitialBeforeExport(requireActivity()) {
                viewModel.exportSummary(requireContext())
            }
        }

        // Observe export result
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.exportResult.collect { result: Result<ExportImportManager.ExportResult>? ->
                result?.let { res ->
                    res.fold(
                        onSuccess = { exportResult: ExportImportManager.ExportResult ->
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(android.content.Intent.EXTRA_STREAM, exportResult.shareUri)
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            startActivity(android.content.Intent.createChooser(shareIntent, "Share CSV file"))
                            
                            val msg = if (exportResult.savedPath != null) {
                                "Exported successfully!\nSaved to: ${exportResult.savedPath}"
                            } else {
                                "Exported successfully!"
                            }
                            android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_LONG).show()
                        },
                        onFailure = { error: Throwable ->
                            android.widget.Toast.makeText(
                                requireContext(),
                                "Export failed: ${error.message}",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                }
            }
        }
    }
}
