package com.pkg.jjsw

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val PREFS_NAME = "WorkshopParams"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // --- 1. 视图绑定：折叠布局控件 ---
        val layoutToggleHeader = findViewById<RelativeLayout>(R.id.layout_toggle_header)
        val layoutParamsContent = findViewById<LinearLayout>(R.id.layout_params_content)
        val tvExpandIcon = findViewById<TextView>(R.id.tv_expand_icon)

        // --- 2. 视图绑定：参数与输入控件 ---
        val btnLockToggle = findViewById<Button>(R.id.btn_lock_toggle)
        val etWoodParam = findViewById<EditText>(R.id.set_wood)
        val etBranParam = findViewById<EditText>(R.id.set_bran)
        val etSoyParam = findViewById<EditText>(R.id.set_soy)
        val etCalcParam = findViewById<EditText>(R.id.set_calc)
        val etLimeParam = findViewById<EditText>(R.id.set_lime)
        val etBagGParam = findViewById<EditText>(R.id.set_bag)
        val etStdPerFull = findViewById<EditText>(R.id.set_per_f)

        val etBinsInput = findViewById<EditText>(R.id.et_bins)
        val etFullKilns = findViewById<EditText>(R.id.et_f_count)
        val etTailBags = findViewById<EditText>(R.id.et_t_count)

        // 补料控件
        val tvLabelExtraWood = findViewById<TextView>(R.id.tv_label_extra_wood)
        val etExtraWood = findViewById<EditText>(R.id.et_extra_wood)
        val tvLabelExtraLime = findViewById<TextView>(R.id.tv_label_extra_lime)
        val etExtraLime = findViewById<EditText>(R.id.et_extra_lime)

        val etReturnedBags = findViewById<EditText>(R.id.et_returned)
        val etCrushedKgInput = findViewById<EditText>(R.id.et_crushed_kg)
        val etBagLossPcs = findViewById<EditText>(R.id.et_bag_loss_pcs)
        val etFloorWasteKg = findViewById<EditText>(R.id.et_floor_waste_kg)

        val rgMode = findViewById<RadioGroup>(R.id.rg_mode)
        val tvManualLabel = findViewById<TextView>(R.id.tv_manual_label)
        val etManual = findViewById<EditText>(R.id.et_manual)
        val btnCalc = findViewById<Button>(R.id.btn_calc)
        val tvReport = findViewById<TextView>(R.id.tv_report)

        // --- 3. 持久化数据加载 ---
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        etWoodParam.setText(sharedPref.getString("wood", "3500"))
        etBranParam.setText(sharedPref.getString("bran", "220"))
        etSoyParam.setText(sharedPref.getString("soy", "87"))
        etCalcParam.setText(sharedPref.getString("calc", "20"))
        etLimeParam.setText(sharedPref.getString("lime", "7"))
        etBagGParam.setText(sharedPref.getString("bag", "3.6"))
        etStdPerFull.setText(sharedPref.getString("std", "12096"))

        // --- 4. 修复：折叠/展开点击逻辑 ---
        layoutToggleHeader.setOnClickListener {
            if (layoutParamsContent.visibility == View.VISIBLE) {
                layoutParamsContent.visibility = View.GONE
                tvExpandIcon.text = "展开 ▼"
            } else {
                layoutParamsContent.visibility = View.VISIBLE
                tvExpandIcon.text = "收起 ▲"
            }
        }

        // --- 5. 补料点击显示逻辑 ---
        tvLabelExtraWood.setOnClickListener {
            etExtraWood.visibility = if (etExtraWood.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            if (etExtraWood.visibility == View.VISIBLE) etExtraWood.requestFocus() else etExtraWood.setText("")
        }
        tvLabelExtraLime.setOnClickListener {
            etExtraLime.visibility = if (etExtraLime.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            if (etExtraLime.visibility == View.VISIBLE) etExtraLime.requestFocus() else etExtraLime.setText("")
        }

        // --- 6. 锁定/解锁参数逻辑 ---
        var isLocked = true
        btnLockToggle.setOnClickListener {
            isLocked = !isLocked
            val params = listOf(etWoodParam, etBranParam, etSoyParam, etCalcParam, etLimeParam, etBagGParam, etStdPerFull)
            params.forEach { it.isEnabled = !isLocked }
            if (isLocked) {
                sharedPref.edit().putString("wood", etWoodParam.text.toString()).putString("bran", etBranParam.text.toString())
                    .putString("soy", etSoyParam.text.toString()).putString("calc", etCalcParam.text.toString())
                    .putString("lime", etLimeParam.text.toString()).putString("bag", etBagGParam.text.toString())
                    .putString("std", etStdPerFull.text.toString()).apply()
                btnLockToggle.text = "🔓 解锁参数"
                btnLockToggle.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#95A5A6"))
                Toast.makeText(this, "✅ 参数已保存", Toast.LENGTH_SHORT).show()
            } else {
                btnLockToggle.text = "🔒 锁定并保存"
                btnLockToggle.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E74C3C"))
            }
        }

        // --- 7. 计算模式切换 ---
        rgMode.setOnCheckedChangeListener { _, id ->
            tvManualLabel.text = if (id == R.id.rb_m1) "手动清点当班不良品(pcs)：" else "装袋机计数总和："
        }

        // --- 8. 核心计算逻辑 ---
        btnCalc.setOnClickListener {
            try {
                val pWood = etWoodParam.text.toString().toDoubleOrNull() ?: 0.0
                val pBran = etBranParam.text.toString().toDoubleOrNull() ?: 0.0
                val pSoy = etSoyParam.text.toString().toDoubleOrNull() ?: 0.0
                val pCalc = etCalcParam.text.toString().toDoubleOrNull() ?: 0.0
                val pLime = etLimeParam.text.toString().toDoubleOrNull() ?: 0.0
                val pBagG = etBagGParam.text.toString().toDoubleOrNull() ?: 0.0
                val pStd = etStdPerFull.text.toString().toIntOrNull() ?: 12096

                val bins = etBinsInput.text.toString().toDoubleOrNull() ?: 0.0
                val fKilns = etFullKilns.text.toString().toIntOrNull() ?: 0
                val tail = etTailBags.text.toString().toIntOrNull() ?: 0
                val exWood = etExtraWood.text.toString().toDoubleOrNull() ?: 0.0
                val exLime = etExtraLime.text.toString().toDoubleOrNull() ?: 0.0
                val returned = etReturnedBags.text.toString().toIntOrNull() ?: 0
                val crushedKg = etCrushedKgInput.text.toString().toDoubleOrNull() ?: 0.0
                val manualVal = etManual.text.toString().toIntOrNull() ?: 0
                val lossPcs = etBagLossPcs.text.toString().toIntOrNull() ?: 0
                val wasteKg = etFloorWasteKg.text.toString().toDoubleOrNull() ?: 0.0

                val inOvenQty = (fKilns * pStd) + tail
                val prodTotal = if (rgMode.checkedRadioButtonId == R.id.rb_m1) inOvenQty + manualVal else manualVal
                val defectQty = prodTotal - inOvenQty
                val yieldRate = if (prodTotal > 0) (inOvenQty - returned).toDouble() / prodTotal * 100 else 0.0

                val totalDry = pWood + pBran + pSoy + pCalc + pLime
                val fWood = (bins * pWood) - (crushedKg * (pWood/totalDry)) + exWood
                val fBran = (bins * pBran) - (crushedKg * (pBran/totalDry))
                val fSoy = (bins * pSoy) - (crushedKg * (pSoy/totalDry))
                val fLime = (bins * pLime) + exLime
                val fBagsKg = ((prodTotal + returned + lossPcs) * pBagG) / 1000.0

                val sb = SpannableStringBuilder("━━━━━━━━━━━━━━━━━━━━━━\n📊 生产日报表\n━━━━━━━━━━━━━━━━━━━━━━\n\n")
                sb.append("装袋机计数总和："); val t1 = sb.length; sb.append("$prodTotal\n")
                sb.setSpan(StyleSpan(Typeface.BOLD), t1, sb.length-1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.append("装袋数量：${inOvenQty - returned}\n当班不良品：")
                val dStart = sb.length; sb.append("$defectQty\n")
                sb.setSpan(ForegroundColorSpan(Color.RED), dStart, sb.length-1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.append("良率：${String.format("%.2f", yieldRate)}%\n入锅数量：$inOvenQty\n")
                sb.append("\n━━━━━━━━━━━━━━━━━━━━━━\n📦 原料用量统计\n━━━━━━━━━━━━━━━━━━━━━━\n\n")
                sb.append("木屑用量：${String.format("%.2f", fWood)} kg\n")
                sb.append("麦麸用量：${String.format("%.2f", fBran)} kg\n")
                sb.append("豆粕用量：${String.format("%.2f", fSoy)} kg\n")
                sb.append("轻钙用量：${String.format("%.2f", bins * pCalc)} kg\n")
                sb.append("石灰用量：${String.format("%.2f", fLime)} kg\n")
                sb.append("菌袋用量：${String.format("%.3f", fBagsKg)} kg\n")
                if(lossPcs > 0) sb.append("  (含调机耗损：$lossPcs pcs)\n")
                if(wasteKg > 0) sb.append("\n⚠️ 落地脏污损耗：$wasteKg kg\n")
                sb.append("━━━━━━━━━━━━━━━━━━━━━━\n")
                tvReport.text = sb
            } catch (e: Exception) { tvReport.text = "计算出错：${e.message}" }
        }
    }
}
