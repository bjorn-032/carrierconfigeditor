package gay.nullby.carriername

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.PersistableBundle
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyFrameworkInitializer
import android.telephony.TelephonyManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.android.internal.telephony.ICarrierConfigLoader
import gay.nullby.carriername.databinding.FragmentTargetBinding
import rikka.shizuku.ShizukuBinderWrapper

class TargetFragment : Fragment() {
    private val TAG: String = "TargetFragment"

    private var _binding: FragmentTargetBinding? = null

    private val binding get() = _binding!!

    private var subId1: Int = -1;
    private var subId2: Int = -1;

    private var selectedSub: Int = 1;

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentTargetBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var _subId1: IntArray? = SubscriptionManager.getSubId(0);
        var _subId2: IntArray? = SubscriptionManager.getSubId(1);

        Log.d(TAG, "#onViewCreated(): subId1=$subId1 subId2=$subId2")

        if (_subId1 != null) {
            subId1 = _subId1[0]
            view.findViewById<RadioButton>(R.id.sub1_button).text = "Network 1 (carrier: ${getCarrierNameBySubId(subId1)})"
        }
        if (_subId2 != null) {
            subId2 = _subId2[0]
            view.findViewById<RadioButton>(R.id.sub2_button).text = "Network 2 (carrier: ${getCarrierNameBySubId(subId2)})"
        }

        if (subId2 == -1) {
            view.findViewById<View>(R.id.sub2_button).visibility = View.GONE
        }

        view.findViewById<Button>(R.id.button_set).setOnClickListener { onSetName(view.findViewById<EditText>(R.id.text_entry).text.toString()) }

        view.findViewById<Button>(R.id.button_reset).setOnClickListener {
            onResetName()
            view.findViewById<EditText>(R.id.text_entry).setText("")
        }

        view.findViewById<RadioGroup>(R.id.sub_selection).setOnCheckedChangeListener { _, checkedId -> onSelectSub(checkedId) }

        onSelectSub(0)
    }

    private fun getPermission(): Boolean {
        
    }

    private fun getCarrierConfig(key: String): String {
        return false;
    }

    private fun onSetName(text: String) {
        Toast.makeText(context, "Set carrier vanity name to \"$text\"", Toast.LENGTH_SHORT).show()



        val telephonyManager = context!!.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

        if (this.context?.let {
                ActivityCompat.checkSelfPermission(
                    it,
                    Manifest.permission.READ_PHONE_STATE
                )
            } != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
        }
        if (telephonyManager != null) {
            println(telephonyManager.carrierConfig.getBoolean(CarrierConfigManager.KEY_CARRIER_CONFIG_VERSION_STRING))
        };








        var p = PersistableBundle();
//        p.putBoolean(CarrierConfigManager.KEY_CARRIER_NAME_OVERRIDE_BOOL, true)
//        p.putString(CarrierConfigManager.KEY_CARRIER_NAME_STRING, text)

        // See T-Mobile NL as roaming
//        val stringArray = arrayOf("20416")
//        p.putStringArray(CarrierConfigManager.KEY_GSM_ROAMING_NETWORKS_STRING_ARRAY, stringArray)
        p.putBoolean(CarrierConfigManager.KEY_FORCE_HOME_NETWORK_BOOL, false)
        p.putBoolean(CarrierConfigManager.KEY_PREFER_2G_BOOL, true)
//        p.putBoolean(CarrierConfigManager.KEY_CARRIER_VT_AVAILABLE_BOOL, false) // disable video-calling
        p.putString(CarrierConfigManager.KEY_CARRIER_CONFIG_VERSION_STRING,":3")

        /*
            Show only the PLMN and not the SPN.

            Example:
            Telekom.de - Orange F
              (PLMN)      (SPN)

            PLMN = Public Land Mobile Network (HPLMN = Home PLMN)
            SPN = Service Provider Name

            -1 = default
            0 = PLMN no, SPN no (Will show "No service")
            1 = PLMN yes, SPN no
            2 = PLMN no, SPN yes
            3 = PLMN yes, SPN yes

        */
        p.putInt(CarrierConfigManager.KEY_SPN_DISPLAY_CONDITION_OVERRIDE_INT, 1)
        val subId: Int;
        if (selectedSub == 1) {
            subId = subId1!!
        } else {
            subId = subId2!!
        }
        overrideCarrierConfig(subId, p)
    }

    private fun onResetName() {
        var p = PersistableBundle();
        p.putBoolean(CarrierConfigManager.KEY_CARRIER_NAME_OVERRIDE_BOOL, false)
        p.putString(CarrierConfigManager.KEY_CARRIER_NAME_STRING, "")
        val subId: Int;
        if (selectedSub == 1) {
            subId = subId1!!
        } else {
            subId = subId2!!
        }
        // Sometimes just setting the override to null doesn't work, so let's first set another override, disabling the name change
        overrideCarrierConfig(subId, p)
        overrideCarrierConfig(subId, null)
    }

    private fun onSelectSub(id: Int) {
        if (id == R.id.sub1_button || id == 0) {
            selectedSub = 1;
            Toast.makeText(context, "Selected Network 1", Toast.LENGTH_SHORT).show()
        } else if (id == R.id.sub2_button) {
            selectedSub = 2;
            Toast.makeText(context, "Selected Network 2", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCarrierNameBySubId(subId: Int): String {
        val telephonyManager = context!!.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            ?: return ""

        return telephonyManager.getNetworkOperatorName(subId)
    }

    private fun overrideCarrierConfig(subId: Int, p: PersistableBundle?) {
        val carrierConfigLoader = ICarrierConfigLoader.Stub.asInterface(
            ShizukuBinderWrapper(
                TelephonyFrameworkInitializer
                    .getTelephonyServiceManager()
                    .carrierConfigServiceRegisterer
                    .get()
            )
        )
        carrierConfigLoader.overrideConfig(subId, p, true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}