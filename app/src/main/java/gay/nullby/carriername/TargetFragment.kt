package gay.nullby.carriername

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.android.internal.telephony.ICarrierConfigLoader
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import gay.nullby.carriername.databinding.FragmentTargetBinding
import rikka.shizuku.ShizukuBinderWrapper
import java.io.InputStream


class TargetFragment : Fragment() {
    private val TAG: String = "TargetFragment"

    private var _binding: FragmentTargetBinding? = null

    private val binding get() = _binding!!

    private var subId1: Int = -1;
    private var subId2: Int = -1;

    private var selectedSub: Int = 1;
    val PICK_XML_FILE_REQUEST_CODE = 1

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

        loadSwitches(view)

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

        view.findViewById<LinearLayout>(R.id.list_spn).setOnClickListener{
            val items = arrayOf(
                "default",
                "PLMN no, SPN no (No service)",
                "PLMN yes, SPN no (Telekom.de)",
                "PLMN no, SPN yes (Orange F)",
                "PLMN yes, SPN yes (Telekom.de - Orange F)")

            var spn = getCarrierConfigInt(CarrierConfigManager.KEY_SPN_DISPLAY_CONDITION_OVERRIDE_INT)
            if (spn == null){
                spn = -1
            }
            var checkedItem = spn + 1
            context?.let { it1 ->
                MaterialAlertDialogBuilder(it1)
                    .setTitle("SPN display condition")
                    .setSingleChoiceItems(items, checkedItem) { _, which ->
                        checkedItem = which
                    }
                    .setNeutralButton("Cancel") { _, _ -> /* Do nothing */ }
                    .setPositiveButton("Save") { _, _ ->
                        setCarrierConfigInt(CarrierConfigManager.KEY_SPN_DISPLAY_CONDITION_OVERRIDE_INT, checkedItem - 1)
                    }
                    .show()
            }
        }

        view.findViewById<LinearLayout>(R.id.list_upload).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.setType("text/xml")
            startActivityForResult(intent, PICK_XML_FILE_REQUEST_CODE)
        }

        view.findViewById<LinearLayout>(R.id.list_view).setOnClickListener {
            val nonroam = getCarrierConfigStringArray(CarrierConfigManager.KEY_NON_ROAMING_OPERATOR_STRING_ARRAY)?.joinToString(",")
            val roam = getCarrierConfigStringArray(CarrierConfigManager.KEY_ROAMING_OPERATOR_STRING_ARRAY)?.joinToString(",")
            println("Non roaming " + nonroam)
            println("Roaming " + roam)

            context?.let { it1 ->
                MaterialAlertDialogBuilder(it1)
                    .setTitle("Currently loaded roaming config")
                    .setMessage("Non roaming: $nonroam\nRoaming: $roam")
                    .setPositiveButton("Close") { _, _ ->
                        // Do nothing
                    }
                    .show()
            }
        }

        onSelectSub(0)
    }

    private fun loadSwitches(view: View) {
        // Handle the switches
        val forceHomeNetwork = getCarrierConfigBoolean(CarrierConfigManager.KEY_FORCE_HOME_NETWORK_BOOL)
        val carrierAggregation = getCarrierConfigBoolean(CarrierConfigManager.KEY_HIDE_LTE_PLUS_DATA_ICON_BOOL)
        val videoCalling = getCarrierConfigBoolean(CarrierConfigManager.KEY_CARRIER_VT_AVAILABLE_BOOL)
        val prefer2G = getCarrierConfigBoolean(CarrierConfigManager.KEY_PREFER_2G_BOOL)
        val roamingFromServiceState = getCarrierConfigBoolean(CarrierConfigManager.KEY_SPN_DISPLAY_RULE_USE_ROAMING_FROM_SERVICE_STATE_BOOL)
        println(roamingFromServiceState)
        if(forceHomeNetwork != null){ view.findViewById<MaterialSwitch>(R.id.switch_force_home_network).isChecked = forceHomeNetwork }
        if(carrierAggregation != null){ view.findViewById<MaterialSwitch>(R.id.switch_carrier_aggregation).isChecked = carrierAggregation }
        if(videoCalling != null){ view.findViewById<MaterialSwitch>(R.id.switch_video_calling).isChecked = videoCalling }
        if(prefer2G != null){ view.findViewById<MaterialSwitch>(R.id.switch_prefer_2g).isChecked = prefer2G }
        if(roamingFromServiceState != null){ view.findViewById<MaterialSwitch>(R.id.switch_roaming_for_service_state).isChecked = roamingFromServiceState }

        view.findViewById<MaterialSwitch>(R.id.switch_force_home_network).setOnCheckedChangeListener {_, isChecked ->
            setCarrierConfigBoolean(CarrierConfigManager.KEY_FORCE_HOME_NETWORK_BOOL, isChecked)
        }
        view.findViewById<MaterialSwitch>(R.id.switch_carrier_aggregation).setOnCheckedChangeListener {_, isChecked ->
            setCarrierConfigBoolean(CarrierConfigManager.KEY_HIDE_LTE_PLUS_DATA_ICON_BOOL, isChecked)
        }
        view.findViewById<MaterialSwitch>(R.id.switch_video_calling).setOnCheckedChangeListener {_, isChecked ->
            setCarrierConfigBoolean(CarrierConfigManager.KEY_CARRIER_VT_AVAILABLE_BOOL, isChecked)
        }
        view.findViewById<MaterialSwitch>(R.id.switch_prefer_2g).setOnCheckedChangeListener {_, isChecked ->
            setCarrierConfigBoolean(CarrierConfigManager.KEY_PREFER_2G_BOOL, isChecked)
        }
        view.findViewById<MaterialSwitch>(R.id.switch_roaming_for_service_state).setOnCheckedChangeListener {_, isChecked ->
            setCarrierConfigBoolean(CarrierConfigManager.KEY_SPN_DISPLAY_RULE_USE_ROAMING_FROM_SERVICE_STATE_BOOL, isChecked)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PICK_XML_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                val uri = data.data
                // Handle the selected XML file URI
                processXmlFile(uri)
            }
        }
    }

    private fun processXmlFile(uri: Uri?) {
        try {
            uri?.let {
                val inputStream: InputStream? = context?.contentResolver?.openInputStream(it)
                var xmlparser = XMLParser()
                if (inputStream != null) {
                    var xml = xmlparser.parse(inputStream)
                    val nonroam: Array<String> = xml.nonroam.items.toArray(arrayOfNulls<String>(0))
                    val roam: Array<String> = xml.roam.items.toArray(arrayOfNulls<String>(0))
                    setCarrierConfigStringArray(CarrierConfigManager.KEY_NON_ROAMING_OPERATOR_STRING_ARRAY, nonroam)
                    setCarrierConfigStringArray(CarrierConfigManager.KEY_ROAMING_OPERATOR_STRING_ARRAY, roam)
                } else {
                    println("Error: no InputStream from file")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun getPermission(): Boolean {
        val hasPermission = ActivityCompat.checkSelfPermission(this.context!!, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            // Permission is not granted
            // Request the permission or handle the case where permission is denied
            ActivityCompat.requestPermissions(this.activity!!, arrayOf(Manifest.permission.READ_PHONE_STATE), 1)
            return ActivityCompat.checkSelfPermission(this.context!!, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        }
        return true;
    }

    @SuppressLint("MissingPermission")
    private fun getCarrierConfigString(key: String): String? {
        if(getPermission()){
            val telephonyManager = context!!.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            return telephonyManager?.carrierConfig?.getString(key);
        }
        return null
    }

    @SuppressLint("MissingPermission")
    private fun getCarrierConfigStringArray(key: String): Array<String>? {
        if(getPermission()){
            val telephonyManager = context!!.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            return telephonyManager?.carrierConfig?.getStringArray(key);
        }
        return null
    }

    @SuppressLint("MissingPermission")
    private fun getCarrierConfigInt(key: String): Int? {
        if(getPermission()){
            val telephonyManager = context!!.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            return telephonyManager?.carrierConfig?.getInt(key);
        }
        return null
    }

    @SuppressLint("MissingPermission")
    private fun getCarrierConfigBoolean(key: String): Boolean? {
        if(getPermission()){
            val telephonyManager = context!!.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            return telephonyManager?.carrierConfig?.getBoolean(key);
        }
        return null
    }

    private fun setCarrierConfigBoolean(key: String, bool: Boolean){
        var p = PersistableBundle();
        p.putBoolean(key, bool)
        p.putString(CarrierConfigManager.KEY_CARRIER_CONFIG_VERSION_STRING,":3")
        val subId: Int;
        if (selectedSub == 1) {
            subId = subId1
        } else {
            subId = subId2
        }
        overrideCarrierConfig(subId, p)
    }

    private fun setCarrierConfigStringArray(key: String, stringArray: Array<String>){
        var p = PersistableBundle();
        p.putStringArray(key, stringArray)
        p.putString(CarrierConfigManager.KEY_CARRIER_CONFIG_VERSION_STRING,":3")
        val subId: Int;
        if (selectedSub == 1) {
            subId = subId1
        } else {
            subId = subId2
        }
        overrideCarrierConfig(subId, p)
    }

    private fun setCarrierConfigInt(key: String, num: Int){
        var p = PersistableBundle();
        p.putInt(key, num)
        p.putString(CarrierConfigManager.KEY_CARRIER_CONFIG_VERSION_STRING,":3")
        val subId: Int;
        if (selectedSub == 1) {
            subId = subId1
        } else {
            subId = subId2
        }
        overrideCarrierConfig(subId, p)
    }

    private fun onSetName(text: String) {
        Toast.makeText(context, "Set carrier vanity name to \"$text\"", Toast.LENGTH_SHORT).show()
        var p = PersistableBundle();
        p.putBoolean(CarrierConfigManager.KEY_CARRIER_NAME_OVERRIDE_BOOL, true)
        p.putString(CarrierConfigManager.KEY_CARRIER_NAME_STRING, text)
        // See T-Mobile NL as roaming
//        val stringArray = arrayOf("20416")
//        p.putStringArray(CarrierConfigManager.KEY_GSM_ROAMING_NETWORKS_STRING_ARRAY, stringArray)
        p.putString(CarrierConfigManager.KEY_CARRIER_CONFIG_VERSION_STRING,":3")
        val subId: Int;
        if (selectedSub == 1) {
            subId = subId1
        } else {
            subId = subId2
        }
        overrideCarrierConfig(subId, p)
    }

    private fun onResetName() {
        var p = PersistableBundle();
        p.putBoolean(CarrierConfigManager.KEY_CARRIER_NAME_OVERRIDE_BOOL, false)
        p.putString(CarrierConfigManager.KEY_CARRIER_NAME_STRING, "")
        val subId: Int;
        if (selectedSub == 1) {
            subId = subId1
        } else {
            subId = subId2
        }
        // Sometimes just setting the override to null doesn't work, so let's first set another override, disabling the name change
        overrideCarrierConfig(subId, p)
        overrideCarrierConfig(subId, null)
        this.view?.let { loadSwitches(it) }
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