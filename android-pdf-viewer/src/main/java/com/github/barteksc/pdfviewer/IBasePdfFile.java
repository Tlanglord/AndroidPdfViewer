package com.github.barteksc.pdfviewer;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.util.Size;
import android.util.SizeF;

import com.github.barteksc.pdfviewer.exception.PageRenderingException;


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

    public Meta getMetaData();

    public List<Bookmark> getBookmarks();

    public List<Link> getPageLinks(int pageIndex);

    public RectF mapRectToDevice(int pageIndex, int startX, int startY, int sizeX, int sizeY, RectF rect);

    public void dispose();

    public int determineValidPageNumberFrom(int userPage);

    public int documentPage(int userPage);


}
