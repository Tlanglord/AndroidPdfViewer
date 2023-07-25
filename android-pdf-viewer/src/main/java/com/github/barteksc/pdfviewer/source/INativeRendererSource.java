package com.github.barteksc.pdfviewer.source;

import android.content.Context;
import android.graphics.pdf.PdfRenderer;

/**
 * Created by dongqiangqiang on 2023/7/25
 */
public interface INativeRendererSource {

    default PdfRenderer createPdfRenderer(Context context, String password) throws Exception {
        return null;
    }

}
