package com.yh.timingqr

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import com.yh.timingqrviewlib.TimingQRView
import kotlinx.android.synthetic.main.act_main.*
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


/**
 * Created by CYH on 2019-09-29 14:28
 */
class MainAct : Activity(), TimingQRView.ITimingListener, TimingQRView.IClickListener {

    private val REPOS_URL = "https://api.github.com/users/clistery/repos"
    private var mAllRepos: JSONArray? = null

    private val mTime = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.act_main)

        val wm = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val dm = DisplayMetrics()
        wm?.defaultDisplay?.getMetrics(dm)
        mTimingQrView.layoutParams.apply {
            val size = (dm.widthPixels * 0.53).toInt()
            this@apply.width = size
            this@apply.height = size
        }
        mTimingQrView.setTimingListener(this)
        mTimingQrView.setOnClickListener(this)

        showLoading()
        Thread(Runnable {
            try {
                val url = URL(REPOS_URL)
                val connect = url.openConnection() as HttpURLConnection
                connect.requestMethod = "GET"
                connect.connectTimeout = 5000
                val bis =
                    BufferedReader(InputStreamReader(if (connect.responseCode in 200..299) connect.inputStream else connect.errorStream))
                val response = StringBuilder()
                val responses = bis.readLines()
                bis.close()
                responses.forEach { response.append(it) }
                mAllRepos = JSONArray(response.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                refreshData()
            }
        }).start()

    }

    private fun showLoading() {
        mLoading.visibility = View.VISIBLE
        mTimingQrView.visibility = View.GONE
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }

    private fun hideLoading() {
        mLoading.visibility = View.GONE
        mTimingQrView.visibility = View.VISIBLE
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }

    private fun refreshData() {
        val allRepos = mAllRepos
        var link = "https://github.com/CListery"
        if (null != allRepos) {
            val repoObj =
                allRepos.getJSONObject((allRepos.length() * Math.random()).toInt() % allRepos.length())
            link = repoObj.optString("html_url", link)
        }
        runOnUiThread {
            hideLoading()
            mNoticeTxt.text = link
        }
        mTimingQrView.setupQRCode(link, mTime)
    }

    override fun onRefreshClick() {
        refreshData()
    }

    override fun onStart(time: Int) {
        mTimingTxt.text = "${time}秒"
    }

    override fun onChanged(time: Int) {
        mTimingTxt.text = "${time}秒"
    }

    override fun onEnd() {
        mNoticeTxt.text = "二维码已过期，请点击刷新"
        mTimingTxt.text = ""
    }
}