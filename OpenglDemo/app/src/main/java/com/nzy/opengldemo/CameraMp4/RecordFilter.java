package com.nzy.opengldemo.CameraMp4;

import android.content.Context;

import com.nzy.opengldemo.CameraMp4.filter.AbstractFilter;
import com.nzy.opengldemo.R;

/**
 * @author niezhiyang
 * since 11/1/21
 */
public class RecordFilter extends AbstractFilter {

    /**
     * @param context
     */
    public RecordFilter(Context context) {
        super(context, R.raw.base_vert, R.raw.base_frag);
    }
}
