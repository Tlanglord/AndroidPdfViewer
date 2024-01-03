package com.github.barteksc.pdfviewer;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.pdf.PdfRenderer;
import android.util.SparseBooleanArray;

import com.github.barteksc.pdfviewer.exception.PageRenderingException;
import com.github.barteksc.pdfviewer.util.FitPolicy;
import com.github.barteksc.pdfviewer.util.PageSizeCalculator;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.util.Size;
import com.shockwave.pdfium.util.SizeF;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dongqiangqiang on 2023/7/25
 */
public class NativePdfFile extends AbsPdfFile {

    private static final Object lock = new Object();

    private PdfRenderer pdfRenderer;

    private int pagesCount = 0;

    private List<Size> originalPageSizes = new ArrayList<>();

    private List<SizeF> pageSizes = new ArrayList<>();

    private SparseBooleanArray openedPages = new SparseBooleanArray();

    private Size originalMaxWidthPageSize = new Size(0, 0);

    /**
     * Page with maximum height
     */
    private Size originalMaxHeightPageSize = new Size(0, 0);


    private SizeF maxHeightPageSize = new SizeF(0, 0);
    /**
     * Scaled page with maximum width
     */
    private SizeF maxWidthPageSize = new SizeF(0, 0);

    private boolean isVertical;

    private int spacingPx;

    private boolean autoSpacing;

    private List<Float> pageOffsets = new ArrayList<>();

    private List<Float> pageSpacing = new ArrayList<>();

    private float documentLength = 0;

    private final FitPolicy pageFitPolicy;

    private final boolean fitEachPage;

    private int[] originalUserPages;

    public NativePdfFile(PdfRenderer pdfRenderer, FitPolicy pageFitPolicy, Size viewSize, int[] originalUserPages,
                         boolean isVertical, int spacing, boolean autoSpacing, boolean fitEachPage) {
        this.pdfRenderer = pdfRenderer;
        this.pageFitPolicy = pageFitPolicy;
        this.originalUserPages = originalUserPages;
        this.isVertical = isVertical;
        this.spacingPx = spacing;
        this.autoSpacing = autoSpacing;
        this.fitEachPage = fitEachPage;

        setup(viewSize);

    }

    private void setup(Size viewSize) {

        if (originalUserPages != null) {
            pagesCount = originalUserPages.length;
        } else {
            pagesCount = pdfRenderer.getPageCount();
        }


        for (int i = 0; i < pagesCount; i++) {
            PdfRenderer.Page page = pdfRenderer.openPage(i);
            int width = page.getWidth();
            int height = page.getHeight();
            Size pageSize = new Size(width, height);
            if (pageSize.getWidth() > originalMaxWidthPageSize.getWidth()) {
                originalMaxWidthPageSize = pageSize;
            }
            if (pageSize.getHeight() > originalMaxHeightPageSize.getHeight()) {
                originalMaxHeightPageSize = pageSize;
            }
            originalPageSizes.add(pageSize);
            page.close();
        }

        recalculatePageSizes(viewSize);

    }

    @Override
    public void recalculatePageSizes(Size viewSize) {

        pageSizes.clear();
        PageSizeCalculator calculator = new PageSizeCalculator(pageFitPolicy,
                                                               originalMaxWidthPageSize,
                                                               originalMaxHeightPageSize,
                                                               viewSize,
                                                               fitEachPage
        );
        maxWidthPageSize = calculator.getOptimalMaxWidthPageSize();
        maxHeightPageSize = calculator.getOptimalMaxHeightPageSize();

        for (Size size : originalPageSizes) {
            pageSizes.add(calculator.calculate(size));
        }
        if (autoSpacing) {
            prepareAutoSpacing(viewSize);
        }
        prepareDocLen();
        preparePagesOffset();
    }

    private void prepareAutoSpacing(Size viewSize) {
        pageSpacing.clear();
        for (int i = 0; i < getPagesCount(); i++) {
            SizeF pageSize = pageSizes.get(i);
            float spacing = Math.max(0,
                                     isVertical ? viewSize.getHeight() - pageSize.getHeight() :
                                             viewSize.getWidth() - pageSize.getWidth()
            );
            if (i < getPagesCount() - 1) {
                spacing += spacingPx;
            }
            pageSpacing.add(spacing);
        }
    }

    private void prepareDocLen() {
        float length = 0;
        for (int i = 0; i < getPagesCount(); i++) {
            SizeF pageSize = pageSizes.get(i);
            length += isVertical ? pageSize.getHeight() : pageSize.getWidth();
            if (autoSpacing) {
                length += pageSpacing.get(i);
            } else if (i < getPagesCount() - 1) {
                length += spacingPx;
            }
        }
        documentLength = length;
    }

    private void preparePagesOffset() {
        pageOffsets.clear();
        float offset = 0;
        for (int i = 0; i < getPagesCount(); i++) {
            SizeF pageSize = pageSizes.get(i);
            float size = isVertical ? pageSize.getHeight() : pageSize.getWidth();
            if (autoSpacing) {
                offset += pageSpacing.get(i) / 2f;
                if (i == 0) {
                    offset -= spacingPx / 2f;
                } else if (i == getPagesCount() - 1) {
                    offset += spacingPx / 2f;
                }
                pageOffsets.add(offset);
                offset += size + pageSpacing.get(i) / 2f;
            } else {
                pageOffsets.add(offset);
                offset += size + spacingPx;
            }
        }
    }

    @Override
    public int getPagesCount() {
        return pagesCount;
    }

    @Override
    public SizeF getPageSize(int pageIndex) {
        int docPage = documentPage(pageIndex);
        if (docPage < 0) {
            return new SizeF(0, 0);
        }
        return pageSizes.get(pageIndex);
    }

    @Override
    public SizeF getScaledPageSize(int pageIndex, float zoom) {
        SizeF size = getPageSize(pageIndex);
        return new SizeF(size.getWidth() * zoom, size.getHeight() * zoom);
    }

    @Override
    public SizeF getMaxPageSize() {
        return isVertical ? maxWidthPageSize : maxHeightPageSize;
    }

    @Override
    public float getMaxPageWidth() {
        return getMaxPageSize().getWidth();
    }

    @Override
    public float getMaxPageHeight() {
        return getMaxPageSize().getHeight();
    }

    @Override
    public float getDocLen(float zoom) {
        return documentLength * zoom;
    }

    @Override
    public float getPageLength(int pageIndex, float zoom) {
        SizeF size = getPageSize(pageIndex);
        return (isVertical ? size.getHeight() : size.getWidth()) * zoom;
    }

    @Override
    public float getPageSpacing(int pageIndex, float zoom) {
        float spacing = autoSpacing ? pageSpacing.get(pageIndex) : spacingPx;
        return spacing * zoom;
    }

    /**
     * 获取当前page顶部相对于初始的canvas的偏移
     *
     */
    @Override
    public float getPageOffset(int pageIndex, float zoom) {
        int docPage = documentPage(pageIndex);
        if (docPage < 0) {
            return 0;
        }
        return pageOffsets.get(pageIndex) * zoom;
    }

    @Override
    public float getSecondaryPageOffset(int pageIndex, float zoom) {
        SizeF pageSize = getPageSize(pageIndex);
        if (isVertical) {
            float maxWidth = getMaxPageWidth();
            return zoom * (maxWidth - pageSize.getWidth()) / 2; //x
        } else {
            float maxHeight = getMaxPageHeight();
            return zoom * (maxHeight - pageSize.getHeight()) / 2; //y
        }
    }

    @Override
    public int getPageAtOffset(float offset, float zoom) {
        int currentPage = 0;
        for (int i = 0; i < getPagesCount(); i++) {
            float off = pageOffsets.get(i) * zoom - getPageSpacing(i, zoom) / 2f;
            if (off >= offset) {
                break;
            }
            currentPage++;
        }
        return --currentPage >= 0 ? currentPage : 0;
    }

    @Override
    public boolean openPage(int pageIndex) throws PageRenderingException {
        int docPage = documentPage(pageIndex);
        if (docPage < 0) {
            return false;
        }

        synchronized (lock) {
            if (openedPages.indexOfKey(docPage) < 0) {
                try {
                    PdfRenderer.Page page = pdfRenderer.openPage(pageIndex);
                    page.close();
                    openedPages.put(docPage, true);
                    return true;
                } catch (Exception e) {
                    openedPages.put(docPage, false);
                    throw new PageRenderingException(pageIndex, e);
                }
            }
            return false;
        }
    }

    @Override
    public boolean pageHasError(int pageIndex) {
        int docPage = documentPage(pageIndex);
        return !openedPages.get(docPage, false);
    }

    @Override
    public void renderPageBitmap(Bitmap bitmap, int pageIndex, Rect bounds, boolean annotationRendering) {
        try {
            int docPage = documentPage(pageIndex);
            PdfRenderer.Page page = pdfRenderer.openPage(docPage);
            page.render(bitmap, bounds, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            page.close();
        } catch (Exception e) {
//            throw new IllegalArgumentException("renderPageBitmap error");
        }
    }

    @Override
    public PdfDocument.Meta getMetaData() {
        return new PdfDocument.Meta();
    }

    @Override
    public List<PdfDocument.Bookmark> getBookmarks() {
        return new ArrayList<>();
    }

    @Override
    public List<PdfDocument.Link> getPageLinks(int pageIndex) {
        return new ArrayList<>();
    }

    @Override
    public RectF mapRectToDevice(int pageIndex, int startX, int startY, int sizeX, int sizeY, RectF rect) {
        return new RectF();
    }

    @Override
    public void dispose() {
        try {
            pdfRenderer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        originalUserPages = null;
    }

    @Override
    public int determineValidPageNumberFrom(int userPage) {
        if (userPage <= 0) {
            return 0;
        }
        if (originalUserPages != null) {
            if (userPage >= originalUserPages.length) {
                return originalUserPages.length - 1;
            }
        } else {
            if (userPage >= getPagesCount()) {
                return getPagesCount() - 1;
            }
        }
        return userPage;
    }

    @Override
    public int documentPage(int userPage) {
        int documentPage = userPage;
        if (originalUserPages != null) {
            if (userPage < 0 || userPage >= originalUserPages.length) {
                return -1;
            } else {
                documentPage = originalUserPages[userPage];
            }
        }

        if (documentPage < 0 || userPage >= getPagesCount()) {
            return -1;
        }

        return documentPage;
    }
}
