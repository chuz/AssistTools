package com.fornut.assisttools.models;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.GridView;

import com.fornut.assisttools.R;
import com.fornut.assisttools.views.QSScreenLock;
import com.fornut.assisttools.views.WhiteDot;

/**
 * 悬浮view的创建、控制及动画
 *
 * @author lcz
 */
public class DropzoneManager {

	private static boolean DBG = false;
	private static String TAG = "AssistTools-DropzoneManager";

	private boolean mWhiteDotIsAdded = false; // 是否已增加悬浮按钮
	private WhiteDot mWhiteDot;
	private LayoutParams mWhiteDot_Params;
	private int mWhiteDotClickCounter = 0;

	private boolean mControlBoardIsAdded = false;
	private View mControlBoard;
	private LayoutParams mControlBoard_Params;

	private boolean mAreQuickSwitchesAdded = false;
	private GridView mQuickSwitchPanel;

	private Context mContext;
	private WindowManager mWindowManager;
	private LayoutInflater mLayoutInflater;

	private static final int MSG_BASE = 0;
	private static final int MSG_CREATE_WHITEDOT = MSG_BASE + 1;
	private static final int MSG_CREATE_CONTROLBOARD = MSG_BASE + 2;
	private static final int MSG_INIT = MSG_BASE + 3;
	private static final int MSG_SHOW_WHITEDOT = MSG_BASE + 4;
	private static final int MSG_HIDE_WHITEDOT = MSG_BASE + 5;

	MyHandler mHandler = new MyHandler(this);

	static class MyHandler extends Handler {
		WeakReference<DropzoneManager> mDropzoneManager;

		MyHandler(DropzoneManager dropzoneManager) {
			mDropzoneManager = new WeakReference<DropzoneManager>(
					dropzoneManager);
		}

		@Override
		public void handleMessage(Message msg) {
			DropzoneManager dropzoneManager = mDropzoneManager.get();
			switch (msg.what) {
			case MSG_CREATE_WHITEDOT:
				dropzoneManager.createWhiteDot(dropzoneManager.mContext);
				sendEmptyMessage(MSG_INIT);
				break;
			case MSG_CREATE_CONTROLBOARD:
				dropzoneManager.createContolBoard(dropzoneManager.mContext);
				dropzoneManager.initQuickSwitches();
				sendEmptyMessage(MSG_INIT);
				break;
			case MSG_INIT:
				if (!dropzoneManager.mWhiteDotIsAdded) {
					sendEmptyMessage(MSG_CREATE_WHITEDOT);
				}
				if (!dropzoneManager.mControlBoardIsAdded) {
					sendEmptyMessage(MSG_CREATE_CONTROLBOARD);
				}
				if (dropzoneManager.mWhiteDotIsAdded
						&& dropzoneManager.mControlBoardIsAdded) {
					dropzoneManager.mControlBoard.setVisibility(View.GONE);
				}
				break;
			case MSG_SHOW_WHITEDOT:
				dropzoneManager.mWhiteDot.setVisibility(View.VISIBLE);
				dropzoneManager.mControlBoard.setVisibility(View.GONE);
				break;
			case MSG_HIDE_WHITEDOT:
				dropzoneManager.mWhiteDot.setVisibility(View.GONE);
				dropzoneManager.mControlBoard.setVisibility(View.VISIBLE);
				break;

			default:
				break;
			}
		}
	};

	public DropzoneManager(Context context) {
		// TODO Auto-generated constructor stub
		mContext = context;
		mWindowManager = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		mLayoutInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mHandler.sendEmptyMessage(MSG_INIT);
	}

	/**
	 * 创建悬浮按钮
	 */
	private void createWhiteDot(Context context) {
		if (mWhiteDotIsAdded) {
			return;
		}
		mWhiteDot = new WhiteDot(context);
		mWhiteDot_Params = new WindowManager.LayoutParams();
		// 设置window type
		mWhiteDot_Params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
		/*
		 * 如果设置为params.type = WindowManager.LayoutParams.TYPE_PHONE; 那么优先级会降低一些,
		 * 即拉下通知栏不可见
		 */

		mWhiteDot_Params.format = PixelFormat.RGBA_8888; // 设置图片格式，效果为背景透明

		// 设置Window flag
		// 透传 LayoutParams.FLAG_NOT_TOUCH_MODAL |
		// LayoutParams.FLAG_NOT_FOCUSABLE;
		mWhiteDot_Params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
				| WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
				| WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
				| WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;

		int w = context.getResources().getDimensionPixelSize(
				R.dimen.whitedot_width);
		int h = context.getResources().getDimensionPixelSize(
				R.dimen.whitedot_height);

		// 设置悬浮按钮的长得宽
		mWhiteDot_Params.width = w;
		mWhiteDot_Params.height = h;
		// 通知view组件重绘
		mWhiteDot.invalidate();

		// 设置悬浮按钮的Touch监听
		mWhiteDot.setOnTouchListener(new OnTouchListener() {
			int lastX, lastY;
			int paramX, paramY;

			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					lastX = (int) event.getRawX();
					lastY = (int) event.getRawY();
					paramX = mWhiteDot_Params.x;
					paramY = mWhiteDot_Params.y;
					mWhiteDotClickCounter = 0;
					if (DBG)
						Log.d(TAG, "ACTION_DOWN");
					break;
				case MotionEvent.ACTION_MOVE:
					int dx = (int) event.getRawX() - lastX;
					int dy = (int) event.getRawY() - lastY;
					mWhiteDot_Params.x = paramX + dx;
					mWhiteDot_Params.y = paramY + dy;
					// 更新悬浮按钮位置
					mWindowManager
							.updateViewLayout(mWhiteDot, mWhiteDot_Params);
					mWhiteDotClickCounter++;
					if (DBG)
						Log.d(TAG, "ACTION_MOVE");
					break;
				case MotionEvent.ACTION_UP:
					if (DBG)
						Log.d(TAG, "ACTION_UP");
					break;
				}
				return false;// true 其他的动作都捕捉不到，比如Click，longClick等等
			}
		});

		/*
		 * OnClickListener只发生在up之后
		 */
		mWhiteDot.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				if (DBG)
					Log.d(TAG, "onClick");
				if (mWhiteDotClickCounter < 5) {
					mHandler.sendEmptyMessage(MSG_HIDE_WHITEDOT);
				}
			}
		});

		/*
		 * OnLongClickListene可能发生在down-move LongClick move-up
		 */
		mWhiteDot.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				// TODO Auto-generated method stub
				if (DBG)
					Log.d(TAG, "onLongClick");
				return false;
			}
		});

		mWindowManager.addView(mWhiteDot, mWhiteDot_Params);
		mWhiteDotIsAdded = true;
	}

	/**
	 * 创建控制盘
	 */
	private void createContolBoard(Context context) {

		if (mControlBoardIsAdded) {
			return;
		}
		mControlBoard = mLayoutInflater.inflate(R.layout.control_board, null);

		mQuickSwitchPanel = (GridView) mControlBoard.findViewById(R.id.quickswitch_panel);

		mControlBoard_Params = new WindowManager.LayoutParams();
		// 设置window type
		mControlBoard_Params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
		/*
		 * 如果设置为params.type = WindowManager.LayoutParams.TYPE_PHONE; 那么优先级会降低一些,
		 * 即拉下通知栏不可见
		 */

		mControlBoard_Params.format = PixelFormat.RGBA_8888; // 设置图片格式，效果为背景透明

		// 设置Window flag
		mControlBoard_Params.flags = WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;

		int w = context.getResources().getDimensionPixelSize(
				R.dimen.controlboard_width);
		int h = context.getResources().getDimensionPixelSize(
				R.dimen.controlboard_height);

		// 设置悬浮按钮的长得宽
		mControlBoard_Params.width = w;
		mControlBoard_Params.height = h;

		mControlBoard.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				Log.d(TAG, "onTouch event " + event.getAction());
				if (event.getAction() == MotionEvent.ACTION_OUTSIDE
						|| event.getAction() == MotionEvent.ACTION_DOWN) {
					mHandler.sendEmptyMessage(MSG_SHOW_WHITEDOT);
				}
				return false;
			}
		});

		mWindowManager.addView(mControlBoard, mControlBoard_Params);
		mControlBoardIsAdded = true;
	}

	void initQuickSwitches(){
		mAreQuickSwitchesAdded = false;
		mQuickSwitchPanel.setAdapter(new QuickSwitchesAdapter());
		mAreQuickSwitchesAdded = true;
	}

	void createVolumeBar() {

	}

	class QuickSwitchesAdapter extends BaseAdapter{

        @Override
        public int getCount() {
              // TODO Auto-generated method stub
              return 9;
        }

        @Override
        public Object getItem(int arg0) {
              // TODO Auto-generated method stub
              return null;
        }

        @Override
        public long getItemId(int position) {
              // TODO Auto-generated method stub
              return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
              // TODO Auto-generated method stub
              return new QSScreenLock(mContext);
        }
  }
}
