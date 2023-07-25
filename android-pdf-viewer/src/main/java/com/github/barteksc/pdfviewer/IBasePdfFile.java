package com.github.barteksc.pdfviewer;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;

import com.github.barteksc.pdfviewer.exception.PageRenderingException;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.util.Size;
import com.shockwave.pdfium.util.SizeF;

import java.util.List;

/**
 * Created by dongqiangqiang on 2023/7/25
 */
public interface IBasePdfFile {

    public void recalculatePageSizes(Size viewSize);

    public int getPagesCount();

    public SizeF getPageSize(int pageIndex);

    public SizeF getScaledPageSize(int pageIndex, float zoom);

    public SizeF getMaxPageSize();

    public float getMaxPageWidth();

    public float getMaxPageHeight();

    public float getDocLen(float zoom);

    public float getPageLength(int pageIndex, float zoom);

    public float getPageSpacing(int pageIndex, float zoom);

    public float getPageOffset(int pageIndex, float zoom);

    public float getSecondaryPageOffset(int pageIndex, float zoom);

    public int getPageAtOffset(float offset, float zoom);

    public boolean openPage(int pageIndex) throws PageRenderingException;

    public boolean pageHasError(int pageIndex);

    public void renderPageBitmap(Bitmap bitmap, int pageIndex, Rect bounds, boolean annotationRendering);

    public PdfDocument.Meta getMetaData();

    public List<PdfDocument.Bookmark> getBookmarks();

    public List<PdfDocument.Link> getPageLinks(int pageIndex);

    public RectF mapRectToDevice(int pageIndex, int startX, int startY, int sizeX, int sizeY, RectF rect);

    public void dispose();

    public int determineValidPageNumberFrom(int userPage);

    public int documentPage(int userPage);


}
