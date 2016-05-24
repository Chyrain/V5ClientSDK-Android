/*
 * Copyright (C), 2014, TP-LINK TECHNOLOGIES CO., LTD.
 *
 * WarningDialog.java
 *
 * Author YuLibo
 *
 * Ver 1.0, 2014-10-27, YuLibo, Create file
 */

package com.v5kf.client.ui.widget;

import com.v5kf.client.ui.utils.UIUtil;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class WarningDialog extends Dialog implements OnClickListener {

    public final static int MODE_ONE_BUTTON = 1;
    public final static int MODE_TWO_BUTTON = 2;

    public interface WarningDialogListener {
        public void onClick(View view);
    }

    private WarningDialogListener mListener;

    protected TextView mContentTextView;

    private TextView mContentTextSecondView;

    protected Button mRightButton;

    protected Button mLeftButton;

    protected Button mMiddleButton;

    public WarningDialog(Context context) {
        super(context, UIUtil.getIdByName(context, "style", "v5_custom_dialog"));
        setContentView(UIUtil.getIdByName(context, "layout", "v5_dialog_warning"));
        findView();
        setOnClickListener();
        setCancelable(false);
    }

    private void findView() {
        mContentTextView = (TextView) findViewById(UIUtil.getIdByName(getContext(), "id", "tv_dialog_warning_content"));
        mContentTextSecondView = (TextView) findViewById(UIUtil.getIdByName(getContext(), "id", "tv_dialog_warning_content_secondlayer"));
        mLeftButton = (Button) findViewById(UIUtil.getIdByName(getContext(), "id", "btn_dialog_warning_left"));
        mRightButton = (Button) findViewById(UIUtil.getIdByName(getContext(), "id", "btn_dialog_warning_right"));
        mMiddleButton = (Button) findViewById(UIUtil.getIdByName(getContext(), "id", "btn_dialog_warning_middle"));
        mLeftButton.setTag("left");
        mRightButton.setTag("right");
        mMiddleButton.setTag("middle");
    }

    public void setContent(String content) {
        mContentTextView.setText(content);
    }

    public void setContentSecondLayer(String content) {
        mContentTextSecondView.setText(content);
        mContentTextSecondView.setVisibility(View.VISIBLE);
    }

    public void setContent(int contentResId) {
        mContentTextView.setText(contentResId);
    }

    public void setContentSecondLayer(int contentResId) {
        mContentTextSecondView.setText(contentResId);
        mContentTextSecondView.setVisibility(View.VISIBLE);
    }

    public Button getMidButton() {
    	return mMiddleButton;
    }

    public Button getRightButton() {
        return mRightButton;
    }

    public Button getLeftButton() {
        return mLeftButton;
    }

    public void setOnClickListener(WarningDialogListener listener) {
        this.mListener = listener;
    }

    private void setOnClickListener() {
        mRightButton.setOnClickListener(this);
        mLeftButton.setOnClickListener(this);
        mMiddleButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
    	if (v.getId() == UIUtil.getIdByName(getContext(), "id", "btn_dialog_warning_middle")) {
    		this.dismiss();
    	} else if (v.getId() == UIUtil.getIdByName(getContext(), "id", "btn_dialog_warning_left")) {
    		this.dismiss();
    	}
    	
    	if (mListener != null) {
    		mListener.onClick(v); // Modify
    	}
    }

    public void setContentViewGravity(int gravity) {
        mContentTextView.setGravity(gravity);
    }

    public void setDialogMode(int mode) {
        if (mode == MODE_ONE_BUTTON) {
            findViewById(UIUtil.getIdByName(getContext(), "id", "layout_dialog_one_button")).setVisibility(View.VISIBLE);
            findViewById(UIUtil.getIdByName(getContext(), "id", "layout_dialog_two_button")).setVisibility(View.GONE);
        } else {
            findViewById(UIUtil.getIdByName(getContext(), "id", "layout_dialog_one_button")).setVisibility(View.GONE);
            findViewById(UIUtil.getIdByName(getContext(), "id", "layout_dialog_two_button")).setVisibility(View.VISIBLE);
        }
    }
}
