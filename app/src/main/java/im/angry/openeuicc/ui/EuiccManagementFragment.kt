package im.angry.openeuicc.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import im.angry.openeuicc.R
import im.angry.openeuicc.databinding.EuiccProfileBinding
import im.angry.openeuicc.databinding.FragmentEuiccBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EuiccManagementFragment : Fragment(), EuiccFragmentMarker, EuiccProfilesChangedListener {
    companion object {
        fun newInstance(slotId: Int): EuiccManagementFragment =
            newInstanceEuicc(EuiccManagementFragment::class.java, slotId)
    }

    private var _binding: FragmentEuiccBinding? = null
    private val binding get() = _binding!!

    private val adapter = EuiccProfileAdapter(listOf())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEuiccBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.swipeRefresh.setOnRefreshListener { refresh() }
        binding.profileList.adapter = adapter
        binding.profileList.layoutManager =
            LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)

        binding.fab.setOnClickListener {
            ProfileDownloadFragment.newInstance(slotId)
                .show(childFragmentManager, ProfileDownloadFragment.TAG)
        }
    }

    override fun onStart() {
        super.onStart()
        refresh()
    }

    override fun onEuiccProfilesChanged() {
        refresh()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refresh() {
        binding.swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            val profiles = withContext(Dispatchers.IO) {
                channel.lpa.profiles
            }

            withContext(Dispatchers.Main) {
                adapter.profiles = profiles.filter { it["PROFILE_CLASS"] != "0" }
                adapter.notifyDataSetChanged()
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    inner class ViewHolder(val binding: EuiccProfileBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.iccid.setOnClickListener {
                if (binding.iccid.transformationMethod == null) {
                    binding.iccid.transformationMethod = PasswordTransformationMethod.getInstance()
                } else {
                    binding.iccid.transformationMethod = null
                }
            }
        }
    }

    inner class EuiccProfileAdapter(var profiles: List<Map<String, String>>) : RecyclerView.Adapter<ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding =
                EuiccProfileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            // TODO: The library is not exposing the nicknames. Expose them so that we can do something here.
            holder.binding.name.text = profiles[position]["NAME"]
            holder.binding.state.setText(
                if (profiles[position]["STATE"]?.lowercase() == "enabled") {
                    R.string.enabled
                } else {
                    R.string.disabled
                }
            )
            holder.binding.provider.text = profiles[position]["PROVIDER_NAME"]
            holder.binding.iccid.text = profiles[position]["ICCID"]
            holder.binding.iccid.transformationMethod = PasswordTransformationMethod.getInstance()
        }

        override fun getItemCount(): Int = profiles.size
    }
}