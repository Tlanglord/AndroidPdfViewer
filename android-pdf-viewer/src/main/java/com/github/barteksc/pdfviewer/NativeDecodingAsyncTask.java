package com.github.barteksc.pdfviewer;

import android.graphics.pdf.PdfRenderer;
import android.os.AsyncTask;

import com.github.barteksc.pdfviewer.source.DocumentSource;
import com.shockwave.pdfium.util.Size;

import java.lang.ref.WeakReference;

/**
 * Created by dongqiangqiang on 2023/7/25
 * 使用PdfRenderer方式打开PDF文件
 */
class NativeDecodingAsyncTask extends AsyncTask<Void, Void, Throwable> {

    private boolean cancelled;

    private WeakReference<PDFView> pdfViewReference;

    private PdfRenderer pdfRenderer;
    private String password;
    private DocumentSource docSource;
    private int[] userPages;
    private AbsPdfFile pdfFile;

    NativeDecodingAsyncTask(DocumentSource docSource, String password, int[] userPages, PDFView pdfView) {
        this.docSource = docSource;
        this.userPages = userPages;
        this.cancelled = false;
        this.pdfViewReference = new WeakReference<>(pdfView);
        this.password = password;
    }

    @Override
    protected Throwable doInBackground(Void... params) {
        try {
            PDFView pdfView = pdfViewReference.get();
            if (pdfView != null) {

                pdfRenderer = docSource.createPdfRenderer(pdfView.getContext(), password);
                pdfFile = new NativePdfFile(
                        pdfRenderer,
                        pdfView.getPageFitPolicy(),
                        getViewSize(pdfView),
                        userPages,
                        pdfView.isSwipeVertical(),
                        pdfView.getSpacingPx(),
                        pdfView.isAutoSpacingEnabled(),
                        pdfView.isFitEachPage()
                );
                
                return null;
            } else {
                return new NullPointerException("pdfView == null");
            }

        } catch (Throwable t) {
            return t;
        }
    }

    private Size getViewSize(PDFView pdfView) {
        return new Size(pdfView.getWidth(), pdfView.getHeight());
    }

    @Override
    protected void onPostExecute(Throwable t) {
        PDFView pdfView = pdfViewReference.get();
        if (pdfView != null) {
            if (t != null) {
                pdfView.loadError(t);
                return;
            }
            if (!cancelled) {
                pdfView.loadComplete(pdfFile);
            }
        }
    }

    @Override
    protected void onCancelled() {
        cancelled = true;
    }
}
