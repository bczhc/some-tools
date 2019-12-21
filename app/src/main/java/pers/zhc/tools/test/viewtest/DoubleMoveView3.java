package pers.zhc.tools.test.viewtest;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Author: aaa
 * Date: 2016/12/5 17:31.
 */
public class DoubleMoveView3 extends View {
    private Context mContext;
    private Bitmap mSrcBitmap;
    private Bitmap mMultiplyBitmap = null;//混合之后的图片,双指缩放移动的时候,单独移动这张混合后图片,提高用户体验
    public boolean mIsMove;//是否双指拖动图片中ing
    private int mBitmapWidth, mBitmapHeight;//图片的长度和高度

    private float mCenterLeft, mCenterTop;//图片居中时左上角的坐标
    private int mCenterHeight, mCenterWidth; // 图片适应屏幕时的大小
    private float mCenterScale;//画布居中时的比例
    private int mViewWidth, mViewHeight;//当前View的长度和宽度

    private float mTransX = 0, mTransY = 0; // 偏移量，图片真实偏移量为　mCentreTranX + mTransX
    private float mScale = 1.0f; // 缩放倍数, 图片真实的缩放倍数为 mPrivateScale * mScale

    private boolean mIsSaveArg = false;//保存参数使用

    private Paint mPaint;
    private Bitmap mGraffitiBitmap; // 用绘制涂鸦的图片
    private Canvas mBitmapCanvas; // 用于绘制涂鸦的画布

    private Bitmap mCurrentBitmap; // 绘制当前线时用到的图片
    private Canvas mCurrentCanvas; // 当前绘制线的画布

    private int[] mWhiteBuffer;//保存白色图片内存，刷新时重新刷新图片
    // 保存涂鸦操作，便于撤销
    private CopyOnWriteArrayList<MyDrawSinglePath> mPathStack = new CopyOnWriteArrayList<MyDrawSinglePath>();
    private CopyOnWriteArrayList<MyDrawSinglePath> pathStackBackup = new CopyOnWriteArrayList<MyDrawSinglePath>();
    private int mTouchMode; // 触摸模式，触点数量
    private float mTouchDownX, mTouchDownY, mLastTouchX, mLastTouchY, mTouchX, mTouchY;
    private MyDrawSinglePath mCurrPath; // 当前手写的路径

    private PorterDuffXfermode mPdXfermode; // 定义PorterDuffXfermode变量
    private Paint mPdXfPaint;// 绘图的混合模式
    private Paint mCurrentPaint;
    private Path mCanvasPath; //仅用于当前Path的绘制

    public DoubleMoveView3(Context context, Bitmap bitmap) {
        super(context);
        mContext = context;
        init(bitmap);
    }

    public DoubleMoveView3(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private void init(Bitmap bitmap) {
        mSrcBitmap = bitmap;
        mBitmapWidth = mSrcBitmap.getWidth();
        mBitmapHeight = mSrcBitmap.getHeight();
        mMultiplyBitmap = Bitmap.createBitmap(mBitmapWidth, mBitmapHeight, Bitmap.Config.ARGB_8888);

        mPaint = new Paint();
        mPaint.setColor(Color.RED);
        mPaint.setStrokeWidth(20);
        mGraffitiBitmap = getTransparentBitmap(mSrcBitmap);

        mCurrentPaint = new Paint();
        mCurrentPaint.setStyle(Paint.Style.STROKE);
        mCurrentPaint.setStrokeWidth(50);
        mCurrentPaint.setColor(Color.RED);
        mCurrentPaint.setAlpha(100);
        mCurrentPaint.setAntiAlias(true);
        mCurrentPaint.setStrokeJoin(Paint.Join.ROUND);
        mCurrentPaint.setStrokeCap(Paint.Cap.ROUND);
        mCurrentPaint.setXfermode(null);

        mCanvasPath = new Path();
        mCurrentBitmap = getTransparentBitmap(mSrcBitmap);

        //设置混合模式   （正片叠底）
        mPdXfermode = new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY);

        mPdXfPaint = new Paint();
        mPdXfPaint.setAntiAlias(true);
        mPdXfPaint.setFilterBitmap(true);

        mIsMove = false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mViewWidth = w;
        mViewHeight = h;

        float nw = mBitmapWidth * 1f / mViewWidth;
        float nh = mBitmapHeight * 1f / mViewHeight;
        if (nw > nh) {
            mCenterScale = 1 / nw;
            mCenterWidth = mViewWidth;
            mCenterHeight = (int) (mBitmapHeight * mCenterScale);
        } else {
            mCenterScale = 1 / nh;
            mCenterWidth = (int) (mBitmapWidth * mCenterScale);
            mCenterHeight = mViewHeight;
        }

        // 使图片居中
        mCenterLeft = (mViewWidth - mCenterWidth) / 2f;
        mCenterTop = (mViewHeight - mCenterHeight) / 2f;

        initCanvas();
        initCurrentCanvas();

        mIsMove = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float scale = mCenterScale * mScale;
        float x = (mCenterLeft + mTransX) / scale;
        float y = (mCenterTop + mTransY) / scale;

        canvas.scale(scale, scale);

        if (!mIsMove) {
            //正片叠底混合模式
            initCurrentCanvas();
            mCurrentCanvas.drawPath(mCanvasPath, mCurrentPaint);

            canvas.drawBitmap(mSrcBitmap, x, y, mPdXfPaint);
            mPdXfPaint.setXfermode(mPdXfermode);
            canvas.drawBitmap(mCurrentBitmap, x, y, mPdXfPaint);
            // 绘制涂鸦图片
            canvas.drawBitmap(mGraffitiBitmap, x, y, mPdXfPaint);
            mPdXfPaint.setXfermode(null);
        } else {
            //只显示原始图片
            canvas.drawBitmap(mMultiplyBitmap, x, y, null);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {

            case MotionEvent.ACTION_DOWN:
                mTouchMode = 1;
                penTouchDown(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mTouchMode = 0;
                mCanvasPath.reset();
                initCanvas();//添上这句防止重复绘制
                draw(mBitmapCanvas, mPathStack); // 保存到图片中
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (mTouchMode == 1 && !mIsMove) {
                    mLastTouchX = mTouchX;
                    mLastTouchY = mTouchY;
                    mTouchX = event.getX();
                    mTouchY = event.getY();

                    mCurrPath.getPath().quadTo(screenToBitmapX(mLastTouchX), screenToBitmapY(mLastTouchY),
                            screenToBitmapX((mTouchX + mLastTouchX) / 2), screenToBitmapY((mTouchY + mLastTouchY) / 2));

                    mCanvasPath.quadTo(screenToBitmapX(mLastTouchX), screenToBitmapY(mLastTouchY),
                            screenToBitmapX((mTouchX + mLastTouchX) / 2), screenToBitmapY((mTouchY + mLastTouchY) / 2));
                    invalidate();
                }
                return true;
        }
        return true;
    }

    public void setTransScale(float scale, float dx, float dy) {
        mScale = scale;
        mTransX = dx;
        mTransY = dy;
        if (!mIsSaveArg) {
            invalidate();
        }
        mIsSaveArg = false;
    }

    public void saveCurrentScale() {
        mCenterScale = mCenterScale * mScale;

        mCenterLeft = (mCenterLeft + mTransX) / mCenterScale;
        mCenterTop = (mCenterTop + mTransY) / mCenterScale;

        mIsSaveArg = true;

        saveMultiplyBitmap();
    }

    //双指移动的时候,生成混合之后的图片
    private void saveMultiplyBitmap() {
        mIsMove = true;

        Canvas canvas = new Canvas(mMultiplyBitmap);
        canvas.drawBitmap(mSrcBitmap, 0, 0, mPdXfPaint);
        mPdXfPaint.setXfermode(mPdXfermode);
        // 绘制涂鸦图片
        canvas.drawBitmap(mGraffitiBitmap, 0, 0, mPdXfPaint);
        mPdXfPaint.setXfermode(null);
    }

    /**
     * 在画笔的状态下第一个触点按下的情况
     */
    private void penTouchDown(float x, float y) {
        mIsMove = false;
        mTouchDownX = mTouchX = mLastTouchX = x;
        mTouchDownY = mTouchY = mLastTouchY = y;

        // 为了仅点击时也能出现绘图，模拟滑动一个像素点
        mTouchX++;
        mTouchY++;

        mCurrPath = new MyDrawSinglePath(Color.RED, 50, 100, true);
        mCurrPath.getPath().moveTo(screenToBitmapX(mTouchDownX), screenToBitmapY(mTouchDownY));
        mPathStack.add(mCurrPath);

        mCanvasPath.reset();
        mCanvasPath.moveTo(screenToBitmapX(mTouchDownX), screenToBitmapY(mTouchDownY));
        // 为了仅点击时也能出现绘图，必须移动path
        mCanvasPath.quadTo(screenToBitmapX(mLastTouchX), screenToBitmapY(mLastTouchY),
                screenToBitmapX((mTouchX + mLastTouchX) / 2), screenToBitmapY((mTouchY + mLastTouchY) / 2));
    }

    /**
     * 初始化当前画线的绘图
     */
    private void initCurrentCanvas() {
        mCurrentBitmap.setPixels(mWhiteBuffer, 0, mSrcBitmap.getWidth(), 0, 0, mSrcBitmap.getWidth(), mSrcBitmap.getHeight());
        mCurrentCanvas = new Canvas(mCurrentBitmap);
    }

    /**
     * 初始化涂鸦的绘图
     */
    private void initCanvas() {
        mGraffitiBitmap.setPixels(mWhiteBuffer, 0, mSrcBitmap.getWidth(), 0, 0, mSrcBitmap.getWidth(), mSrcBitmap.getHeight());
        mBitmapCanvas = new Canvas(mGraffitiBitmap);
    }

    /**
     * 创建一个图片,透明度为255(不透明), 底色为白色 ,目的是为了使用正片叠底
     *
     * @param sourceImg
     * @return
     */
    public Bitmap getTransparentBitmap(Bitmap sourceImg) {
        mWhiteBuffer = new int[sourceImg.getWidth() * sourceImg.getHeight()];
        Arrays.fill(mWhiteBuffer, 0xFFFFFFFF);
        sourceImg = Bitmap.createBitmap(mWhiteBuffer, sourceImg.getWidth(), sourceImg.getHeight(), Bitmap.Config.ARGB_8888).copy(Bitmap.Config.ARGB_8888, true);
        return sourceImg;
    }

    private void draw(Canvas canvas, CopyOnWriteArrayList<MyDrawSinglePath> pathStack) {
        // 还原堆栈中的记录的操作
        for (MyDrawSinglePath path : pathStack) {
            canvas.drawPath(path.getPath(), path.getMyPen().getPenPaint());
        }
    }

    //双指抬起时
    public void PointertUp() {

        if (!mCanvasPath.isEmpty()) {//单指画线过程中，出现双触点则停止画线
            mCanvasPath.reset();
            if (!mPathStack.isEmpty()) {
                mPathStack.remove(mPathStack.size() - 1);
            }
        }
    }

    /**
     * 将触摸的屏幕坐标转换成实际图片中的坐标
     */
    public float screenToBitmapX(float touchX) {
        return (touchX - mCenterLeft - mTransX) / (mCenterScale * mScale);
    }

    public float screenToBitmapY(float touchY) {
        return (touchY - mCenterTop - mTransY) / (mCenterScale * mScale);
    }

    //通过触点的坐标和实际图片中的坐标,得到当前图片的起始点坐标
    public final float toTransX(float touchX, float graffitiX) {
        return -graffitiX * (mCenterScale * mScale) + touchX - mCenterLeft;
    }

    public final float toTransY(float touchY, float graffitiY) {
        return -graffitiY * (mCenterScale * mScale) + touchY - mCenterTop;
    }
}