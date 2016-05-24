package com.v5kf.client.ui.callback;

import android.content.Context;

/**
 * 自定义对话消息中链接点击事件，将替代默认方式
 * @author Chenhy
 *
 */
public interface OnURLClickListener {
	public void onURLClick(Context context, String url);
}
