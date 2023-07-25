/*
 * Copyright (C) 2016 Bartosz Schiller.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.barteksc.pdfviewer.source;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.pdf.PdfRenderer;
import android.os.MemoryFile;
import android.os.ParcelFileDescriptor;

import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ByteArraySource implements DocumentSource {

    private byte[] data;

    public ByteArraySource(byte[] data) {
        this.data = data;
    }

    @Override
    public PdfDocument createDocument(Context context, PdfiumCore core, String password) throws IOException {
        return core.newDocument(data, password);
    }

    @Override
    public PdfRenderer createPdfRenderer(Context context, String password) throws Exception {
        ParcelFileDescriptor parcelFileDescriptor = genParcelFileDescriptorWithMemoryFile();
        return new PdfRenderer(parcelFileDescriptor);
    }

    @SuppressLint("DiscouragedPrivateApi")
    private ParcelFileDescriptor genParcelFileDescriptorWithMemoryFile() throws IOException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {
        MemoryFile memoryFile = new MemoryFile(null, data.length);
        Method method = MemoryFile.class.getDeclaredMethod("getFileDescriptor");
        method.setAccessible(true);
        ParcelFileDescriptor parcelFileDescriptor = (ParcelFileDescriptor) method.invoke(memoryFile);

        memoryFile.writeBytes(data, 0, 0, data.length);
        return parcelFileDescriptor;
    }

    public static ParcelFileDescriptor convertByteArrayToParcelFileDescriptor(byte[] data) {
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            File tempFile = File.createTempFile("temp_file", ".tmp");
            FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
            fileOutputStream.write(data);
            fileOutputStream.close();
            parcelFileDescriptor = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return parcelFileDescriptor;
    }
}
