package de.tum.i13.datasets.airquality;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class StringZipFile {

    private final File file;

    public StringZipFile(File file) {
        this.file = file;
    }

    public StringZipFileIterator open() throws IOException {
        ZipFile zipFile = new ZipFile(this.file);
        var entries = Collections.list(zipFile.entries());
        for(ZipEntry zipEntry : entries) {
            if(zipEntry.getName().endsWith(".csv")) {
                InputStream stream = zipFile.getInputStream(zipEntry);
                InputStreamReader isr = new InputStreamReader(stream, StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(isr, 10*1024*1024);

                return new StringZipFileIterator(zipFile, zipEntry, stream, isr, br);
            }
        }
        zipFile.close();        
        throw new IOException("csv not found in file");
    }
}
