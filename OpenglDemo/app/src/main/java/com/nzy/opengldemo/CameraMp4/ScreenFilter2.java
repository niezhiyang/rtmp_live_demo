package com.nzy.opengldemo.CameraMp4;

import android.content.Context;

import com.nzy.opengldemo.CameraMp4.filter.AbstractFilter;
import com.nzy.opengldemo.R;

/**
 * @author niezhiyang
 * since 11/1/21
 */
public class ScreenFilter2 extends AbstractFilter {
    /**
     * @param context
     */
    public ScreenFilter2(Context context) {
        super(context, R.raw.base_vert, R.raw.base_frag);
    }
}
