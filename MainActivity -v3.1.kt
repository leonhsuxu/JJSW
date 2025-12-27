package com.pkg.jjsw
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.*
import android.text.style.*
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private val PREFS_NAME = "WorkshopV3"
    private var isSalaryLocked = true
    private var isLeaveMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // --- 1. 基础视图绑定 ---
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
        val etReturned = findViewById<EditText>(R.id.et_returned)
        val etCrushedKg = findViewById<EditText>(R.id.et_crushed_kg)
        val etBagLoss = findViewById<EditText>(R.id.et_bag_loss_pcs)
        val etWasteKg = findViewById<EditText>(R.id.et_floor_waste_kg)
        val tvLabelExWood = findViewById<TextView>(R.id.tv_label_extra_wood)
        val etExWood = findViewById<EditText>(R.id.et_extra_wood)
        val tvLabelExLime = findViewById<TextView>(R.id.tv_label_extra_lime)
        val etExLime = findViewById<EditText>(R.id.et_extra_lime)
        val rgMode = findViewById<RadioGroup>(R.id.rg_mode)
        val etManual = findViewById<EditText>(R.id.et_manual)
        val btnCalc = findViewById<Button>(R.id.btn_calc)
        val tvReport = findViewById<TextView>(R.id.tv_report)

        val containerEmployees = findViewById<LinearLayout>(R.id.container_employees)
        val btnAddEmp = findViewById<Button>(R.id.btn_add_employee)
        val btnLockSalary = findViewById<Button>(R.id.btn_lock_salary)
        val btnLeaveToggle = findViewById<Button>(R.id.btn_leave_toggle)
        val layoutSalaryContent = findViewById<LinearLayout>(R.id.layout_salary_content)
        val layoutSalaryHeader = findViewById<RelativeLayout>(R.id.layout_salary_header)

        // --- 2. 参数加载与折叠逻辑 ---
        loadParams(listOf(etWoodParam, etBranParam, etSoyParam, etCalcParam, etLimeParam, etBagGParam, etStdPerFull))

        findViewById<LinearLayout>(R.id.layout_params_content).visibility = View.GONE
        findViewById<TextView>(R.id.tv_expand_icon).text = "展开 ▼"
        layoutSalaryContent.visibility = View.GONE
        findViewById<TextView>(R.id.tv_salary_expand_icon).text = "展开 ▼"

        findViewById<RelativeLayout>(R.id.layout_toggle_header).setOnClickListener {
            val content = findViewById<LinearLayout>(R.id.layout_params_content)
            val icon = findViewById<TextView>(R.id.tv_expand_icon)
            content.visibility = if (content.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            icon.text = if (content.visibility == View.VISIBLE) "收起 ▲" else "展开 ▼"
        }

        layoutSalaryHeader.setOnClickListener {
            val icon = findViewById<TextView>(R.id.tv_salary_expand_icon)
            layoutSalaryContent.visibility = if (layoutSalaryContent.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            icon.text = if (layoutSalaryContent.visibility == View.VISIBLE) "收起 ▲" else "展开 ▼"
        }

        // --- 3. 补料与模式切换 ---
        tvLabelExWood.setOnClickListener { toggleEx(etExWood) }
        tvLabelExLime.setOnClickListener { toggleEx(etExLime) }
        rgMode.setOnCheckedChangeListener { _, id ->
            findViewById<TextView>(R.id.tv_manual_label).text = if(id == R.id.rb_m1) "当班不良品数(pcs):" else "生产数量:"
        }

        btnLockToggle.setOnClickListener {
            toggleLockParams(it as Button, listOf(etWoodParam, etBranParam, etSoyParam, etCalcParam, etLimeParam, etBagGParam, etStdPerFull))
        }

        // --- 4. 工资系统核心逻辑 ---
        loadEmployees(containerEmployees)
        btnAddEmp.setOnClickListener { addEmployeeRow(containerEmployees) }

        btnLeaveToggle.setOnClickListener {
            isLeaveMode = !isLeaveMode
            updateLeaveModeUI(containerEmployees, btnLeaveToggle)
        }

        btnLockSalary.setOnClickListener {
            isSalaryLocked = !isSalaryLocked
            updateSalaryUIState(containerEmployees, btnLockSalary)
            if(isSalaryLocked) saveEmployees(containerEmployees)
        }

        // --- 5. 计算生成报表（V3粉料岗位+背景色对齐） ---
        btnCalc.setOnClickListener {
            try {
                val p = readDoubles(etWoodParam, etBranParam, etSoyParam, etCalcParam, etLimeParam, etBagGParam, etStdPerFull)
                val bins = etBinsInput.text.toString().toDoubleOrNull() ?: 0.0
                val fK = etFullKilns.text.toString().toIntOrNull() ?: 0
                val tail = etTailBags.text.toString().toIntOrNull() ?: 0
                val ret = etReturned.text.toString().toIntOrNull() ?: 0
                val cru = etCrushedKg.text.toString().toDoubleOrNull() ?: 0.0
                val manual = etManual.text.toString().toIntOrNull() ?: 0
                val exW = etExWood.text.toString().toDoubleOrNull() ?: 0.0
                val exL = etExLime.text.toString().toDoubleOrNull() ?: 0.0
                val lossPcs = etBagLoss.text.toString().toIntOrNull() ?: 0
                val wasteKg = etWasteKg.text.toString().toDoubleOrNull() ?: 0.0

                val inOven = (fK * p[6].toInt()) + tail
                val prodTotal = if(rgMode.checkedRadioButtonId == R.id.rb_m1) inOven + manual else manual
                val defect = prodTotal - inOven
                val bagged = inOven - ret
                val rate = if(prodTotal > 0) bagged.toDouble() / prodTotal * 100 else 0.0

                val totalD = p[0]+p[1]+p[2]+p[3]+p[4]
                val fW = (bins * p[0]) - (cru * (p[0]/totalD)) + exW
                val fB = (bins * p[1]) - (cru * (p[1]/totalD))
                val fS = (bins * p[2]) - (cru * (p[2]/totalD))
                val fC = bins * p[3]
                val fL = (bins * p[4]) + exL
                val fBagKg = ((prodTotal + ret + lossPcs) * p[5]) / 1000.0

                val sb = SpannableStringBuilder()
                sb.append("━━━━━━━━━━━━━━━━━━━━━━\n")
                sb.append("📊 生产日报表\n")
                sb.append("━━━━━━━━━━━━━━━━━━━━━━\n\n")

                sb.append(formatReportLine("生产数量", "$prodTotal", true))
                sb.append(formatReportLine("装袋数量", "$bagged", false))
                sb.append(formatReportLine("当班不良品", "$defect", false, Color.RED))
                sb.append(formatReportLine("二车间退回", "$ret", false, Color.RED))
                sb.append(formatReportLine("良率", "${String.format("%.2f", rate)}%", false))
                sb.append("\n")
                sb.append(formatReportLine("入锅数量", "$inOven", false, Color.parseColor("#2E7D32")))

                sb.append("\n\n━━━━━━━━━━━━━━━━━━━━━━\n")
                sb.append("📦 原料用量统计\n")
                sb.append("━━━━━━━━━━━━━━━━━━━━━━\n\n")
                sb.append("木屑用量：${String.format("%.2f", fW)} kg\n")
                sb.append("麦麸用量：${String.format("%.2f", fB)} kg\n")
                sb.append("豆粕用量：${String.format("%.2f", fS)} kg\n")
                sb.append("轻钙用量：${String.format("%.2f", fC)} kg\n")
                sb.append("石灰用量：${String.format("%.2f", fL)} kg\n")
                sb.append("菌袋用量：${String.format("%.3f", fBagKg)} kg")
                if(lossPcs > 0) sb.append("\n  (含调机耗损菌袋：$lossPcs pcs)")
                if(wasteKg > 0) sb.append("\n\n⚠️ 落地脏污损耗原料：$wasteKg kg")

                // V3工资计算（含粉料岗位+背景色对齐）
                sb.append("\n\n━━━━━━━━━━━━━━━━━━━━━━\n")
                sb.append("💰 员工计件工资明细\n")
                sb.append("━━━━━━━━━━━━━━━━━━━━━━\n\n")

                val headerStart = sb.length
                sb.append(formatSalaryRowV3("姓名", "单价", "计件", "平摊", "总计", true))
                sb.setSpan(StyleSpan(Typeface.BOLD), headerStart, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                val headerText = formatSalaryRowV3("姓名", "单价", "计件", "平摊", "总计", true)
                sb.append("\n" + "─".repeat(headerText.length) + "\n")

                val allEmps = getEmployeeDataV3(containerEmployees)
                val powderEmps = allEmps.filter { it.getString("pos").trim().equals("粉料", ignoreCase = true) && !it.getBoolean("onLeave") }
                val normalEmps = allEmps.filter { !it.getString("pos").trim().equals("粉料", ignoreCase = true) && !it.getBoolean("onLeave") }
                val leaveEmps = allEmps.filter { it.getBoolean("onLeave") }

                val leaveTotalSalary = leaveEmps.filter { !it.getString("pos").trim().equals("粉料", ignoreCase = true) }
                    .sumOf { it.getDouble("price") * bagged.toDouble() }

                val sharePerPerson = if(normalEmps.isNotEmpty()) leaveTotalSalary / normalEmps.size else 0.0
                var totalActualSalary = 0.0

                // 普通岗位（背景色统一长度）
                normalEmps.forEachIndexed { index, jo ->
                    val name = jo.getString("name")
                    val price = jo.optDouble("price", 0.0)
                    val pieceSalary = price * bagged.toDouble()
                    val share = sharePerPerson
                    val total = pieceSalary + share
                    totalActualSalary += total

                    val c1 = padChinese(name, 4)
                    val c2 = padChinese(String.format("%.5f", price), 4)
                    val c3 = padChinese(String.format("%.2f", pieceSalary), 4)
                    val c4 = padChinese(String.format("%.2f", share), 3)
                    val c5 = String.format("%.2f", total)

                    val rowStart = sb.length
                    sb.append("$c1 $c2 ")

                    val pieceStart = sb.length
                    sb.append(c3)
                    sb.setSpan(ForegroundColorSpan(Color.parseColor("#1976D2")), pieceStart, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    sb.append(" ")

                    val shareStart = sb.length
                    sb.append(c4)
                    sb.setSpan(ForegroundColorSpan(Color.parseColor("#FF6F00")), shareStart, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    sb.append(" ")

                    val totalStart = sb.length
                    sb.append(c5)
                    sb.setSpan(ForegroundColorSpan(Color.parseColor("#2E7D32")), totalStart, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    sb.setSpan(StyleSpan(Typeface.BOLD), totalStart, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                    // ⭐ 补齐到统一长度
                    val currentLength = sb.length - rowStart
                    val targetLength = headerText.length
                    if (currentLength < targetLength) {
                        sb.append(" ".repeat(targetLength - currentLength))
                    }

                    sb.append("\n")

                    // ⭐ 背景色覆盖整行
                    if (index % 2 == 1) {
                        sb.setSpan(BackgroundColorSpan(Color.parseColor("#F0F0F0")), rowStart, sb.length - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }

                // 粉料岗位（独立计算+浅紫背景统一长度）
                if(powderEmps.isNotEmpty()) {
                    sb.append("\n")

                    powderEmps.forEach { jo ->
                        val name = jo.getString("name")
                        val price = jo.optDouble("price", 0.0)
                        val powderSalary = price * fS
                        totalActualSalary += powderSalary

                        val c1 = padChinese("🌀$name", 4)
                        val c2 = padChinese(String.format("%.5f", price), 4)
                        val c3 = padChinese(String.format("%.2f", powderSalary), 4)
                        val c4 = padChinese("-", 3)
                        val c5 = String.format("%.2f", powderSalary)

                        val rowStart = sb.length
                        sb.append("$c1 $c2 ")

                        val powderStart = sb.length
                        sb.append(c3)
                        sb.setSpan(ForegroundColorSpan(Color.parseColor("#9C27B0")), powderStart, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        sb.append(" ")

                        val dashStart = sb.length
                        sb.append(c4)
                        sb.setSpan(ForegroundColorSpan(Color.parseColor("#9E9E9E")), dashStart, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        sb.append(" ")

                        val totalStart = sb.length
                        sb.append(c5)
                        sb.setSpan(ForegroundColorSpan(Color.parseColor("#9C27B0")), totalStart, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        sb.setSpan(StyleSpan(Typeface.BOLD), totalStart, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                        // ⭐ 补齐到统一长度
                        val currentLength = sb.length - rowStart
                        val targetLength = headerText.length
                        if (currentLength < targetLength) {
                            sb.append(" ".repeat(targetLength - currentLength))
                        }

                        sb.append("\n")

                        // ⭐ 浅紫背景覆盖整行
                        sb.setSpan(BackgroundColorSpan(Color.parseColor("#F3E5F5")), rowStart, sb.length - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }

                // 请假人员
                if(leaveEmps.isNotEmpty()) {
                    sb.append("\n")
                    val leaveStart = sb.length
                    val leaveNames = leaveEmps.joinToString("、") { it.getString("name") }
                    sb.append("请假：$leaveNames")
                    sb.setSpan(ForegroundColorSpan(Color.parseColor("#9E9E9E")), leaveStart, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    sb.setSpan(StyleSpan(Typeface.ITALIC), leaveStart, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    sb.append("\n")
                }

                sb.append("\n")

                // ⭐ V3新增：计件总和（不含粉料岗位）
                val normalTotalSalary = normalEmps.sumOf { jo ->
                    val price = jo.optDouble("price", 0.0)
                    val pieceSalary = price * bagged.toDouble()
                    val share = sharePerPerson
                    pieceSalary + share
                }

                val normalStart = sb.length
                sb.append("当日计件总和(不含粉料)：${String.format("%.2f", normalTotalSalary)} 元")
                sb.setSpan(RelativeSizeSpan(1.2f), normalStart, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.setSpan(StyleSpan(Typeface.BOLD), normalStart, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.setSpan(ForegroundColorSpan(Color.parseColor("#00897B")), normalStart, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.append("\n")

                // 当日实发总和（含所有岗位）
                val totalStart = sb.length
                sb.append("当日工资总和(含粉料)：${String.format("%.2f", totalActualSalary)} 元")
                sb.setSpan(RelativeSizeSpan(1.2f), totalStart, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.setSpan(StyleSpan(Typeface.BOLD), totalStart, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.setSpan(ForegroundColorSpan(Color.parseColor("#1976D2")), totalStart, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                sb.append("\n\n━━━━━━━━━━━━━━━━━━━━━━\n")
                tvReport.text = sb

            } catch (e: Exception) {
                Toast.makeText(this, "数据有误：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ========== 格式化函数 ==========
    private fun formatSalaryRowV3(col1: String, col2: String, col3: String, col4: String, col5: String, isHeader: Boolean): String {
        val c1 = padChinese(col1, 4)
        val c2 = padChinese(col2, 4)
        val c3 = padChinese(col3, 4)
        val c4 = padChinese(col4, 3)
        val c5 = col5
        return "$c1 $c2 $c3 $c4 $c5"
    }

    private fun formatReportLine(label: String, value: String, bold: Boolean, color: Int? = null): SpannableStringBuilder {
        val sb = SpannableStringBuilder()
        val labelWithColon = if(!label.endsWith("：") && !label.endsWith(":")) label + "：" else label
        val labelPadded = padChinese(labelWithColon, 6) + "  "
        sb.append(labelPadded)
        val valueStart = sb.length
        sb.append(value)
        sb.append("\n")
        if(bold) sb.setSpan(StyleSpan(Typeface.BOLD), valueStart, sb.length - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        if(color != null) {
            sb.setSpan(ForegroundColorSpan(color), valueStart, sb.length - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.setSpan(StyleSpan(Typeface.BOLD), valueStart, sb.length - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return sb
    }

    private fun padChinese(str: String, targetWidth: Int): String {
        var width = 0
        for (char in str) {
            width += if (char.code > 128) 2 else 1
        }
        val paddingNeeded = targetWidth * 2 - width
        return str + "　".repeat(Math.max(0, paddingNeeded / 2)) + " ".repeat(paddingNeeded % 2)
    }

    // ========== 工资管理辅助函数 ==========
    private fun addEmployeeRow(container: LinearLayout, name: String = "", pos: String = "", price: String = "", onLeave: Boolean = false) {
        val row = LayoutInflater.from(this).inflate(R.layout.item_employee_row, null)
        val etName = row.findViewById<EditText>(R.id.et_emp_name)
        val etPos = row.findViewById<EditText>(R.id.et_emp_pos)
        val etPrice = row.findViewById<EditText>(R.id.et_emp_price)
        val cbLeave = row.findViewById<CheckBox>(R.id.cb_leave)
        val btnDel = row.findViewById<ImageButton>(R.id.btn_del_emp)

        etName.setText(name)
        etPos.setText(pos)
        etPrice.setText(price)
        cbLeave.isChecked = onLeave

        etName.isEnabled = !isSalaryLocked
        etPos.isEnabled = !isSalaryLocked
        etPrice.isEnabled = !isSalaryLocked
        cbLeave.visibility = if(isLeaveMode) View.VISIBLE else View.GONE
        btnDel.visibility = if(isSalaryLocked) View.GONE else View.VISIBLE

        btnDel.setOnClickListener {
            container.removeView(row)
            saveEmployees(container)
        }

        container.addView(row)
    }

    private fun updateLeaveModeUI(container: LinearLayout, btn: Button) {
        btn.text = if(isLeaveMode) "✅ 完成选择" else "🏖️ 请假管理"
        btn.backgroundTintList = ColorStateList.valueOf(Color.parseColor(if(isLeaveMode) "#4CAF50" else "#FF9800"))

        for (i in 0 until container.childCount) {
            val cb = container.getChildAt(i).findViewById<CheckBox>(R.id.cb_leave)
            cb.visibility = if(isLeaveMode) View.VISIBLE else View.GONE
        }

        if(!isLeaveMode) saveEmployees(container)
    }

    private fun updateSalaryUIState(container: LinearLayout, btn: Button) {
        btn.text = if (isSalaryLocked) "🔓 解锁修改" else "🔒 锁定保存"
        btn.backgroundTintList = ColorStateList.valueOf(Color.parseColor(if(isSalaryLocked) "#90A4AE" else "#D84315"))

        for (i in 0 until container.childCount) {
            val v = container.getChildAt(i)
            v.findViewById<View>(R.id.et_emp_name).isEnabled = !isSalaryLocked
            v.findViewById<View>(R.id.et_emp_pos).isEnabled = !isSalaryLocked
            v.findViewById<View>(R.id.et_emp_price).isEnabled = !isSalaryLocked
            v.findViewById<View>(R.id.btn_del_emp).visibility = if(isSalaryLocked) View.GONE else View.VISIBLE
        }
    }

    private fun saveEmployees(container: LinearLayout) {
        val array = JSONArray()
        for (i in 0 until container.childCount) {
            val v = container.getChildAt(i)
            val jo = JSONObject()
            jo.put("name", v.findViewById<EditText>(R.id.et_emp_name).text.toString())
            jo.put("pos", v.findViewById<EditText>(R.id.et_emp_pos).text.toString())
            jo.put("price", v.findViewById<EditText>(R.id.et_emp_price).text.toString())
            jo.put("onLeave", v.findViewById<CheckBox>(R.id.cb_leave).isChecked)
            array.put(jo)
        }
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString("emps", array.toString()).apply()
    }

    private fun loadEmployees(container: LinearLayout) {
        val raw = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString("emps", "[]")
        val array = JSONArray(raw)
        for(i in 0 until array.length()){
            val o = array.getJSONObject(i)
            addEmployeeRow(
                container,
                o.getString("name"),
                o.getString("pos"),
                o.getString("price"),
                o.optBoolean("onLeave", false)
            )
        }
    }

    private fun getEmployeeDataV3(container: LinearLayout): List<JSONObject> {
        val list = mutableListOf<JSONObject>()
        for (i in 0 until container.childCount) {
            val v = container.getChildAt(i)
            val name = v.findViewById<EditText>(R.id.et_emp_name).text.toString().trim()
            val priceStr = v.findViewById<EditText>(R.id.et_emp_price).text.toString().trim()
            val price = priceStr.toDoubleOrNull() ?: 0.0

            if (name.isEmpty() || priceStr.isEmpty() || price == 0.0) {
                continue
            }

            val jo = JSONObject()
            jo.put("name", name)
            jo.put("pos", v.findViewById<EditText>(R.id.et_emp_pos).text.toString())
            jo.put("price", price)
            jo.put("onLeave", v.findViewById<CheckBox>(R.id.cb_leave).isChecked)
            list.add(jo)
        }
        return list
    }

    // ========== 辅助工具函数 ==========
    private fun toggleEx(et: EditText) {
        et.visibility = if(et.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        if(et.visibility == View.GONE) et.setText("")
    }

    private fun toggleLockParams(btn: Button, ets: List<EditText>) {
        val locked = btn.text.contains("解锁")
        ets.forEach { it.isEnabled = locked }
        btn.text = if(locked) "🔒 锁定并保存" else "🔓 解锁参数"
        btn.backgroundTintList = ColorStateList.valueOf(Color.parseColor(if(locked) "#E74C3C" else "#95A5A6"))
        if(!locked) saveParams(ets)
    }

    private fun saveParams(ets: List<EditText>) {
        val keys = listOf("wood","bran","soy","calc","lime","bag","std")
        val pref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        ets.forEachIndexed { i, et -> pref.putString(keys[i], et.text.toString()) }
        pref.apply()
    }

    private fun loadParams(ets: List<EditText>) {
        val keys = listOf("wood","bran","soy","calc","lime","bag","std")
        val def = listOf("3500","220","87","20","7","3.6","12096")
        val pref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        ets.forEachIndexed { i, et -> et.setText(pref.getString(keys[i], def[i])) }
    }

    private fun readDoubles(vararg ets: EditText) = ets.map { it.text.toString().toDoubleOrNull() ?: 0.0 }
}
