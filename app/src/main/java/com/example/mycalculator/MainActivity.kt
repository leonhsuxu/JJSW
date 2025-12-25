package com.pkg.jjsw

import android.graphics.Color
// A. 读取设置参数
val woodP = findViewById<EditText>(R.id.set_wood).text.toString().toDoubleOrNull() ?: 3500.0
val branP = findViewById<EditText>(R.id.set_bran).text.toString().toDoubleOrNull() ?: 220.0
val soyP = findViewById<EditText>(R.id.set_soy).text.toString().toDoubleOrNull() ?: 87.0
val calcP = findViewById<EditText>(R.id.set_calc).text.toString().toDoubleOrNull() ?: 20.0
val limeP = findViewById<EditText>(R.id.set_lime).text.toString().toDoubleOrNull() ?: 7.0
val bagUnitG = findViewById<EditText>(R.id.set_bag).text.toString().toDoubleOrNull() ?: 3.6
val perFurnaceStd = findViewById<EditText>(R.id.set_per_f).text.toString().toDoubleOrNull() ?: 12096.0

// B. 读取每日录入数据
val bins = findViewById<EditText>(R.id.et_bins).text.toString().toDoubleOrNull() ?: 0.0
val fCount = findViewById<EditText>(R.id.et_f_count).text.toString().toDoubleOrNull() ?: 0.0
val tCount = findViewById<EditText>(R.id.et_t_count).text.toString().toDoubleOrNull() ?: 0.0
val defect = findViewById<EditText>(R.id.et_defect).text.toString().toDoubleOrNull() ?: 0.0
val returned = findViewById<EditText>(R.id.et_returned).text.toString().toDoubleOrNull() ?: 0.0

// C. 计算产量逻辑
val furnaceTotal = (fCount * perFurnaceStd) + tCount
val productionQty = if (findViewById<RadioButton>(R.id.rb_m1).isChecked) {
    furnaceTotal + defect  // 方式1：入炉+不良
} else {
    etManual.text.toString().toDoubleOrNull() ?: 0.0 // 方式2：手填
}
val baggingQty = productionQty - defect - returned
val yieldRate = if (productionQty > 0) (baggingQty / productionQty) * 100 else 0.0

// D. 构建彩色加粗报表
val builder = SpannableStringBuilder()

// 辅助函数：添加一行，标签黑，数字彩色加粗
fun appendColoredRow(label: String, value: String, color: String) {
    builder.append(label)
    val start = builder.length
    builder.append(value)
    val end = builder.length
    // 设置颜色
    builder.setSpan(ForegroundColorSpan(Color.parseColor(color)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    // 设置加粗
    builder.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    builder.append("\n")
}

// 1-5项：产量与质量 (蓝色或红色)
appendColoredRow("装袋数量：      ", "${baggingQty.toInt()}", "#1565C0") // 蓝色
appendColoredRow("生产数量：      ", "${productionQty.toInt()}", "#1565C0")
appendColoredRow("当班不良数量：  ", "${defect.toInt()}", "#D32F2F") // 红色
appendColoredRow("后工序退回数量：", "${returned.toInt()}", "#D32F2F")

val yieldColor = if (yieldRate < 98.5) "#D32F2F" else "#1565C0"
appendColoredRow("良率：          ", String.format(Locale.getDefault(), "%.2f%%", yieldRate), yieldColor)

builder.append("----------------------------\n")

// 6-11项：材料用量 (绿色)
val woodTotal = woodP * bins
val branTotal = branP * bins
val soyTotal = soyP * bins
val calcTotal = calcP * bins
val limeTotal = limeP * bins
val bagTotalKg = (productionQty * bagUnitG) / 1000.0

appendColoredRow("木屑用量：      ", "${woodTotal.toInt()} kg", "#2E7D32") // 绿色
appendColoredRow("麦麸用量：      ", "${branTotal.toInt()} kg", "#2E7D32")
appendColoredRow("豆粕用量：      ", "${soyTotal.toInt()} kg", "#2E7D32")
appendColoredRow("轻钙用量：      ", "${calcTotal.toInt()} kg", "#2E7D32")
appendColoredRow("石灰用量：      ", "${limeTotal.toInt()} kg", "#2E7D32")
appendColoredRow("菌袋用量：      ", String.format(Locale.getDefault(), "%.2f kg", bagTotalKg), "#2E7D32")

// 最终赋值显示
tvReport.text = builder
}
}
}
