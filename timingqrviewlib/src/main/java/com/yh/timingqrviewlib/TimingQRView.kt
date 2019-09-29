package com.yh.timingqrviewlib

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.text.TextUtils
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.abs

/**
 * Created by CYH on 2019-07-01 14:10
 */
class TimingQRView : View,
    View.OnClickListener {

    companion object {
        private const val TIMING_START = 0x11
        private const val TIMING_CHANGE = 0x12
    }

    private var mTimingListener: ITimingListener? = null
    private val mMsgCallback: Handler.Callback = Handler.Callback { msg ->
        when (msg.what) {
            TIMING_START -> {
                mTimingListener?.onStart(mQRInvalidTime)
                if (mQRInvalidTime > 0) {
                    mTimingHandler.sendEmptyMessageDelayed(TIMING_CHANGE, 1000)
                } else {
                    internalInvalid()
                }
            }

            TIMING_CHANGE -> {
                --mQRInvalidTime
                if (mQRInvalidTime > 0) {
                    mTimingListener?.onChanged(mQRInvalidTime)
                    mTimingHandler.sendEmptyMessageDelayed(TIMING_CHANGE, 1000)
                } else {
                    internalInvalid()
                }
            }
        }
        true
    }

    private fun internalInvalid() {
        mTimingListener?.onEnd()
        mShowRefreshBtn = true
        invalidate()
    }

    private val mTimingHandler = Handler(mMsgCallback)

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    private var mQRInvalidTime: Int = 0
    private var mMinSlideDistance: Int = 0
    private var mClickListener: IClickListener? = null
    private var mShowRefreshBtn: Boolean = false
        set(value) {
            isEnabled = value
            isFocusable = value
            isClickable = value
            field = value
        }

    private val mRadius = dipToPx(10F).toFloat()
    private val mRectW = dipToPx(1F).toFloat()
    private var mInnerPadding = dipToPx(5F).toFloat()
    private var mRefreshSize: Float = 0F

    private val mRect = RectF()
    private val mBackgroundPaint = Paint()

    private val mQrPaint = Paint()
    private val mQrRect = RectF()
    private var mQRBitmap: Bitmap? = null

    private val mRefreshPaint = Paint()
    private val mRefreshRect = RectF()
    private var mRefreshBitmap: Bitmap? = null

    init {
        mBackgroundPaint.isAntiAlias = true
        mBackgroundPaint.strokeWidth = mRectW
        mBackgroundPaint.strokeJoin = Paint.Join.ROUND

        setOnClickListener(this)
        mShowRefreshBtn = false

        mMinSlideDistance = ViewConfiguration.get(context)
            .scaledTouchSlop

        mRefreshPaint.isAntiAlias = true
        mRefreshBitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_refresh)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mRect.set(
            mRectW / 2, mRectW / 2, w.toFloat() - mRectW / 2, h.toFloat() - mRectW / 2
        )
        mQrRect.set(
            mRect.left + mInnerPadding,
            mRect.top + mInnerPadding,
            mRect.right - mInnerPadding,
            mRect.bottom - mInnerPadding
        )
        mRefreshSize = w.toFloat() * 0.3F / 2F
        mRefreshRect.set(
            w.toFloat() / 2 - mRefreshSize,
            h.toFloat() / 2 - mRefreshSize,
            w.toFloat() / 2 + mRefreshSize,
            h.toFloat() / 2 + mRefreshSize
        )
    }

    override fun onDraw(canvas: Canvas?) {
        if (null == canvas) {
            return
        }
        mBackgroundPaint.style = Paint.Style.FILL
        mBackgroundPaint.color = Color.WHITE
        canvas.drawRoundRect(mRect, mRadius, mRadius, mBackgroundPaint)
        mBackgroundPaint.style = Paint.Style.STROKE
        mBackgroundPaint.color = Color.parseColor("#5212CC")
        canvas.drawRoundRect(mRect, mRadius, mRadius, mBackgroundPaint)

        if (null != mQRBitmap && false == mQRBitmap?.isRecycled) {
            canvas.drawBitmap(mQRBitmap!!, null, mQrRect, mQrPaint)
        }

        if (mShowRefreshBtn && null != mRefreshBitmap && false == mRefreshBitmap?.isRecycled) {
            mBackgroundPaint.style = Paint.Style.FILL
            mBackgroundPaint.color = Color.parseColor("#CC000000")
            canvas.drawRoundRect(mRect, mRadius, mRadius, mBackgroundPaint)
            canvas.drawBitmap(mRefreshBitmap!!, null, mRefreshRect, mRefreshPaint)
        }
    }

    override fun onSaveInstanceState(): Parcelable? {
        val bundle = Bundle()
        val superData = super.onSaveInstanceState()
        bundle.putParcelable("super_data", superData)
        if (null != tag) {
            bundle.putString("qr_data", tag.toString())
            bundle.putBoolean("show_refresh", mShowRefreshBtn)
        }
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (null == state as? Bundle) {
            return
        }
        val superData = state.getParcelable<Parcelable?>("super_data")
        tag = state.getString("qr_data")
        super.onRestoreInstanceState(superData)
        if (null != tag) {
            mShowRefreshBtn = state.getBoolean("show_refresh")
            invalidate()
        }
    }

    fun setOnClickListener(l: IClickListener) {
        mClickListener = l
    }

    fun setTimingListener(l: ITimingListener) {
        mTimingListener = l
    }

    fun setupQRCode(data: String, validTime: Int) {
        if (validTime <= 0) {
            return
        }
        if (null != tag && TextUtils.equals(data, tag.toString())) {
            mQRInvalidTime = validTime
            mShowRefreshBtn = false
            mTimingHandler.sendEmptyMessage(TIMING_START)
            postInvalidate()
            return
        }else if (null != mQRBitmap && false == mQRBitmap?.isRecycled) {
            mQRBitmap?.recycle()
        }
        mQRBitmap = QREncodingHelper.createQRCode(data, width)
        tag = data
        mQRInvalidTime = validTime
        mShowRefreshBtn = false
        mTimingHandler.sendEmptyMessage(TIMING_START)
        postInvalidate()
    }

    fun enableRefresh(flag: Boolean) {
        if (flag != mShowRefreshBtn) {
            mShowRefreshBtn = flag
            postInvalidate()
        }
    }

    private var mY: Float = 0F
    private var mX: Float = 0F
    private var isClick: Boolean = false
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.pointerCount > 1) return false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mX = event.x
                mY = event.y
                isClick = true
            }

            MotionEvent.ACTION_MOVE -> {
                val mDY: Float
                if (isClick) {
                    mDY = event.y - mY
                    isClick = abs(mDY) <= mMinSlideDistance
                }
            }

            MotionEvent.ACTION_UP -> {
                mX = event.x
                mY = event.y
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onClick(v: View?) {
        if (!isClick || !mShowRefreshBtn) {
            return
        }
        mClickListener?.onRefreshClick()
    }

    /**
     * dp转px
     *
     * @param dpValue dp
     * @return px
     */
    private fun dipToPx(dpValue: Float): Int {
        val scale = resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    interface IClickListener {
        fun onRefreshClick()
    }

    interface ITimingListener {
        fun onStart(time: Int)
        fun onChanged(time: Int)
        fun onEnd()
    }
}