package com.hankkin.reading.ui.tools

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import com.hankkin.library.fuct.android.CaptureActivity
import com.hankkin.library.fuct.bean.ZxingConfig
import com.hankkin.library.utils.SPUtils
import com.hankkin.reading.R
import com.hankkin.reading.adapter.ToolsAdapter
import com.hankkin.reading.base.BaseMvpFragment
import com.hankkin.reading.common.Constant
import com.hankkin.reading.domain.ToolsBean
import com.hankkin.reading.domain.Weatherbean
import com.hankkin.reading.domain.WordNoteBean
import com.hankkin.reading.event.EventMap
import com.hankkin.reading.dao.DaoFactory
import com.hankkin.reading.ui.home.articledetail.CommonWebActivity
import com.hankkin.reading.ui.tools.acount.AccountListActivity
import com.hankkin.reading.ui.tools.acount.LockSetActivity
import com.hankkin.reading.ui.tools.translate.TranslateActivity
import com.hankkin.reading.ui.tools.wordnote.WordNoteActivity
import com.hankkin.reading.ui.tools.wordnote.WordNoteDaoContract
import com.hankkin.reading.utils.LoadingUtils
import com.hankkin.reading.utils.ThemeHelper
import com.hankkin.reading.utils.ViewHelper
import com.hankkin.reading.utils.WeatherUtils
import kotlinx.android.synthetic.main.fragment_word.*
import kotlinx.android.synthetic.main.layout_word_every.*
import kotlinx.android.synthetic.main.layout_word_no_data.*
import java.util.*


/**
 * Created by huanghaijie on 2018/5/15.
 */
class ToolsFragment : BaseMvpFragment<ToolsContract.IPresenter>(), ToolsContract.IView {
    val REQUEST_CODE_SCAN = 0x1

    private lateinit var mData: MutableList<ToolsBean>

    private lateinit var mToolsAdapter: ToolsAdapter

    private var mWords: MutableList<WordNoteBean>? = null


    override fun registerPresenter() = ToolsPresenter::class.java

    override fun isHasBus(): Boolean {
        return true
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_word
    }

    override fun initView() {
        tv_translate_weather.text = "正在获取天气..."
        et_tools_search.setOnEditorActionListener(object : TextView.OnEditorActionListener {
            override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
                when (actionId) {
                    EditorInfo.IME_ACTION_SEARCH -> {
                        TranslateActivity.intentTo(context, et_tools_search.text.toString())
                        et_tools_search.setText("")
                    }
                }
                return false
            }
        })
        tv_word_go.setOnClickListener { startActivity(Intent(context, TranslateActivity::class.java)) }
        tv_word_note.setOnClickListener { startActivity(Intent(context, WordNoteActivity::class.java)) }
        tv_word_next.setOnClickListener {
            ViewHelper.startShakeAnim(card_word_every)
            setEveryWord()
        }
    }


    override fun initData() {
        addData()
        rv_tools.layoutManager = GridLayoutManager(context, 4)
        mToolsAdapter = ToolsAdapter()
        mToolsAdapter.addAll(mData)
        rv_tools.adapter = mToolsAdapter
        mToolsAdapter.setOnItemClickListener { t, position ->
            when (t.id) {
                Constant.TOOLS.ID_KUAIDI -> startActivity(Intent(context, KuaiDiActivity::class.java))
                Constant.TOOLS.ID_ABOUT -> context?.let { ViewHelper.showAboutDialog(it) }
                Constant.TOOLS.ID_JUEJIN -> context?.let { CommonWebActivity.loadUrl(it, Constant.AboutUrl.JUEJIN, Constant.AboutUrl.JUEJIN_TITLE) }
                Constant.TOOLS.ID_WORD -> startActivity(Intent(context, TranslateActivity::class.java))
                Constant.TOOLS.ID_WORD_NOTE -> startActivity(Intent(context, WordNoteActivity::class.java))
                Constant.TOOLS.ID_PWD_NOTE -> {
                    if (SPUtils.getInt(Constant.SP_KEY.LOCK_OPEN) != 0){
                        startActivity(Intent(context, LockSetActivity::class.java))
                    }
                    else{
                        startActivity(Intent(context,AccountListActivity::class.java))
                    }
                }
                Constant.TOOLS.ID_SAOYISAO -> {
                    val intent = Intent(context, CaptureActivity::class.java)
                    val bundle = Bundle()
                    val config = ZxingConfig()
                    config.reactColor = ThemeHelper.getCurrentColor(context)
                    bundle.putSerializable(com.hankkin.library.fuct.common.Constant.INTENT_ZXING_CONFIG, config)
                    intent.putExtras(bundle)
                    startActivityForResult(intent, REQUEST_CODE_SCAN)
                }
            }
        }
        setEveryWord()
        getPresenter().getWeather("beijing")
    }

    private fun addData() {
        mData = mutableListOf<ToolsBean>(
                ToolsBean(Constant.TOOLS.ID_KUAIDI, activity!!.resources.getString(R.string.work_kuaidi), R.mipmap.icon_kuaidi),
                ToolsBean(Constant.TOOLS.ID_SAOYISAO, activity!!.resources.getString(R.string.work_sao), R.mipmap.icon_saoyisao),
                ToolsBean(Constant.TOOLS.ID_WORD, activity!!.resources.getString(R.string.work_word), R.mipmap.icon_word),
                ToolsBean(Constant.TOOLS.ID_WORD_NOTE, activity!!.resources.getString(R.string.work_word_note), R.mipmap.icon_wrod_note),
//                ToolsBean(Constant.TOOLS.ID_MOVIE, activity!!.resources.getString(R.string.work_movie), R.mipmap.icon_dianying),
//                ToolsBean(Constant.TOOLS.ID_MUSIC, activity!!.resources.getString(R.string.work_music), R.mipmap.icon_music),
//                ToolsBean(Constant.TOOLS.ID_WEATHER, activity!!.resources.getString(R.string.work_weather), R.mipmap.icon_weather),
                ToolsBean(Constant.TOOLS.ID_PWD_NOTE, activity!!.resources.getString(R.string.work_pwd_note), R.mipmap.icon_pwd_tools),
//                ToolsBean(Constant.TOOLS.ID_NEWS, activity!!.resources.getString(R.string.work_news), R.mipmap.icon_computer),
                ToolsBean(Constant.TOOLS.ID_JUEJIN, activity!!.resources.getString(R.string.work_juejin), R.mipmap.icon_juejin),
                ToolsBean(Constant.TOOLS.ID_ABOUT, activity!!.resources.getString(R.string.work_about), R.mipmap.icon_about)
        )
    }

    fun getWords() = DaoFactory.getProtocol(WordNoteDaoContract::class.java).queryEmphasisWord()

    private fun setEveryWord() {
        mWords = getWords()
        if (mWords != null && mWords!!.size > 0) {
            layout_word_every.visibility = View.VISIBLE
            layout_word_no_data.visibility = View.GONE
            val word = mWords!!.get(Random().nextInt(mWords!!.size))
            tv_word_key.text = word.translateBean.query
            tv_word_content.text = word.translateBean.explains.toString()
            tv_word_phoneic.text = "/${word.translateBean.phonetic}/"
        } else {
            layout_word_every.visibility = View.GONE
            layout_word_no_data.visibility = View.VISIBLE
        }
    }

    override fun setWeather(weatherbean: Weatherbean) {
        val now = weatherbean.results[0].now
        val format = resources.getString(R.string.format_weather)
        tv_translate_weather.text = String.format(format, now.text, now.temperature)
        iv_translate_weather.setImageResource(WeatherUtils.getWeatherImg(now.code, context))
    }

    override fun setWeatherError() {
        tv_translate_weather.text = "获取天气失败"
    }

    override fun hideLoading() {
        LoadingUtils.hideLoading()
    }

    override fun showLoading() {
        LoadingUtils.showLoading(context)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SCAN) {
            if (resultCode == Activity.RESULT_OK) {
                val url = data!!.getStringExtra(com.hankkin.library.fuct.common.Constant.CODED_CONTENT)
                context?.let { CommonWebActivity.loadUrl(it, url, "扫描结果") }
            }
        }
    }

    override fun onEvent(event: EventMap.BaseEvent) {
        if (event is EventMap.UpdateEveryEvent){
            setEveryWord()
        }
    }

}

