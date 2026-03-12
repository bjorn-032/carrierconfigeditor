package dev.fireants.carrierconfig

import android.app.Instrumentation
import android.os.Bundle
import android.os.PersistableBundle
import android.telephony.TelephonyFrameworkInitializer
import android.util.Log
import com.android.internal.telephony.ICarrierConfigLoader
import rikka.shizuku.ShizukuBinderWrapper

class CarrierConfigInstrumentation : Instrumentation() {

    private val TAG = "CarrierConfigInstr"

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)

        val result = Bundle()
        try {
            val subIdStr = arguments?.getString("subId")
                ?: throw IllegalArgumentException("Missing 'subId' argument")
            val subId = subIdStr.toInt()

            val bundleStr = arguments.getString("bundle")
                ?: throw IllegalArgumentException("Missing 'bundle' argument")

            val persistableBundle: PersistableBundle? = if (bundleStr == "null") {
                null
            } else {
                deserializeBundle(bundleStr)
            }

            Log.d(TAG, "Calling overrideConfig for subId=$subId bundle=${persistableBundle?.keySet()}")

            val carrierConfigLoader = ICarrierConfigLoader.Stub.asInterface(
                ShizukuBinderWrapper(
                    TelephonyFrameworkInitializer
                        .getTelephonyServiceManager()
                        .carrierConfigServiceRegisterer
                        .get()
                )
            )
            carrierConfigLoader.overrideConfig(subId, persistableBundle, false)

            result.putString("result", "success")
            Log.d(TAG, "overrideConfig succeeded")

        } catch (e: Exception) {
            Log.e(TAG, "overrideConfig failed in instrumentation: ${e.message}", e)
            result.putString("result", "error")
            result.putString("error", e.message)
        }

        finish(0, result)
    }

    private fun deserializeBundle(encoded: String): PersistableBundle {
        val bundle = PersistableBundle()
        val input = encoded.replace("_SPACE_", " ")
        val parts = splitUnescaped(input, ',')

        for (part in parts) {
            if (part.isBlank()) continue
            val eqIdx = part.indexOf('=')
            if (eqIdx < 0) {
                Log.w(TAG, "Skipping malformed bundle part: $part")
                continue
            }
            val key = part.substring(0, eqIdx)
            val rest = part.substring(eqIdx + 1)

            val colonIdx = rest.indexOf(':')
            if (colonIdx < 0) {
                Log.w(TAG, "Skipping malformed type:value for key $key")
                continue
            }
            val type = rest.substring(0, colonIdx)
            val value = rest.substring(colonIdx + 1)

            when (type) {
                "bool" -> bundle.putBoolean(key, value.toBooleanStrict())
                "int"  -> bundle.putInt(key, value.toInt())
                "string" -> bundle.putString(
                    key,
                    value.replace("\\,", ",").replace("\\=", "=")
                )
                "stringArray" -> {
                    val items = splitUnescaped(value, '|')
                        .map { it.replace("\\|", "|") }
                        .toTypedArray()
                    bundle.putStringArray(key, items)
                }
                else -> Log.w(TAG, "Unknown type '$type' for key $key, skipping")
            }
        }
        return bundle
    }

    private fun splitUnescaped(input: String, delimiter: Char): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (c == '\\' && i + 1 < input.length && input[i + 1] == delimiter) {
                current.append('\\')
                current.append(delimiter)
                i += 2
            } else if (c == delimiter) {
                parts.add(current.toString())
                current.clear()
                i++
            } else {
                current.append(c)
                i++
            }
        }
        parts.add(current.toString())
        return parts
    }
}
