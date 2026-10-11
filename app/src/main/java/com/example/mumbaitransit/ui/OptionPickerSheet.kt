package com.example.mumbaitransit.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mumbaitransit.databinding.ItemPickerOptionBinding
import com.example.mumbaitransit.databinding.SheetOptionPickerBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * A searchable list of anything.
 *
 * Used for trains, stations and platforms alike — the caller supplies the rows
 * already filtered to the room, so this class never needs to know which line it
 * is showing.
 */
class OptionPickerSheet : BottomSheetDialogFragment() {

    /** [key] is what gets stored, [title]/[subtitle] are what the user reads. */
    data class Option(
        val key: String,
        val title: String,
        val subtitle: String? = null,
        val tag: String? = null
    )

    private var _binding: SheetOptionPickerBinding? = null
    private val binding get() = _binding!!

    var sheetTitle: String = ""
    var sheetSubtitle: String = ""
    var options: List<Option> = emptyList()
    var onPicked: ((Option) -> Unit)? = null

    private lateinit var adapter: OptionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?
    ): View {
        _binding = SheetOptionPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, saved: Bundle?) {
        binding.tvPickerTitle.text = sheetTitle
        binding.tvPickerSubtitle.text = sheetSubtitle

        adapter = OptionAdapter { option ->
            onPicked?.invoke(option)
            dismiss()
        }
        binding.rvPickerOptions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPickerOptions.adapter = adapter
        apply(options)

        binding.etPickerSearch.doAfterTextChanged { text ->
            val q = text?.toString()?.trim().orEmpty()
            apply(
                if (q.isEmpty()) options
                else options.filter {
                    it.title.contains(q, true) || it.subtitle?.contains(q, true) == true
                }
            )
        }
    }

    private fun apply(list: List<Option>) {
        adapter.submit(list)
        val empty = list.isEmpty()
        binding.tvPickerEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        binding.rvPickerOptions.visibility = if (empty) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "OptionPickerSheet"
    }
}

private class OptionAdapter(
    private val onClick: (OptionPickerSheet.Option) -> Unit
) : RecyclerView.Adapter<OptionAdapter.VH>() {

    private var items: List<OptionPickerSheet.Option> = emptyList()

    fun submit(list: List<OptionPickerSheet.Option>) {
        items = list
        notifyDataSetChanged()
    }

    class VH(val b: ItemPickerOptionBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemPickerOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.b.tvOptionTitle.text = item.title
        holder.b.tvOptionSubtitle.apply {
            visibility = if (item.subtitle.isNullOrBlank()) View.GONE else View.VISIBLE
            text = item.subtitle
        }
        holder.b.tvOptionTag.apply {
            visibility = if (item.tag.isNullOrBlank()) View.GONE else View.VISIBLE
            text = item.tag
        }
        holder.b.root.setOnClickListener { onClick(item) }
    }
}
